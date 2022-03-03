package mega.privacy.android.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.data.gateway.MonitorHideRecentActivity
import mega.privacy.android.app.data.gateway.MonitorStartScreen
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.SharedPreferenceConstants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    @MegaApi private val sdk: MegaApiAndroid,
    private val monitorStartScreen: MonitorStartScreen,
    private val monitorHideRecentActivity: MonitorHideRecentActivity,
) : SettingsRepository {

    /*
    Duplicate constants used here to remove dependency on LogUtil static class.
     */
    private val logPreferences = "LOG_PREFERENCES"
    private val karereLogs = "KARERE_LOGS"
    private val sdkLogs = "SDK_LOGS"

    init {
        initialisePreferences()
    }

    private fun initialisePreferences() {
        if (databaseHandler.preferences == null) {
            logWarning("databaseHandler.preferences is NULL")
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

    override suspend fun fetchContactLinksOption(): Boolean {
        return suspendCoroutine { continuation ->
            sdk.getContactLinksOption(object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                    if (isFetchAutoAcceptQRResponse(request)) {
                        when (e?.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(request!!.flag))
                            }
                            MegaError.API_ENOENT -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        SettingNotFoundException(
                                            e.errorCode,
                                            e.errorString
                                        )
                                    )
                                )
                            }
                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        MegaException(
                                            e?.errorCode,
                                            e?.errorString
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                }
            })
        }
    }

    private fun isFetchAutoAcceptQRResponse(request: MegaRequest?) =
        request?.type == MegaRequest.TYPE_GET_ATTR_USER && request.paramType == MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION

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
        return context.getSharedPreferences(logPreferences, Context.MODE_PRIVATE)
            .getBoolean(karereLogs, false)
    }

    override fun setChatLoggingEnabled(enabled: Boolean) {
        if (!enabled) {
            logInfo("Karere logs are now disabled - App Version: " + Util.getVersion())
        }

        context.getSharedPreferences(LOG_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean(KARERE_LOGS, enabled).apply()

        if (enabled) {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
            logInfo("Karere logs are now enabled - App Version: " + Util.getVersion())
        } else {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        }
    }

    override fun isLoggingEnabled(): Boolean {
        return context.getSharedPreferences(logPreferences, Context.MODE_PRIVATE)
            .getBoolean(sdkLogs, false)
    }

    override fun setLoggingEnabled(enabled: Boolean) {
        if (!enabled) {
            logInfo("SDK logs are now disabled - App Version: " + Util.getVersion())
        }

        context.getSharedPreferences(LOG_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean(SDK_LOGS, enabled).apply()
        if (enabled) {
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
            logInfo("SDK logs are now enabled - App Version: " + Util.getVersion())
        } else {
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
        }
    }

    override suspend fun setAutoAcceptQR(accept: Boolean): Boolean {

        return suspendCoroutine { continuation ->
            sdk.setContactLinksOption(!accept, object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                    if (isSetAutoAcceptQRResponse(request)) {
                        when (e?.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(accept))
                            }
                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        MegaException(
                                            e?.errorCode,
                                            e?.errorString
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                }
            })
        }
    }

    private fun isSetAutoAcceptQRResponse(request: MegaRequest?) =
        request?.type == MegaRequest.TYPE_SET_ATTR_USER && request.paramType == MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION

    private fun getUiPreferences(): SharedPreferences {
        return context
            .getSharedPreferences(
                SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
                Context.MODE_PRIVATE
            )
    }

    override fun getAttributes(): MegaAttributes = databaseHandler.attributes

    override fun getPreferences(): MegaPreferences = databaseHandler.preferences

    override fun monitorStartScreen(): Flow<Int> {
        return monitorStartScreen.getEvents()
    }

    override fun monitorHideRecentActivity(): Flow<Boolean> {
        return monitorHideRecentActivity.getEvents()
    }
}