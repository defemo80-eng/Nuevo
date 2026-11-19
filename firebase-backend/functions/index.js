const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_HMAC_SECRET = process.env.DETFLIX_CODE_SECRET || "DETFLIX_MASTER_CODE_VERIFY_2026";

function signCodeBody(body) {
  const hmac = crypto.createHmac("sha256", CODE_HMAC_SECRET).update(body).digest();
  let tag = "";
  for (let i = 0; i < 4; i++) {
    tag += ALPHABET[hmac[i] % ALPHABET.length];
  }
  return `${body}-${tag}`;
}

function randomBlock() {
  const bytes = crypto.randomBytes(4);
  let out = "";
  for (let i = 0; i < 4; i++) {
    out += ALPHABET[bytes[i] % ALPHABET.length];
  }
  return out;
}

async function assertAdmin(uid, token) {
  if (!uid) throw new HttpsError("unauthenticated", "Autenticación requerida.");
  if (token?.admin === true) return;
  const adminSnap = await db.collection("admins").doc(uid).get();
  if (!adminSnap.exists) {
    throw new HttpsError("permission-denied", "Se requiere rol ADMIN.");
  }
}

// 1. Generar código único mensual desde el servidor (solo ADMIN)
exports.generateAccessCode = onCall(async (request) => {
  await assertAdmin(request.auth?.uid, request.auth?.token);
  const durationDays = Number(request.data?.durationDays) || 30;
  const body = `DFX-${randomBlock()}-${randomBlock()}`;
  const code = signCodeBody(body);

  await db.collection("access_codes").doc(code).set({
    code,
    status: "available",
    durationDays,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    activatedAt: null,
    expiresAt: null,
    usedByUserId: null,
    createdByAdmin: request.auth.uid
  });

  await db.collection("admin_logs").add({
    adminUid: request.auth.uid,
    action: "GENERATE_CODE",
    targetId: code,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { code, durationDays };
});

// 2. Validar y canjear código mensual (atómico, un solo uso, fechas calculadas en servidor)
exports.redeemAccessCode = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ success: false, message: "Método no permitido" });
  }
  const code = String(req.body?.code || "").trim().toUpperCase();
  const userId = String(req.body?.userId || "").trim();
  const email = String(req.body?.email || "").trim().toLowerCase();

  if (!code || !userId) {
    return res.status(400).json({ success: false, message: "Código y usuario requeridos." });
  }

  try {
    const result = await db.runTransaction(async (tx) => {
      const codeRef = db.collection("access_codes").doc(code);
      const codeSnap = await tx.get(codeRef);
      if (!codeSnap.exists) {
        throw new Error("Código inexistente en DETFLIX.");
      }
      const codeData = codeSnap.data();
      if (codeData.status !== "available") {
        throw new Error(`El código no está disponible (estado actual: ${codeData.status}).`);
      }

      const memRef = db.collection("memberships").doc(userId);
      const memSnap = await tx.get(memRef);
      const nowMs = Date.now();
      const durationDays = Number(codeData.durationDays) || 30;
      const existingExp = memSnap.exists && memSnap.data().status === "ACTIVE" && memSnap.data().expiresAt
        ? memSnap.data().expiresAt.toMillis()
        : 0;
      const baseMs = existingExp > nowMs ? existingExp : nowMs;
      const expiresAtMs = baseMs + durationDays * 86400000;

      const activatedTs = admin.firestore.Timestamp.fromMillis(nowMs);
      const expiresTs = admin.firestore.Timestamp.fromMillis(expiresAtMs);

      tx.update(codeRef, {
        status: "used",
        activatedAt: activatedTs,
        expiresAt: expiresTs,
        usedByUserId: userId,
        usedByEmail: email
      });

      tx.set(memRef, {
        userId,
        status: "ACTIVE",
        planName: `Mensual DETFLIX ${durationDays} Días`,
        activatedAt: activatedTs,
        expiresAt: expiresTs,
        lastCodeUsed: code,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      return { activatedAt: nowMs, expiresAt: expiresAtMs, durationDays };
    });

    return res.json({
      success: true,
      message: "Membresía activada por 30 días.",
      ...result
    });
  } catch (err) {
    return res.status(400).json({ success: false, message: err.message });
  }
});

// 3. Sincronización automática periódica del catálogo autorizado (cada 6 horas)
exports.syncAuthorizedCatalog = onSchedule("every 6 hours", async () => {
  const url = "https://archive.org/advancedsearch.php?q=collection:feature_films+AND+mediatype:movies&fl[]=identifier&fl[]=title&fl[]=description&fl[]=year&rows=5&output=json&sort[]=downloads+desc";
  const resp = await fetch(url);
  if (!resp.ok) return;
  const json = await resp.json();
  const docs = json?.response?.docs || [];
  const batch = db.batch();

  for (const doc of docs) {
    if (!doc.identifier || !doc.title) continue;
    const ref = db.collection("content").doc(`ia_${doc.identifier}`);
    batch.set(ref, {
      title: String(doc.title).slice(0, 80),
      description: String(doc.description || "Dominio público autorizado").slice(0, 360),
      type: "MOVIE",
      posterUrl: `https://archive.org/services/img/${doc.identifier}`,
      streamUrl: `https://archive.org/download/${doc.identifier}/${doc.identifier}_512kb.mp4`,
      categories: "Drama,Suspenso",
      year: parseInt(String(doc.year || "1960").slice(0, 4), 10) || 1960,
      ageRating: "+12",
      isAvailable: true,
      sourceName: "Internet Archive Sync",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
  }
  await batch.commit();
});

// 4. Vencimiento automático diario de membresías
exports.expireMemberships = onSchedule("every 24 hours", async () => {
  const nowTs = admin.firestore.Timestamp.now();
  const expiredSnap = await db.collection("memberships")
    .where("status", "==", "ACTIVE")
    .where("expiresAt", "<=", nowTs)
    .get();

  const batch = db.batch();
  expiredSnap.forEach((docSnap) => {
    batch.update(docSnap.ref, {
      status: "EXPIRED",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    const notifRef = db.collection("notifications").doc();
    batch.set(notifRef, {
      userId: docSnap.id,
      title: "Membresía DETFLIX vencida",
      message: "Tu suscripción mensual finalizó. Introduce un nuevo código para continuar.",
      type: "MEMBERSHIP_EXPIRED",
      isRead: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  await batch.commit();
});
