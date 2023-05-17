package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.IOException
import javax.inject.Inject


private const val ACCOUNT_PREFERENCE = "ACCOUNT_PREFERENCE"
private const val accountPreferenceFileName = ACCOUNT_PREFERENCE

private const val SHOW_2FA_DIALOG = "SHOW_2FA_DIALOG"
private const val LATEST_TARGET_PATH_COPY = "LATEST_TARGET_PATH_COPY"
private const val LATEST_TARGET_PATH_MOVE = "LATEST_TARGET_PATH_MOVE"
private const val LATEST_TARGET_PATH_TIMESTAMP_COPY = "LATEST_TARGET_PATH_TIMESTAMP_COPY"
private const val LATEST_TARGET_PATH_TIMESTAMP_MOVE = "LATEST_TARGET_PATH_TIMESTAMP_MOVE"

private val Context.accountPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = accountPreferenceFileName,
)

/**
 * Stores user account related info to data store
 *
 * @property context
 * @property ioDispatcher
 */
class AccountPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountPreferencesGateway {

    private val show2FADialog = booleanPreferencesKey(SHOW_2FA_DIALOG)
    private val latestTargetPathCopyPreferenceKey = longPreferencesKey(LATEST_TARGET_PATH_COPY)
    private val latestTargetPathMovePreferenceKey = longPreferencesKey(LATEST_TARGET_PATH_MOVE)
    private val latestTargetPathTimestampCopyPreferenceKey =
        longPreferencesKey(LATEST_TARGET_PATH_TIMESTAMP_COPY)
    private val latestTargetPathTimestampMovePreferenceKey =
        longPreferencesKey(LATEST_TARGET_PATH_TIMESTAMP_MOVE)

    override suspend fun setDisplay2FADialog(show2FA: Boolean) {
        context.accountPreferencesDataStore.edit {
            it[show2FADialog] = show2FA
        }
    }

    override fun monitorShow2FADialog(): Flow<Boolean> =
        context.accountPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { it[show2FADialog] ?: false }

    override suspend fun setLatestTargetPathCopyPreference(path: Long) {
        context.accountPreferencesDataStore.edit {
            it[latestTargetPathCopyPreferenceKey] = path
        }
    }

    override fun getLatestTargetPathCopyPreference(): Flow<Long?> =
        context.accountPreferencesDataStore.monitor(latestTargetPathCopyPreferenceKey)

    override suspend fun setLatestTargetTimestampCopyPreference(timestamp: Long) {
        context.accountPreferencesDataStore.edit {
            it[latestTargetPathTimestampCopyPreferenceKey] = timestamp
        }
    }

    override fun getLatestTargetTimestampCopyPreference(): Flow<Long?> =
        context.accountPreferencesDataStore.monitor(latestTargetPathTimestampCopyPreferenceKey)

    override suspend fun setLatestTargetPathMovePreference(path: Long) {
        context.accountPreferencesDataStore.edit {
            it[latestTargetPathMovePreferenceKey] = path
        }
    }

    override fun getLatestTargetPathMovePreference(): Flow<Long?> =
        context.accountPreferencesDataStore.monitor(latestTargetPathMovePreferenceKey)

    override suspend fun setLatestTargetTimestampMovePreference(timestamp: Long) {
        context.accountPreferencesDataStore.edit {
            it[latestTargetPathTimestampMovePreferenceKey] = timestamp
        }
    }

    override fun getLatestTargetTimestampMovePreference(): Flow<Long?> =
        context.accountPreferencesDataStore.monitor(latestTargetPathTimestampMovePreferenceKey)

    override suspend fun clearPreferences() {
        withContext(ioDispatcher) {
            context.accountPreferencesDataStore.edit {
                it.clear()
            }
        }
    }
}