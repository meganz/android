package mega.privacy.android.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.SharedPreferenceConstants
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    @MegaApi private val sdk: MegaApiAndroid,
) : SettingsRepository {
    private val logPreferences = "LOG_PREFERENCES"
    private val karereLogs = "KARERE_LOGS"
    private val sdkLogs = "SDK_LOGS"
    init {
        initialisePreferences()
    }

    private fun initialisePreferences() {
        if (databaseHandler.preferences == null) {
            LogUtil.logWarning("databaseHandler.preferences is NULL")
            databaseHandler.setStorageAskAlways(true)
            val defaultDownloadLocation = FileUtil.buildDefaultDownloadDir(context)
            defaultDownloadLocation.mkdirs()
            databaseHandler.setStorageDownloadLocation(defaultDownloadLocation.absolutePath)
            databaseHandler.setFirstTime(false)
        }
    }

    override fun setPasscodeLockEnabled(enabled: Boolean) {
        databaseHandler.isPasscodeLockEnabled = enabled
    }

    override fun fetchContactLinksOption(listenerInterface: MegaRequestListenerInterface) {
        sdk.getContactLinksOption(listenerInterface)
    }

    override fun getStartScreen(): Int {
        return getUiPreferences().getInt(
            SharedPreferenceConstants.PREFERRED_START_SCREEN,
            StartScreenUtil.HOME_BNV
        )
    }

    override fun shouldHideRecentActivity(): Boolean {
        return getUiPreferences().getBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false)
    }

    override fun isChatLoggingEnabled(): Boolean {
        return context.getSharedPreferences(logPreferences, Context.MODE_PRIVATE).getBoolean(karereLogs, false)
    }

    override fun isLoggingEnabled(): Boolean {
        return context.getSharedPreferences(logPreferences, Context.MODE_PRIVATE).getBoolean(sdkLogs, false)
    }

    private fun getUiPreferences(): SharedPreferences {
        return context
            .getSharedPreferences(
                SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
                Context.MODE_PRIVATE
            )
    }

    override fun getAttributes(): MegaAttributes = databaseHandler.attributes

    override fun getPreferences(): MegaPreferences = databaseHandler.preferences
}