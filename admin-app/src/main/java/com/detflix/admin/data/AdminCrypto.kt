package com.detflix.admin.data

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic generator for DETFLIX single-use monthly codes (`DFX-XXXX-XXXX-CCCC`).
 * Uses the exact same HMAC-SHA256 secret and alphabet as DETFLIX APP (`DetflixCrypto.kt`)
 * so every code generated in DETFLIX Admin APK activates seamlessly in the client app.
 */
object AdminCrypto {
    private const val CODE_HMAC_SECRET = "DETFLIX_MASTER_CODE_VERIFY_2026"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generateSignedCode(): String {
        val body = "DFX-${randomBlock()}-${randomBlock()}"
        val tag = computeTag(body)
        return "$body-$tag"
    }

    private fun randomBlock(): String {
        val bytes = ByteArray(4)
        random.nextBytes(bytes)
        val sb = StringBuilder(4)
        for (b in bytes) {
            sb.append(ALPHABET[(b.toInt() and 0xFF) % ALPHABET.length])
        }
        return sb.toString()
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
