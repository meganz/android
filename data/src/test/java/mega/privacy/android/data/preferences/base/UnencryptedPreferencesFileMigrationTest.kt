package mega.privacy.android.data.preferences.base

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class UnencryptedPreferencesFileMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val enabledKey = booleanPreferencesKey("passcodeEnabled")

    private lateinit var sourceFileName: String
    private lateinit var underTest: UnencryptedPreferencesFileMigration

    @Before
    fun setUp() {
        sourceFileName = "source-${UUID.randomUUID()}"
        underTest = migration(AdoptedSourceDisposal.DeletePlaintextCopy)
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(sourceFileName).deleteRecursively()
    }

    private fun migration(sourceDisposal: AdoptedSourceDisposal) =
        UnencryptedPreferencesFileMigration(
            context = context,
            sourceFileName = sourceFileName,
            sourceDisposal = sourceDisposal,
        )

    private fun seedSourceFile(build: MutablePreferences.() -> Unit) {
        val preferences = mutablePreferencesOf().apply(build)
        val file = context.preferencesDataStoreFile(sourceFileName)
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            runBlocking { PreferencesSerializer.writeTo(preferences, out) }
        }
    }

    @Test
    fun `test that should migrate returns false when the source file is missing`() = runTest {
        assertThat(underTest.shouldMigrate(mutablePreferencesOf())).isFalse()
    }

    @Test
    fun `test that should migrate returns false when the store already holds data`() = runTest {
        seedSourceFile { this[enabledKey] = true }

        assertThat(underTest.shouldMigrate(preferencesOf(enabledKey to false))).isFalse()
    }

    @Test
    fun `test that should migrate returns true when the store is empty and the source file exists`() =
        runTest {
            seedSourceFile { this[enabledKey] = true }

            assertThat(underTest.shouldMigrate(mutablePreferencesOf())).isTrue()
        }

    @Test
    fun `test that migrate copies the values held in the source file`() = runTest {
        seedSourceFile { this[enabledKey] = true }

        val result = underTest.migrate(mutablePreferencesOf())

        assertThat(result[enabledKey]).isTrue()
    }

    @Test
    fun `test that migrate returns the current data when the source file cannot be read`() =
        runTest {
            val file = context.preferencesDataStoreFile(sourceFileName)
            file.parentFile?.mkdirs()
            file.writeBytes(zeroProtobufTag)

            val result = underTest.migrate(preferencesOf(enabledKey to true))

            assertThat(result[enabledKey]).isTrue()
        }

    @Test
    fun `test that migrate leaves the source file in place`() = runTest {
        seedSourceFile { this[enabledKey] = true }
        val file = context.preferencesDataStoreFile(sourceFileName)
        val bytes = file.readBytes()

        underTest.migrate(mutablePreferencesOf())

        assertThat(file.readBytes()).isEqualTo(bytes)
    }

    @Test
    fun `test that clean up deletes the source file once its values have been adopted`() = runTest {
        seedSourceFile { this[enabledKey] = true }
        underTest.migrate(mutablePreferencesOf())

        underTest.cleanUp()

        assertThat(context.preferencesDataStoreFile(sourceFileName).exists()).isFalse()
    }

    @Test
    fun `test that clean up leaves the source file in place when the source is the encrypted file`() =
        runTest {
            underTest = migration(AdoptedSourceDisposal.KeepEncryptedSource)
            seedSourceFile { this[enabledKey] = true }
            underTest.migrate(mutablePreferencesOf())

            underTest.cleanUp()

            assertThat(context.preferencesDataStoreFile(sourceFileName).exists()).isTrue()
        }

    @Test
    fun `test that clean up leaves the source file in place when nothing was adopted`() = runTest {
        val file = context.preferencesDataStoreFile(sourceFileName)
        file.parentFile?.mkdirs()
        file.writeBytes(zeroProtobufTag)
        underTest.migrate(mutablePreferencesOf())

        underTest.cleanUp()

        assertThat(file.readBytes()).isEqualTo(zeroProtobufTag)
    }

    @Test
    fun `test that clean up does not fail when the source file cannot be deleted`() = runTest {
        seedSourceFile { this[enabledKey] = true }
        underTest.migrate(mutablePreferencesOf())
        val file = context.preferencesDataStoreFile(sourceFileName)
        file.delete()
        file.mkdirs()
        File(file, "undeletable").writeBytes(zeroProtobufTag)

        underTest.cleanUp()

        assertThat(file.exists()).isTrue()
    }

    private companion object {
        /**
         * A zero protobuf tag, which no preferences file can hold: it stands in for the bytes of
         * an encrypted file handed to the plaintext reader.
         */
        val zeroProtobufTag = byteArrayOf(0, 0, 0, 0)
    }
}
