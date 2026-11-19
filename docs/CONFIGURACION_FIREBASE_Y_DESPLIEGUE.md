# Guía Completa de Configuración, Conexión y Despliegue de DETFLIX

Este proyecto contiene **dos plataformas independientes** diseñadas para trabajar conectadas al mismo proyecto Firebase:

1. **DETFLIX APP** (`/app/`): Aplicación Android de streaming para clientes (Kotlin + Jetpack Compose + Room + Media3 ExoPlayer + Retrofit + WorkManager). **No contiene ningún panel administrativo.**
2. **DETFLIX ADMIN APK** (`/admin-app/`): Aplicación Android independiente para el administrador (generador de códigos `DFX`, clientes, pagos, renovaciones, catálogo y exportación `catalog.json`).
3. **DETFLIX ADMIN WEB** (`/admin-web/`): Panel administrativo web independiente para el propietario (HTML/CSS/JS moderno + Firebase Web SDK v10 + Hosting).
4. **BACKEND FIREBASE** (`/firebase-backend/`): Esquema (`firebase-blueprint.json`), reglas de seguridad zero-trust (`firestore.rules`) y Cloud Functions (`functions/index.js`).

---

## 1. Configurar Firebase (Gratis en Plan Spark / Blaze para Functions)

1. Ingresa a [Firebase Console](https://console.firebase.google.com/) y crea un proyecto llamado `detflix-platform` (no necesitas API keys pagas).
2. En **Authentication > Sign-in method**, habilita **Correo electrónico/contraseña** (y opcionalmente Google).
3. En **Firestore Database**, crea la base de datos en modo producción.
4. En **App Check**, registra tu app Android (Play Integrity / Debug) y la web administrativa (reCAPTCHA v3).

---

## 2. Asignar tu Cuenta Principal como Administrador (Seguridad)

Nunca permitas que un usuario normal se convierta en administrador desde el registro:

1. Crea tu cuenta de propietario en **Authentication > Users** y copia tu `UID`.
2. En Firestore, crea manualmente el documento `/admins/{TU_UID}` con `{ role: "ADMIN", email: "tu-correo@ejemplo.com" }`.
3. Opcionalmente, asigna el custom claim `{ "admin": true }` mediante Firebase Admin SDK.
4. Las reglas en `firebase-backend/firestore.rules` bloquean toda escritura en `/admins`, `/memberships`, `/access_codes`, `/payments` y `/admin_logs` para cualquier cuenta que no sea `isAdmin()`.

---

## 3. Publicar DETFLIX ADMIN WEB en su Propia Dirección Web

1. Copia `admin-web/firebase-config.example.js` a `admin-web/firebase-config.js`.
2. Pega las credenciales de tu Web App de Firebase en `window.DETFLIX_FIREBASE_CONFIG`.
3. Instala Firebase CLI y despliega Hosting + Reglas + Cloud Functions:

```bash
npm install -g firebase-tools
firebase login
cd firebase-backend
firebase use --add   # Selecciona tu proyecto detflix-platform
cd functions && npm install && cd ..
firebase deploy --only firestore:rules,hosting,functions
```

Tu panel administrativo quedará publicado en una URL privada propia, por ejemplo:
`https://detflix-platform.web.app` (accesible desde Chrome en celular o computador).

---

## 4. Conectar DETFLIX APP con Firebase y Catálogo Remoto

1. **Conexión rápida sin recompilar**: En la app Android, abre **Mi Membresía > Conectar Servidor y Catálogo Remoto** y pega la URL generada por `firebase deploy` (ej. `https://us-central1-detflix-platform.cloudfunctions.net`) y/o la URL del `catalog.json` exportado desde `DETFLIX ADMIN WEB` (ej. `https://detflix-platform.web.app/catalog.json`).
2. **Modo directo con `google-services.json`**: Descarga `google-services.json` desde Firebase Console para el paquete `com.aistudio.detflix.krmvqx`, colócalo en `/app/google-services.json` y descomenta las dependencias de Firebase en `/app/build.gradle.kts`.
3. **Acceso directo con Nombre y Código (sin API)**: Los usuarios entran en `DETFLIX APP` únicamente con **su nombre** y el código (`DFX-XXXX-XXXX-CCCC`) generado en `DETFLIX ADMIN WEB`, validado mediante firma HMAC-SHA256 en `DetflixCrypto.kt` y control de un solo uso en Room.

---

## 5. Compilar los 2 APKs (DETFLIX APP y DETFLIX ADMIN APK)

Desde la raíz del proyecto (o automáticamente en GitHub Actions `DETFLIX Build APK & Admin Web`):

```bash
gradle :app:assembleDebug :admin-app:assembleDebug
# APK Clientes: app/build/outputs/apk/debug/app-debug.apk (artefacto DETFLIX-APP-APK)
# APK Admin:    admin-app/build/outputs/apk/debug/admin-app-debug.apk (artefacto DETFLIX-ADMIN-APK)
```
