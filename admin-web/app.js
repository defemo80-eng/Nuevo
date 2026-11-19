// DETFLIX ADMIN WEB — Independent Executive Console
// Supports live Firebase (Auth + Firestore + Cloud Functions) when firebase-config.js is provided,
// and Local Storage console mode with real HMAC-SHA256 DFX code generation when unconfigured.

const CODE_HMAC_SECRET = "DETFLIX_MASTER_CODE_VERIFY_2026";
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const STORAGE_KEY = "detflix_admin_web_state_v1";

const DEFAULT_STATE = {
  settings: {
    monthlyPrice: 9.99,
    durationDays: 30,
    platformName: "DETFLIX",
    syncHours: 6,
    authorizedSources: "Internet Archive Public Domain API, Blender Foundation CC-BY, Media3 HLS",
    supportChannel: "Soporte WhatsApp / Telegram / Correo oficial DETFLIX"
  },
  clients: [],
  codes: [],
  payments: [],
  renewals: [],
  contents: [
    {
      id: "blender_sintel",
      title: "Sintel",
      type: "MOVIE",
      categories: "Acción, Aventura, Animación",
      year: 2010,
      featured: true,
      available: true,
      posterUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Sintel_poster.jpg/640px-Sintel_poster.jpg",
      streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      description: "Producción abierta de Blender Foundation."
    },
    {
      id: "series_flash_gordon",
      title: "Flash Gordon: Crónicas del Planeta Mongo",
      type: "SERIES",
      categories: "Ciencia ficción, Aventura",
      year: 1936,
      featured: true,
      available: true,
      posterUrl: "https://archive.org/services/img/flash_gordon_space_soldiers",
      streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      description: "Serie clásica de dominio público."
    }
  ],
  logs: []
};

let state = loadLocalState();
let firebaseCtx = null;

function loadLocalState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return structuredClone(DEFAULT_STATE);
    return { ...structuredClone(DEFAULT_STATE), ...JSON.parse(raw) };
  } catch {
    return structuredClone(DEFAULT_STATE);
  }
}

function saveLocalState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function logAction(message) {
  state.logs.unshift({ time: new Date().toISOString(), message });
  state.logs = state.logs.slice(0, 40);
  saveLocalState();
}

