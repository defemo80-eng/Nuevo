package com.example

import com.example.data.security.DetflixCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetflixCryptoAndMembershipTest {

    @Test
    fun passwordHashing_verifiesCorrectAndRejectsWrong() {
        val salt = DetflixCrypto.generateSalt()
        val hash = DetflixCrypto.hashPassword("Detflix2026!", salt)
        assertTrue(DetflixCrypto.verifyPassword("Detflix2026!", hash))
        assertFalse(DetflixCrypto.verifyPassword("WrongPass", hash))
    }

    @Test
    fun accessCodeSignature_validatesGeneratedCodeAndRejectsTampered() {
        val body = "DFX-AB34-CD56"
        val tag = DetflixCrypto.computeTag(body)
        val fullCode = "$body-$tag"

        assertEquals(4, tag.length)
        assertTrue(DetflixCrypto.isValidAccessCodeFormatAndSignature(fullCode))
        assertTrue(DetflixCrypto.isValidAccessCodeFormatAndSignature(fullCode.lowercase()))
        assertFalse(DetflixCrypto.isValidAccessCodeFormatAndSignature("DFX-AB34-CD56-ZZZZ"))
        assertFalse(DetflixCrypto.isValidAccessCodeFormatAndSignature("INVALID-CODE"))
    }
}
