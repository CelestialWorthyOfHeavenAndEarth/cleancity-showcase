package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GroqCredentials
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GroqCredentialsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun credentialsRoundTripEncryptedOutsideBackupsAndCanBeRemoved() {
        // Test AES/GCM with an injected key; hardware-backed AndroidKeyStore remains a device check.
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = GroqCredentials(context, bundledKey = "") { encryptionKey }
        store.clear()
        val value = "gsk_" + "test_only_credential_".repeat(3)
        try {
            store.save(value)
            assertEquals(value, GroqCredentials(context, bundledKey = "") { encryptionKey }.read())
            val ciphertext = File(context.noBackupFilesDir, "groq_credentials").readBytes()
            assertFalse(String(ciphertext, Charsets.ISO_8859_1).contains(value))
            store.clear()
            assertEquals("", store.read())
        } finally { store.clear() }
    }

    @Test fun invalidKeysDoNotOverwriteSavedCredentialsAndCorruptionFailsClosed() {
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = GroqCredentials(context, bundledKey = "") { encryptionKey }
        store.clear()
        val value = "gsk_" + "test_only_credential_".repeat(3)
        try {
            store.save(value)
            assertTrue(runCatching { store.save("invalid") }.isFailure)
            assertEquals(value, store.read())
            val file = File(context.noBackupFilesDir, "groq_credentials")
            val corrupted = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            file.writeBytes(corrupted)
            assertEquals("", store.read())
        } finally { store.clear() }
    }

    @Test fun bundledKeyConnectsFreshInstallsWithoutUsingAndroidKeystore() {
        val bundled = "gsk_" + "bundled_test_key_".repeat(3)
        val store = GroqCredentials(context, bundled) { error("Bundled credentials need no device key") }
        assertEquals(bundled, store.read())
        assertEquals(bundled, GroqCredentials(context, bundled).read())
        assertFalse(File(context.noBackupFilesDir, "groq_credentials").exists())
    }

    @Test fun deviceOverrideWinsAndRemovingItAlsoDisablesBundledKeyAcrossRestarts() {
        val bundled = "gsk_" + "bundled_test_key_".repeat(3)
        val override = "gsk_" + "override_test_key_".repeat(3)
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = GroqCredentials(context, bundled) { encryptionKey }
        assertEquals(bundled, store.read())
        store.save(override)
        assertEquals(override, GroqCredentials(context, bundled) { encryptionKey }.read())
        store.clear()
        assertEquals("", GroqCredentials(context, bundled) { encryptionKey }.read())
        store.save(override)
        assertEquals(override, GroqCredentials(context, bundled) { encryptionKey }.read())
    }

    @Test fun corruptOverrideDoesNotSilentlySwitchToBundledAccount() {
        val bundled = "gsk_" + "bundled_test_key_".repeat(3)
        val override = "gsk_" + "override_test_key_".repeat(3)
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = GroqCredentials(context, bundled) { encryptionKey }
        store.save(override)
        File(context.noBackupFilesDir, "groq_credentials").writeBytes(byteArrayOf(0))
        assertEquals("", store.read())
    }
}
