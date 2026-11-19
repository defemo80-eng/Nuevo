package com.detflix.admin

import com.detflix.admin.data.AdminCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminCryptoTest {
    @Test
    fun generatedCodesMatchDetflixSignatureFormat() {
        val regex = Regex("^DFX-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$")
        repeat(10) {
            val code = AdminCrypto.generateSignedCode()
            assertTrue("Unexpected code format: $code", regex.matches(code))
            val body = code.substring(0, 13)
            val tag = code.substring(14, 18)
            assertEquals(AdminCrypto.computeTag(body), tag)
        }
    }
}
