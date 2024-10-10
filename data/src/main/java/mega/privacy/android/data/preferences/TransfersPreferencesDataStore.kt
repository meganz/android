package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.TransfersPreferencesGateway
import javax.inject.Inject
import javax.inject.Named


internal const val transfersPreferencesDataStoreName = "transfersPreferencesDataStoreName"

internal class TransfersPreferencesDataStore(
    private val getPreferenceFlow: () -> Flow<Preferences>,
    private val editPreferences: suspend (suspend (MutablePreferences) -> Unit) -> Preferences,
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
) : TransfersPreferencesGateway {

    @Inject
    constructor(
        @Named(transfersPreferencesDataStoreName) dataStore: DataStore<Preferences>,
        encryptData: EncryptData,
        decryptData: DecryptData,
    ) : this(
        getPreferenceFlow = dataStore::data,
        editPreferences = dataStore::edit,
        encryptData = encryptData,
        decryptData = decryptData,
    )

    private val requestFilesPermissionDeniedKey =
        stringPreferencesKey("requestFilesPermissionDeniedKey")

    override suspend fun setRequestFilesPermissionDenied() {
        setRequestFilesPermissionDenied(true)
    }

    override fun monitorRequestFilesPermissionDenied(): Flow<Boolean> =
        getPreferenceFlow().monitor(requestFilesPermissionDeniedKey)
            .map { decryptData(it)?.toBooleanStrictOrNull() ?: false }

    override suspend fun clearPreferences() {
        setRequestFilesPermissionDenied(false)
    }

    private suspend fun setRequestFilesPermissionDenied(value: Boolean) {
        val encryptedValue = encryptData(value.toString()) ?: return
        editPreferences {
            it[requestFilesPermissionDeniedKey] = encryptedValue
        }
    }
}