package mega.privacy.android.data.facade

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import javax.inject.Inject

/**
 * [FileManagementPreferencesGateway] implementation
 *
 * @property context context
 */
internal class FileManagementPreferencesFacade @Inject constructor(
    @ApplicationContext val context: Context,
) : FileManagementPreferencesGateway {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override suspend fun isMobileDataAllowed(): Boolean {
        return preferences.getBoolean(KEY_MOBILE_DATA_HIGH_RESOLUTION, true)
    }

    companion object {
        private const val KEY_MOBILE_DATA_HIGH_RESOLUTION = "setting_mobile_data_high_resolution"
    }
}