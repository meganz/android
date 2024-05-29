package mega.privacy.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.preferences.base.createEncrypted
import org.junit.After
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals

internal class EncryptedPreferenceDataStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = context.preferencesDataStoreFile("test")

    @Test
    fun encryptAndDecrypt(): Unit = runTest {
        val dataKey = stringPreferencesKey("testKey")
        val dataStore = createDataStore(this)
        val data = dataStore.data
        val random = Random(100)
        // Write data and close DataStore
        repeat(1000) {
            val plaintext = random.nextBytes(100).toString(Charsets.UTF_8)
            coroutineScope {
                dataStore.edit { it[dataKey] = plaintext }
            }

            // Read data
            val preferences = data.first()

            assertEquals(plaintext, preferences[dataKey])
        }
    }

    @After
    fun tearDown() {
        file.delete()
    }

    private fun createDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return PreferenceDataStoreFactory.createEncrypted(
            scope = scope,
            context = context,
            fileName = "test",
            masterKey = masterKey
        )
    }
}
