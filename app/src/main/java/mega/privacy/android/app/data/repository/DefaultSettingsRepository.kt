package mega.privacy.android.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.isTypeWithParam
import mega.privacy.android.app.data.gateway.MonitorHideRecentActivityFacade
import mega.privacy.android.app.data.gateway.MonitorStartScreenFacade
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.SharedPreferenceConstants
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SettingsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
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
 * @property ioDispatcher
 * @property chatPreferencesGateway
 * @property callsPreferencesGateway

 */
@ExperimentalContracts
class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    private val apiFacade: MegaApiGateway,
    private val monitorStartScreenFacade: MonitorStartScreenFacade,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val monitorHideRecentActivityFacade: MonitorHideRecentActivityFacade,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val chatPreferencesGateway: ChatPreferencesGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val cacheFolderGateway: CacheFolderGateway,
    private val uiPreferencesGateway: UIPreferencesGateway,
    private val startScreenMapper: StartScreenMapper,
) : SettingsRepository {
    init {
        initialisePreferences()
    }

    private fun initialisePreferences() {
        if (databaseHandler.preferences == null) {
            Timber.w("databaseHandler.preferences is NULL")
            databaseHandler.setStorageAskAlways(true)
            val defaultDownloadLocation = FileUtil.buildDefaultDownloadDir(context)
            defaultDownloadLocation.mkdirs()
            databaseHandler.setStorageDownloadLocation(defaultDownloadLocation.absolutePath)
        }
    }

    override fun isPasscodeLockPreferenceEnabled() =
        databaseHandler.preferences?.passcodeLockEnabled.toBoolean()

    override fun setPasscodeLockEnabled(enabled: Boolean) {
        megaLocalStorageGateway.setPasscodeLockEnabled(enabled)
    }

    override suspend fun setPasscodeLockCode(passcodeLockCode: String) =
        megaLocalStorageGateway.setPasscodeLockCode(passcodeLockCode)

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
        accept: Boolean,
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

    override fun monitorHideRecentActivityEvent(): Flow<Boolean> =
        monitorHideRecentActivityFacade.getEvents()

    override fun monitorHideRecentActivity(): Flow<Boolean?> =
        uiPreferencesGateway.monitorHideRecentActivity()

    override suspend fun setHideRecentActivity(value: Boolean) =
        uiPreferencesGateway.setHideRecentActivity(value)

    override fun isCameraSyncPreferenceEnabled(): Boolean =
        databaseHandler.preferences?.camSyncEnabled.toBoolean()

    override suspend fun setEnableCameraUpload(enable: Boolean) {
        megaLocalStorageGateway.setCamSyncEnabled(enable)
    }

    override suspend fun setCameraUploadVideoQuality(quality: Int) {
        megaLocalStorageGateway.setCameraUploadVideoQuality(quality)
    }

    override suspend fun setCameraUploadFileType(syncVideo: Boolean) {
        val fileUpload =
            if (syncVideo) MegaPreferences.PHOTOS_AND_VIDEOS else MegaPreferences.ONLY_PHOTOS
        megaLocalStorageGateway.setCamSyncFileUpload(fileUpload)
    }

    override suspend fun setCamSyncWifi(enableCellularSync: Boolean) {
        megaLocalStorageGateway.setCamSyncWifi(enableCellularSync)
    }

    override suspend fun setCameraUploadLocalPath(path: String?) {
        megaLocalStorageGateway.setCamSyncLocalPath(path)
    }

    override suspend fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean) =
        megaLocalStorageGateway.setCameraFolderExternalSDCard(cameraFolderExternalSDCard)

    override suspend fun setConversionOnCharging(onCharging: Boolean) =
        megaLocalStorageGateway.setConversionOnCharging(onCharging)

    override suspend fun setChargingOnSize(size: Int) {
        megaLocalStorageGateway.setChargingOnSize(size)
    }

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) =
        megaLocalStorageGateway.setStorageAskAlways(isStorageAskAlways)

    override suspend fun setDefaultStorageDownloadLocation() {
        val defaultDownloadLocation = cacheFolderGateway.buildDefaultDownloadDir()
        defaultDownloadLocation.mkdirs()
        megaLocalStorageGateway.setStorageDownloadLocation(defaultDownloadLocation.absolutePath)
    }


    override suspend fun setStorageDownloadLocation(storageDownloadLocation: String) =
        megaLocalStorageGateway.setStorageDownloadLocation(storageDownloadLocation)

    override suspend fun setShowCopyright() {
        if (apiFacade.getPublicLinks().isEmpty()) {
            Timber.d("No public links: showCopyright set true")
            megaLocalStorageGateway.setShowCopyright(true)
        } else {
            Timber.d("Already public links: showCopyright set false")
            megaLocalStorageGateway.setShowCopyright(false)
        }
    }


    override suspend fun isUseHttpsPreferenceEnabled(): Boolean =
        databaseHandler.useHttpsOnly.toBoolean()

    override suspend fun setUseHttpsPreference(enabled: Boolean) {
        databaseHandler.setUseHttpsOnly(enabled)
    }

    override fun getChatImageQuality(): Flow<ChatImageQuality> =
        chatPreferencesGateway.getChatImageQualityPreference()

    override suspend fun setChatImageQuality(quality: ChatImageQuality) =
        withContext(ioDispatcher) { chatPreferencesGateway.setChatImageQualityPreference(quality) }

    override fun getCallsSoundNotifications(): Flow<CallsSoundNotifications> =
        callsPreferencesGateway.getCallsSoundNotificationsPreference()

    override suspend fun setCallsSoundNotifications(soundNotifications: CallsSoundNotifications) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setCallsSoundNotificationsPreference(soundNotifications)
        }

    override suspend fun setStringPreference(key: String?, value: String?) =
        setPreference(key to value, appPreferencesGateway::putString)

    override suspend fun setStringSetPreference(key: String?, value: MutableSet<String>?) =
        setPreference(key to value, appPreferencesGateway::putStringSet)

    override suspend fun setIntPreference(key: String?, value: Int?) =
        setPreference(key to value, appPreferencesGateway::putInt)

    override suspend fun setLongPreference(key: String?, value: Long?) =
        setPreference(key to value, appPreferencesGateway::putLong)

    override suspend fun setFloatPreference(key: String?, value: Float?) =
        setPreference(key to value, appPreferencesGateway::putFloat)

    override suspend fun setBooleanPreference(key: String?, value: Boolean?) =
        setPreference(key to value, appPreferencesGateway::putBoolean)

    private suspend fun <T> setPreference(
        preference: Pair<String?, T?>,
        setFunction: suspend (String, T) -> Unit,
    ) {
        val (key, value) = preference.toNonNullPairOrNull()
            ?: Timber.w("Failed to set value ${preference.second} on preference ${preference.first}")
                .run { return }
        withContext(ioDispatcher) { setFunction(key, value) }
    }

    private fun <A, B> Pair<A?, B?>.toNonNullPairOrNull(): Pair<A, B>? =
        this.takeIf { first != null && second != null }?.let { Pair(first!!, second!!) }

    override fun monitorStringPreference(key: String?, defaultValue: String?) =
        monitorPreference(key, defaultValue, appPreferencesGateway::monitorString)

    override fun monitorStringSetPreference(
        key: String?,
        defaultValue: MutableSet<String>?,
    ) = monitorPreference(key, defaultValue, appPreferencesGateway::monitorStringSet)

    override fun monitorIntPreference(key: String?, defaultValue: Int) =
        monitorPreference(key, defaultValue, appPreferencesGateway::monitorInt)

    override fun monitorLongPreference(key: String?, defaultValue: Long) =
        monitorPreference(key, defaultValue, appPreferencesGateway::monitorLong)

    override fun monitorFloatPreference(key: String?, defaultValue: Float) =
        monitorPreference(key, defaultValue, appPreferencesGateway::monitorFloat)

    override fun monitorBooleanPreference(key: String?, defaultValue: Boolean) =
        monitorPreference(key, defaultValue, appPreferencesGateway::monitorBoolean)

    private fun <T> monitorPreference(
        key: String?,
        defaultValue: T,
        monitorFunction: (String, T) -> Flow<T>,
    ) = if (key.isNullOrBlank()) {
        Timber.w("Failed to fetch preference with an empty or null key")
        emptyFlow()
    } else {
        monitorFunction(key, defaultValue)
    }

    override fun getLastContactPermissionDismissedTime(): Flow<Long> {
        return chatPreferencesGateway.getLastContactPermissionRequestedTime()
    }

    override suspend fun setLastContactPermissionDismissedTime(time: Long) {
        chatPreferencesGateway.setLastContactPermissionRequestedTime(time)
    }

    override fun monitorPreferredStartScreen() =
        uiPreferencesGateway.monitorPreferredStartScreen()
            .map { startScreenMapper(it) }

    override suspend fun setPreferredStartScreen(screen: StartScreen) {
        uiPreferencesGateway.setPreferredStartScreen(screen.id)
    }
}