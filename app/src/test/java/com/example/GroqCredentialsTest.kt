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
        val store = GroqCredentials(context) { encryptionKey }
        store.clear()
        val value = "gsk_" + "test_only_credential_".repeat(3)
        try {
            store.save(value)
            assertEquals(value, GroqCredentials(context) { encryptionKey }.read())
            val ciphertext = File(context.noBackupFilesDir, "groq_credentials").readBytes()
            assertFalse(String(ciphertext, Charsets.ISO_8859_1).contains(value))
            store.clear()
            assertEquals("", store.read())
        } finally { store.clear() }
    }

    @Test fun invalidKeysDoNotOverwriteSavedCredentialsAndCorruptionFailsClosed() {
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = GroqCredentials(context) { encryptionKey }
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
}
