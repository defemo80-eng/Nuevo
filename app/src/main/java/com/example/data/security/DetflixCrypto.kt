package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DetflixCrypto {
    // Shared HMAC key for verifying offline-compatible DETFLIX ADMIN WEB codes (DFX-XXXX-XXXX-CCCC)
    // When Firebase Cloud Functions is connected, codes are validated directly on the server.
    private const val CODE_HMAC_SECRET = "DETFLIX_MASTER_CODE_VERIFY_2026"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return "$salt$${bytes.joinToString("") { "%02x".format(it) }}"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split("$")
        if (parts.size != 2) return false
        val salt = parts[0]
        return hashPassword(password, salt) == storedHash
    }

    /**
     * Validates access code format `DFX-XXXX-XXXX-CCCC` where `CCCC` is the 4-char
     * HMAC-SHA256 verification tag derived from `DFX-XXXX-XXXX`.
     * Matches the exact generator algorithm in DETFLIX ADMIN WEB (`admin-web/app.js`).
     */
    fun isValidAccessCodeFormatAndSignature(rawCode: String): Boolean {
        val normalized = rawCode.trim().uppercase()
        val regex = Regex("^DFX-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$")
        if (!regex.matches(normalized)) return false
        val body = normalized.substring(0, 13) // DFX-XXXX-XXXX
        val tag = normalized.substring(14, 18) // CCCC
        return computeTag(body) == tag
    }

    fun computeTag(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(CODE_HMAC_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(body.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(4)
        for (i in 0 until 4) {
            val idx = (digest[i].toInt() and 0xFF) % ALPHABET.length
            sb.append(ALPHABET[idx])
        }
        return sb.toString()
    }
}
