package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject


private const val ACCOUNT_PREFERENCE = "ACCOUNT_PREFERENCE"
private const val accountPreferenceFileName = ACCOUNT_PREFERENCE

private const val SHOW_2FA_DIALOG = "SHOW_2FA_DIALOG"

private val Context.accountPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = accountPreferenceFileName,
)

/**
 * Stores user account related info to data store
 */
class AccountPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountPreferencesGateway {
    private val show2FADialog = booleanPreferencesKey(SHOW_2FA_DIALOG)

    override suspend fun setDisplay2FADialog(show2FA: Boolean) {
        context.accountPreferencesDataStore.edit {
            it[show2FADialog] = show2FA
        }
    }

    override suspend fun monitorShow2FADialog(): Flow<Boolean> =
        context.accountPreferencesDataStore.monitor(
            show2FADialog
        ).map { return@map it ?: false }

    override suspend fun clearPreferences() {
        withContext(ioDispatcher) {
            context.accountPreferencesDataStore.edit {
                it.clear()
            }
        }
    }
}