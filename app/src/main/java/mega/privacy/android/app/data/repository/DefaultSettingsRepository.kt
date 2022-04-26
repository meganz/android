package mega.privacy.android.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.failWithException
import mega.privacy.android.app.data.extensions.isTypeWithParam
import mega.privacy.android.app.data.gateway.MonitorHideRecentActivityFacade
import mega.privacy.android.app.data.gateway.MonitorStartScreenFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.preferences.LoggingPreferencesDataStore
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.SharedPreferenceConstants
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default settings repository implementation
 *
 * @property databaseHandler
 * @property context
 * @property apiFacade
 * @property monitorStartScreenFacade
 * @property monitorHideRecentActivityFacade
 * @property loggingSettingsGateway
 * @property appScope
 * @property ioDispatcher
 */
@ExperimentalContracts
class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    private val apiFacade: MegaApiGateway,
    private val monitorStartScreenFacade: MonitorStartScreenFacade,
    private val monitorHideRecentActivityFacade: MonitorHideRecentActivityFacade,
    private val loggingSettingsGateway: LoggingPreferencesDataStore,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {
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

    override fun isPasscodeLockPreferenceEnabled() =
        databaseHandler.preferences.passcodeLockEnabled.toBoolean()

    override fun setPasscodeLockEnabled(enabled: Boolean) {
        databaseHandler.isPasscodeLockEnabled = enabled
    }

    override suspend fun fetchContactLinksOption(): Boolean = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            apiFacade.isAutoAcceptContactsFromLinkEnabled(
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onGetContactLinksOptionRequestFinished(continuation)
                )
            )
        }
    }

    private fun onGetContactLinksOptionRequestFinished(continuation: Continuation<Boolean>) =
        { request: MegaRequest, error: MegaError ->
            if (isFetchAutoAcceptQRResponse(request)) {
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        continuation.resumeWith(Result.success(request.flag))
                    }
                    MegaError.API_ENOENT -> continuation.failWithException(
                        SettingNotFoundException(
                            error.errorCode,
                            error.errorString
                        )
                    )
                    else -> continuation.failWithError(error)
                }
            }
        }

    private fun isFetchAutoAcceptQRResponse(request: MegaRequest?) =
        request.isTypeWithParam(
            MegaRequest.TYPE_GET_ATTR_USER,
            MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION
        )

    override fun getStartScreen() = getUiPreferences().getInt(
        SharedPreferenceConstants.PREFERRED_START_SCREEN,
        StartScreenUtil.HOME_BNV
    )

    override fun shouldHideRecentActivity() =
        getUiPreferences().getBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false)

    override suspend fun setAutoAcceptQR(accept: Boolean): Boolean =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                apiFacade.setAutoAcceptContactsFromLink(
                    !accept, OptionalMegaRequestListenerInterface(
                        onRequestFinish = onSetContactLinksOptionRequestFinished(
                            continuation,
                            accept
                        )
                    )
                )
            }
        }

    private fun onSetContactLinksOptionRequestFinished(
        continuation: Continuation<Boolean>,
        accept: Boolean
    ) = { request: MegaRequest, error: MegaError ->
        if (isSetAutoAcceptQRResponse(request)) {
            when (error.errorCode) {
                MegaError.API_OK -> {
                    continuation.resumeWith(Result.success(accept))
                }
                else -> continuation.failWithError(error)
            }
        }
    }

    private fun isSetAutoAcceptQRResponse(request: MegaRequest?) =
        request.isTypeWithParam(
            MegaRequest.TYPE_SET_ATTR_USER,
            MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION
        )

    private fun getUiPreferences(): SharedPreferences =
        context.getSharedPreferences(
            SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
            Context.MODE_PRIVATE
        )

    override fun monitorStartScreen(): Flow<Int> = monitorStartScreenFacade.getEvents()

    override fun monitorHideRecentActivity(): Flow<Boolean> =
        monitorHideRecentActivityFacade.getEvents()

    override fun isSdkLoggingEnabled(): SharedFlow<Boolean> =
        loggingSettingsGateway.isLoggingPreferenceEnabled()
            .shareIn(appScope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun setSdkLoggingEnabled(enabled: Boolean) {
        loggingSettingsGateway.setLoggingEnabledPreference(enabled)
    }

    override fun isChatLoggingEnabled(): Flow<Boolean> =
        loggingSettingsGateway.isChatLoggingPreferenceEnabled()

    override suspend fun setChatLoggingEnabled(enabled: Boolean) {
        loggingSettingsGateway.setChatLoggingEnabledPreference(enabled)
    }

    override fun isCameraSyncPreferenceEnabled(): Boolean =
        databaseHandler.preferences?.camSyncEnabled.toBoolean()

    override suspend fun isUseHttpsPreferenceEnabled(): Boolean =
        databaseHandler.useHttpsOnly.toBoolean()

    override suspend fun setUseHttpsPreference(enabled: Boolean) {
        databaseHandler.setUseHttpsOnly(enabled)
    }
}