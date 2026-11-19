package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DetflixCategory
import com.example.data.repository.DetflixRepository
import com.example.data.security.DetflixCrypto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DetflixRepositoryRobolectricTest {

    @Test
    fun enterWithNameAndCodeSingleUseAndCategories() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = DetflixRepository(context)
        repo.initializeApp()

        // 1. Generate a valid signed DFX code matching DETFLIX ADMIN WEB
        val body1 = "DFX-7K9P-3M4Q"
        val validCode1 = "$body1-${DetflixCrypto.computeTag(body1)}"

        // 2. Enter strictly with Name + Access Code (no email or password required)
        val entryResult = repo.enterWithNameAndCode(
            displayName = "Carlos Cine",
            rawCode = validCode1
        )
        assertTrue(entryResult.isSuccess)
        val user = entryResult.getOrThrow()
        assertEquals("Carlos Cine", user.displayName)

        val membership = repo.observeMembership(user.uid).first()
        assertEquals("ACTIVE", membership?.status)
        assertTrue((membership?.expiresAt ?: 0L) > System.currentTimeMillis() + 29L * 86400_000L)

        // 3. Reusing the exact same code MUST fail (single-use invariant)
        val reuseAttempt = repo.enterWithNameAndCode(
            displayName = "Otro Usuario",
            rawCode = validCode1
        )
        assertTrue(reuseAttempt.isFailure)

        // 4. Returning user can enter via saved local account to view/renew membership
        repo.logout()
        val savedAttempt = repo.enterSavedAccount(user.uid)
        assertTrue(savedAttempt.isSuccess)

        // 5. Verify all 13 categories exist across the authorized seeded catalog
        val catalog = repo.observeCatalogWithEpisodes().first()
        val presentCategories = catalog.flatMap { it.categories }.toSet()
        for (cat in DetflixCategory.entries) {
            assertTrue(
                "Missing category ${cat.displayName} in catalog",
                presentCategories.any { it.equals(cat.displayName, ignoreCase = true) }
            )
        }
    }

    @Test
    fun revokedCodeClosesActiveSessionAndBlocksReentry() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = DetflixRepository(context)
        repo.initializeApp()

        val body = "DFX-9M2K-4P7R"
        val validCode = "$body-${DetflixCrypto.computeTag(body)}"
        val entryResult = repo.enterWithNameAndCode("Lucia Viewer", validCode)
        assertTrue(entryResult.isSuccess)
        val user = entryResult.getOrThrow()

        // Mark code as CANCELLED (simulating admin revocation synced to local store / provider)
        val dao = com.example.data.local.AppDatabase.getInstance(context).detflixDao()
        val codeEntity = dao.getAccessCode(validCode)!!
        dao.upsertAccessCode(codeEntity.copy(status = "CANCELLED"))

        val stillValid = repo.verifyActiveSessionAndCode(user.uid)
        assertTrue(!stillValid)
        assertEquals(null, repo.observeCurrentUserId().first())
        assertEquals("SUSPENDED", repo.observeMembership(user.uid).first()?.status)

        // Saved account re-entry MUST fail when the code was revoked
        val reEntryAttempt = repo.enterSavedAccount(user.uid)
        assertTrue(reEntryAttempt.isFailure)
    }
}