// Cryptographic DFX-XXXX-XXXX-CCCC generator (matches DetflixCrypto.kt and functions/index.js)
async function generateSignedCode() {
  const randomBlock = () => {
    const arr = new Uint8Array(4);
    crypto.getRandomValues(arr);
    return Array.from(arr, (b) => ALPHABET[b % ALPHABET.length]).join("");
  };
  const body = `DFX-${randomBlock()}-${randomBlock()}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(CODE_HMAC_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(body)));
  let tag = "";
  for (let i = 0; i < 4; i++) {
    tag += ALPHABET[sig[i] % ALPHABET.length];
  }
  return `${body}-${tag}`;
}

async function initFirebaseIfConfigured() {
  const cfg = window.DETFLIX_FIREBASE_CONFIG;
  if (!cfg || !cfg.apiKey || !cfg.projectId) return null;
  try {
    const [{ initializeApp }, authMod, fsMod] = await Promise.all([
      import("https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js"),
      import("https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js"),
      import("https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js")
    ]);
    const app = initializeApp(cfg);
    const auth = authMod.getAuth(app);
    const db = cfg.databaseId && cfg.databaseId !== "(default)"
      ? fsMod.getFirestore(app, cfg.databaseId)
      : fsMod.getFirestore(app);
    document.getElementById("modeNotice").innerHTML =
      `Conectado al proyecto Firebase <strong>${cfg.projectId}</strong>. Acceso exclusivo para cuentas con rol ADMIN.`;
    return { app, auth, db, authMod, fsMod, cfg };
  } catch (err) {
    console.warn("Firebase Web SDK fallback to local mode:", err);
    return null;
  }
}

// DOM References
const authSection = document.getElementById("authSection");
const adminApp = document.getElementById("adminApp");
const adminLoginForm = document.getElementById("adminLoginForm");
const authError = document.getElementById("authError");
const sidebar = document.getElementById("sidebar");

const quickLocalBtn = document.getElementById("quickLocalBtn");

initFirebaseIfConfigured().then((ctx) => {
  firebaseCtx = ctx;
  if (ctx && quickLocalBtn) {
    quickLocalBtn.classList.add("hidden");
  }
});

if (quickLocalBtn) {
  quickLocalBtn.addEventListener("click", () => {
    if (firebaseCtx) return;
    document.getElementById("connectionStatus").textContent = "Modo Local (Acceso Directo) — Códigos compatibles con DETFLIX APP";
    enterConsole();
  });
}

adminLoginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  authError.textContent = "";
  const email = document.getElementById("adminEmail").value.trim();
  const password = document.getElementById("adminPassword").value;

  if (firebaseCtx) {
    try {
      const cred = await firebaseCtx.authMod.signInWithEmailAndPassword(firebaseCtx.auth, email, password);
      const token = await cred.user.getIdTokenResult(true);
      const adminDoc = await firebaseCtx.fsMod.getDoc(
        firebaseCtx.fsMod.doc(firebaseCtx.db, "admins", cred.user.uid)
      );
      if (token.claims.admin !== true && !adminDoc.exists()) {
        await firebaseCtx.authMod.signOut(firebaseCtx.auth);
        authError.textContent = "Acceso denegado: esta cuenta no tiene rol ADMIN en el servidor.";
        return;
      }
      document.getElementById("connectionStatus").textContent = `Firebase en vivo: ${firebaseCtx.cfg.projectId}`;
      enterConsole();
    } catch (err) {
      authError.textContent = `Error de autenticación Firebase: ${err.message}`;
    }
  } else {
    if (!email.includes("@") || password.length < 4) {
      authError.textContent = "Ingresa un correo y contraseña válidos.";
      return;
    }
    document.getElementById("connectionStatus").textContent = `Modo Local (${email}) — Códigos compatibles con DETFLIX APP`;
    enterConsole();
  }
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  if (firebaseCtx) await firebaseCtx.authMod.signOut(firebaseCtx.auth);
  adminApp.classList.add("hidden");
  authSection.classList.remove("hidden");
});

document.getElementById("mobileMenuBtn").addEventListener("click", () => {
  sidebar.classList.toggle("open");
});

const tabTitles = {
  inicio: "A. Inicio y Estadísticas",
  codigos: "B. Generador de Códigos Mensuales",
  clientes: "C. Administración de Clientes",
  pagos: "D. Control de Pagos e Ingresos",
  renovaciones: "E. Renovaciones de Membresía",
  contenido: "F. Gestión de Catálogo",
  configuracion: "G. Configuración de Plataforma"
};

document.querySelectorAll(".nav-item").forEach((btn) => {
  btn.addEventListener("click", () => {
    const tab = btn.dataset.tab;
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.add("hidden"));
    document.getElementById(`tab-${tab}`).classList.remove("hidden");
    document.getElementById("pageTitle").textContent = tabTitles[tab] || "DETFLIX ADMIN";
    sidebar.classList.remove("open");
  });
});

function enterConsole() {
  authSection.classList.add("hidden");
  adminApp.classList.remove("hidden");
  renderAll();
}

function fmtDate(isoOrMs) {
  if (!isoOrMs) return "—";
  return new Date(isoOrMs).toLocaleDateString("es-ES", {
    day: "2-digit", month: "short", year: "numeric"
  });
}

// B. Generate Access Code
document.getElementById("generateCodeBtn").addEventListener("click", async () => {
  const code = await generateSignedCode();
  const now = Date.now();
  const entry = {
    code,
    status: "available",
    createdAt: now,
    activatedAt: null,
    expiresAt: null,
    usedByEmail: null
  };
  state.codes.unshift(entry);
  logAction(`Código generado: ${code}`);
  saveLocalState();

  if (firebaseCtx) {
    await firebaseCtx.fsMod.setDoc(
      firebaseCtx.fsMod.doc(firebaseCtx.db, "access_codes", code),
      {
        code,
        status: "available",
        durationDays: state.settings.durationDays,
        createdAt: firebaseCtx.fsMod.serverTimestamp(),
        createdByAdmin: firebaseCtx.auth.currentUser.uid
      }
    );
  }

  document.getElementById("generatedCodeText").textContent = code;
  document.getElementById("generatedCodeBanner").classList.remove("hidden");
  renderAll();
});

document.getElementById("copyCodeBtn").addEventListener("click", async () => {
  const code = document.getElementById("generatedCodeText").textContent;
  await navigator.clipboard.writeText(code);
  document.getElementById("copyCodeBtn").textContent = "¡Copiado!";
  setTimeout(() => { document.getElementById("copyCodeBtn").textContent = "Copiar Código"; }, 1800);
});

// C. Clients (Nombre como identificador principal; contacto opcional)
document.getElementById("addClientForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const name = document.getElementById("newClientName").value.trim();
  const contact = document.getElementById("newClientEmail").value.trim();
  if (!name) return;
  const normalizedName = name.toLowerCase();
  if (!state.clients.some((c) => c.name.toLowerCase() === normalizedName)) {
    state.clients.unshift({
      id: `usr_${Date.now()}`,
      name,
      email: contact,
      status: "expired",
      expiresAt: null,
      lastCode: null,
      suspended: false
    });
    logAction(`Cliente registrado: ${name}`);
    saveLocalState();
  }
  e.target.reset();
  renderAll();
});

document.getElementById("clientSearchInput").addEventListener("input", renderClients);

// D. Payments (requires admin confirmation before activating)
document.getElementById("paymentForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const clientRef = document.getElementById("payClientEmail").value.trim();
  const amount = parseFloat(document.getElementById("payAmount").value);
  const method = document.getElementById("payMethod").value;
  const status = document.getElementById("payStatus").value;
  if (!clientRef) return;
  state.payments.unshift({
    id: `pay_${Date.now()}`,
    email: clientRef,
    amount,
    method,
    status,
    date: Date.now()
  });
  logAction(`Pago ${status === "confirmed" ? "confirmado" : "pendiente"} de ${clientRef} ($${amount.toFixed(2)})`);
  saveLocalState();
  e.target.reset();
  renderAll();
});

// E. Renewals
document.getElementById("renewalForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const clientRef = document.getElementById("renewClientEmail").value.trim();
  if (!clientRef) return;
  const code = await generateSignedCode();
  const now = Date.now();
  const expiresAt = now + state.settings.durationDays * 86400000;
  state.codes.unshift({
    code,
    status: "available",
    createdAt: now,
    activatedAt: null,
    expiresAt,
    usedByEmail: clientRef
  });
  state.renewals.unshift({
    id: `ren_${now}`,
    email: clientRef,
    code,
    date: now,
    expiresAt
  });
  const refLower = clientRef.toLowerCase();
  const client = state.clients.find(
    (c) => c.name.toLowerCase() === refLower || (c.email && c.email.toLowerCase() === refLower)
  );
  if (client) {
    client.lastCode = code;
    client.status = "pending_code";
  } else {
    state.clients.unshift({
      id: `usr_${now}`,
      name: clientRef,
      email: "",
      status: "pending_code",
      expiresAt,
      lastCode: code,
      suspended: false
    });
  }
  logAction(`Renovación preparada para ${clientRef} — Código: ${code}`);
  saveLocalState();
  e.target.reset();
  renderAll();
});

// F. Content Management
document.getElementById("contentForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const item = {
    id: `cnt_${Date.now()}`,
    title: document.getElementById("cntTitle").value.trim(),
    type: document.getElementById("cntType").value,
    categories: document.getElementById("cntCategory").value.trim(),
    year: parseInt(document.getElementById("cntYear").value, 10) || 2025,
    posterUrl: document.getElementById("cntPoster").value.trim(),
    streamUrl: document.getElementById("cntStream").value.trim(),
    description: document.getElementById("cntDesc").value.trim(),
    featured: true,
    available: true
  };
  state.contents.unshift(item);
  logAction(`Contenido agregado: ${item.title}`);
  saveLocalState();
  e.target.reset();
  renderAll();
});

document.getElementById("exportCatalogBtn").addEventListener("click", () => {
  const payload = {
    updatedAt: new Date().toISOString(),
    contents: state.contents.map((c) => ({
      id: c.id,
      title: c.title,
      description: c.description,
      posterUrl: c.posterUrl,
      backdropUrl: c.posterUrl,
      streamUrl: c.streamUrl,
      type: c.type,
      categories: c.categories,
      year: c.year,
      featured: Boolean(c.featured),
      available: Boolean(c.available)
    }))
  };
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "catalog.json";
  a.click();
  URL.revokeObjectURL(url);
  logAction("Catálogo exportado a catalog.json para sincronización remota");
  renderAll();
});

// G. Settings
document.getElementById("settingsForm").addEventListener("submit", (e) => {
  e.preventDefault();
  state.settings.monthlyPrice = parseFloat(document.getElementById("cfgPrice").value) || 9.99;
  state.settings.durationDays = parseInt(document.getElementById("cfgDuration").value, 10) || 30;
  state.settings.platformName = document.getElementById("cfgPlatformName").value.trim() || "DETFLIX";
  state.settings.syncHours = parseInt(document.getElementById("cfgSyncHours").value, 10) || 6;
  state.settings.authorizedSources = document.getElementById("cfgSources").value.trim();
  state.settings.supportChannel = document.getElementById("cfgSupport").value.trim();
  logAction("Configuración de plataforma actualizada");
  saveLocalState();
  renderAll();
});

function renderAll() {
  renderDashboard();
  renderCodes();
  renderClients();
  renderPayments();
  renderRenewals();
  renderContent();
  renderSettings();
}

function renderDashboard() {
  const now = Date.now();
  const activeMemberships = state.clients.filter((c) => !c.suspended && c.expiresAt && c.expiresAt > now).length;
  const expiredMemberships = state.clients.length - activeMemberships;
  const availableCodes = state.codes.filter((c) => c.status === "available").length;
  const usedCodes = state.codes.filter((c) => c.status === "used").length;
  const confirmedPayments = state.payments.filter((p) => p.status === "confirmed");
  const monthRevenue = confirmedPayments.reduce((acc, p) => acc + p.amount, 0);

  const cards = [
    ["Usuarios Registrados", state.clients.length],
    ["Membresías Activas", activeMemberships],
    ["Membresías Vencidas", expiredMemberships],
    ["Códigos Disponibles", availableCodes],
    ["Códigos Utilizados", usedCodes],
    ["Pagos Registrados", state.payments.length],
    ["Ingresos del Mes", `$${monthRevenue.toFixed(2)}`]
  ];
  document.getElementById("dashboardStats").innerHTML = cards
    .map(([label, val]) => `<div class="stat-card"><div class="stat-label">${label}</div><div class="stat-value">${val}</div></div>`)
    .join("");

  document.getElementById("recentActivityList").innerHTML = state.logs.length
    ? state.logs.slice(0, 7).map((l) => `<div><span style="color:var(--red);">●</span> ${l.message} <span style="color:var(--muted);font-size:11.5px;">(${fmtDate(l.time)})</span></div>`).join("")
    : `<div style="color:var(--muted);">Sin operaciones administrativas registradas aún.</div>`;

  // Simple SVG bar chart for confirmed vs pending revenue
  const confirmedTotal = monthRevenue;
  const pendingTotal = state.payments.filter((p) => p.status === "pending").reduce((a, p) => a + p.amount, 0);
  const maxVal = Math.max(confirmedTotal, pendingTotal, 10);
  const hConf = Math.round((confirmedTotal / maxVal) * 130);
  const hPend = Math.round((pendingTotal / maxVal) * 130);

  document.getElementById("revenueChart").innerHTML = `
    <rect x="70" y="${155 - hConf}" width="90" height="${hConf}" rx="8" fill="#e50914" />
    <text x="115" y="178" fill="#ffffff" font-size="12" text-anchor="middle">Confirmado ($${confirmedTotal.toFixed(0)})</text>
    <rect x="260" y="${155 - hPend}" width="90" height="${hPend}" rx="8" fill="#ffb800" />
    <text x="305" y="178" fill="#ffffff" font-size="12" text-anchor="middle">Pendiente ($${pendingTotal.toFixed(0)})</text>
  `;
}

function renderCodes() {
  const tbody = document.getElementById("codesTableBody");
  tbody.innerHTML = state.codes.map((c) => {
    const pillClass = c.status === "available" ? "pill-green" : c.status === "used" ? "pill-gold" : "pill-red";
    return `<tr>
      <td><code>${c.code}</code></td>
      <td><span class="pill ${pillClass}">${c.status.toUpperCase()}</span></td>
      <td>${fmtDate(c.createdAt)}</td>
      <td>${fmtDate(c.activatedAt)}</td>
      <td>${fmtDate(c.expiresAt)}</td>
      <td>${c.usedByEmail || "—"}</td>
      <td>${c.status === "available" ? `<button class="btn btn-secondary btn-sm" data-cancel-code="${c.code}">Cancelar</button>` : "—"}</td>
    </tr>`;
  }).join("") || `<tr><td colspan="7" style="color:var(--muted);">Genera tu primer código mensual con el botón superior.</td></tr>`;

  tbody.querySelectorAll("[data-cancel-code]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const code = btn.dataset.cancelCode;
      const found = state.codes.find((c) => c.code === code);
      if (found) found.status = "cancelled";
      logAction(`Código cancelado: ${code}`);
      saveLocalState();
      renderAll();
    });
  });
}

function renderClients() {
  const q = document.getElementById("clientSearchInput").value.trim().toLowerCase();
  const list = state.clients.filter((c) => !q || c.name.toLowerCase().includes(q) || (c.email && c.email.toLowerCase().includes(q)));
  const tbody = document.getElementById("clientsTableBody");
  tbody.innerHTML = list.map((c) => {
    const statusPill = c.suspended ? `<span class="pill pill-red">SUSPENDIDO</span>` : `<span class="pill pill-gold">${c.status.toUpperCase()}</span>`;
    return `<tr>
      <td>${c.name}</td>
      <td>${c.email || "—"}</td>
      <td>${statusPill}</td>
      <td>${fmtDate(c.expiresAt)}</td>
      <td>${c.lastCode || "—"}</td>
      <td>
        <button class="btn btn-secondary btn-sm" data-toggle-suspend="${c.id}">
          ${c.suspended ? "Reactivar" : "Suspender"}
        </button>
      </td>
    </tr>`;
  }).join("") || `<tr><td colspan="6" style="color:var(--muted);">No hay clientes que coincidan.</td></tr>`;

  tbody.querySelectorAll("[data-toggle-suspend]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const client = state.clients.find((c) => c.id === btn.dataset.toggleSuspend);
      if (client) {
        client.suspended = !client.suspended;
        logAction(`${client.suspended ? "Acceso suspendido" : "Acceso reactivado"}: ${client.name}`);
        saveLocalState();
        renderAll();
      }
    });
  });
}

function renderPayments() {
  const now = Date.now();
  const confirmed = state.payments.filter((p) => p.status === "confirmed");
  const daily = confirmed.filter((p) => now - p.date < 86400000).reduce((a, p) => a + p.amount, 0);
  const weekly = confirmed.filter((p) => now - p.date < 7 * 86400000).reduce((a, p) => a + p.amount, 0);
  const monthly = confirmed.filter((p) => now - p.date < 30 * 86400000).reduce((a, p) => a + p.amount, 0);

  document.getElementById("paymentStatsGrid").innerHTML = [
    ["Ingresos Hoy", `$${daily.toFixed(2)}`],
    ["Ingresos Semana", `$${weekly.toFixed(2)}`],
    ["Ingresos 30 Días", `$${monthly.toFixed(2)}`],
    ["Pagos Pendientes", state.payments.filter((p) => p.status === "pending").length]
  ].map(([l, v]) => `<div class="stat-card"><div class="stat-label">${l}</div><div class="stat-value">${v}</div></div>`).join("");

  const tbody = document.getElementById("paymentsTableBody");
  tbody.innerHTML = state.payments.map((p) => `<tr>
    <td>${fmtDate(p.date)}</td>
    <td>${p.email}</td>
    <td>$${p.amount.toFixed(2)}</td>
    <td>${p.method}</td>
    <td><span class="pill ${p.status === "confirmed" ? "pill-green" : "pill-gold"}">${p.status === "confirmed" ? "CONFIRMADO" : "PENDIENTE"}</span></td>
    <td>${p.status === "pending" ? `<button class="btn btn-primary btn-sm" data-confirm-pay="${p.id}">Confirmar Pago</button>` : "Verificado"}</td>
  </tr>`).join("") || `<tr><td colspan="6" style="color:var(--muted);">Sin pagos registrados.</td></tr>`;

  tbody.querySelectorAll("[data-confirm-pay]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const p = state.payments.find((x) => x.id === btn.dataset.confirmPay);
      if (p) {
        p.status = "confirmed";
        logAction(`Pago confirmado administrativamente para ${p.email} ($${p.amount.toFixed(2)})`);
        saveLocalState();
        renderAll();
      }
    });
  });
}

function renderRenewals() {
  document.getElementById("renewalsTableBody").innerHTML = state.renewals.map((r) => `<tr>
    <td>${fmtDate(r.date)}</td>
    <td>${r.email}</td>
    <td><code>${r.code}</code></td>
    <td>${fmtDate(r.expiresAt)}</td>
  </tr>`).join("") || `<tr><td colspan="4" style="color:var(--muted);">Sin renovaciones registradas.</td></tr>`;
}

function renderContent() {
  const tbody = document.getElementById("contentTableBody");
  tbody.innerHTML = state.contents.map((c) => `<tr>
    <td>${c.title}</td>
    <td>${c.type}</td>
    <td>${c.categories}</td>
    <td>${c.year}</td>
    <td><span class="pill ${c.available ? "pill-green" : "pill-red"}">${c.available ? "ACTIVO" : "RETIRADO"}</span></td>
    <td><button class="btn btn-secondary btn-sm" data-toggle-content="${c.id}">${c.available ? "Retirar" : "Publicar"}</button></td>
  </tr>`).join("");

  tbody.querySelectorAll("[data-toggle-content]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = state.contents.find((c) => c.id === btn.dataset.toggleContent);
      if (item) {
        item.available = !item.available;
        logAction(`${item.available ? "Publicado" : "Retirado"} contenido: ${item.title}`);
        saveLocalState();
        renderAll();
      }
    });
  });
}

function renderSettings() {
  document.getElementById("cfgPrice").value = state.settings.monthlyPrice;
  document.getElementById("cfgDuration").value = state.settings.durationDays;
  document.getElementById("cfgPlatformName").value = state.settings.platformName;
  document.getElementById("cfgSyncHours").value = state.settings.syncHours;
  document.getElementById("cfgSources").value = state.settings.authorizedSources;
  document.getElementById("cfgSupport").value = state.settings.supportChannel;
}
