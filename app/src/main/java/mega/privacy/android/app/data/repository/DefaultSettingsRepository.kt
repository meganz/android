package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    @MegaApi private val sdk: MegaApiAndroid,
) : SettingsRepository {

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

    override fun getAttributes(): MegaAttributes = databaseHandler.attributes

    override fun getPreferences(): MegaPreferences = databaseHandler.preferences
}