package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.InAppUpdatePreferencesGateway
import javax.inject.Inject

private val inAppUpdatePreferenceFileName = "IN_APP_UPDATE"
private val Context.appInfoPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = inAppUpdatePreferenceFileName
)

/**
 * InAppUpdate preferences datastore
 *
 */
internal class InAppUpdatePreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
) : InAppUpdatePreferencesGateway {
    override suspend fun setLastInAppUpdatePromptTime(time: Long) {
        context.appInfoPreferenceDataStore.edit {
            it[KEY_LAST_IN_APP_UPDATE_PROMPT_TIME] = time
        }
    }

    override suspend fun getLastInAppUpdatePromptTime(): Long =
        context.appInfoPreferenceDataStore.monitor(KEY_LAST_IN_APP_UPDATE_PROMPT_TIME).firstOrNull()
            ?: 0L

    override suspend fun incrementInAppUpdatePromptCount() {
        context.appInfoPreferenceDataStore.edit {
            it[KEY_IN_APP_UPDATE_PROMPT_COUNT] = getInAppUpdatePromptCount() + 1
        }
    }

    override suspend fun getInAppUpdatePromptCount(): Int =
        context.appInfoPreferenceDataStore.monitor(KEY_IN_APP_UPDATE_PROMPT_COUNT).firstOrNull()
            ?: 0

    override suspend fun setInAppUpdatePromptCount(count: Int) {
        context.appInfoPreferenceDataStore.edit {
            it[KEY_IN_APP_UPDATE_PROMPT_COUNT] = count
        }
    }

    override suspend fun getLastInAppUpdatePromptVersion(): Int =
        context.appInfoPreferenceDataStore.monitor(KEY_IN_APP_UPDATE_PROMPT_VERSION).firstOrNull()
            ?: 0

    override suspend fun setLastInAppUpdatePromptVersion(version: Int) {
        context.appInfoPreferenceDataStore.edit {
            it[KEY_IN_APP_UPDATE_PROMPT_VERSION] = version
        }
    }

    override suspend fun getInAppUpdateNeverShowAgain(): Boolean =
        context.appInfoPreferenceDataStore.monitor(KEY_IN_APP_UPDATE_NEVER_SHOW_AGAIN).firstOrNull()
            ?: false

    override suspend fun setInAppUpdateNeverShowAgain(value: Boolean) {
        context.appInfoPreferenceDataStore.edit {
            it[KEY_IN_APP_UPDATE_NEVER_SHOW_AGAIN] = value
        }
    }


    companion object {
        private val KEY_LAST_IN_APP_UPDATE_PROMPT_TIME =
            longPreferencesKey("KEY_LAST_IN_APP_UPDATE_PROMPT_TIME")

        private val KEY_IN_APP_UPDATE_PROMPT_COUNT =
            intPreferencesKey("KEY_IN_APP_UPDATE_PROMPT_COUNT")

        private val KEY_IN_APP_UPDATE_PROMPT_VERSION =
            intPreferencesKey("KEY_IN_APP_UPDATE_PROMPT_VERSION")

        private val KEY_IN_APP_UPDATE_NEVER_SHOW_AGAIN =
            booleanPreferencesKey("KEY_IN_APP_UPDATE_NEVER_SHOW_AGAIN")
    }

}