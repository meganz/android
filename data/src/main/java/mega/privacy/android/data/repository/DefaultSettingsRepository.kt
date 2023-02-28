package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.isTypeWithParam
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.mapper.VideoQualityIntMapper
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SettingsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default settings repository implementation
 *
 * @property databaseHandler
 * @property context
 * @property megaApiGateway
 * @property ioDispatcher
 * @property chatPreferencesGateway
 * @property callsPreferencesGateway
 * @property appPreferencesGateway
 * @property cacheFolderGateway
 * @property uiPreferencesGateway
 * @property startScreenMapper
 * @property cameraTimestampsPreferenceGateway
 */
@ExperimentalContracts
internal class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val chatPreferencesGateway: ChatPreferencesGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val cacheFolderGateway: CacheFolderGateway,
    private val uiPreferencesGateway: UIPreferencesGateway,
    private val startScreenMapper: StartScreenMapper,
    private val cameraTimestampsPreferenceGateway: CameraTimestampsPreferenceGateway,
    private val videoQualityIntMapper: VideoQualityIntMapper,
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway,
) : SettingsRepository {
    init {
        runBlocking {
            initialisePreferences()
        }
    }

    private suspend fun initialisePreferences() {
        if (databaseHandler.preferences == null) {
            Timber.w("databaseHandler.preferences is NULL")
            databaseHandler.setStorageAskAlways(true)
            val defaultDownloadLocation = cacheFolderGateway.buildDefaultDownloadDir()
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
            megaApiGateway.isAutoAcceptContactsFromLinkEnabled(
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
                            error.errorCode, error.errorString
                        )
                    )
                    else -> continuation.failWithError(error)
                }
            }
        }

    private fun isFetchAutoAcceptQRResponse(request: MegaRequest?) = request.isTypeWithParam(
        TYPE_GET_ATTR_USER, MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION
    )

    override suspend fun setAutoAcceptQR(accept: Boolean): Boolean = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.setAutoAcceptContactsFromLink(
                !accept, OptionalMegaRequestListenerInterface(
                    onRequestFinish = onSetContactLinksOptionRequestFinished(
                        continuation, accept
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

    private fun isSetAutoAcceptQRResponse(request: MegaRequest?) = request.isTypeWithParam(
        MegaRequest.TYPE_SET_ATTR_USER, MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION
    )

    override fun monitorHideRecentActivity(): Flow<Boolean?> =
        uiPreferencesGateway.monitorHideRecentActivity()

    override suspend fun setHideRecentActivity(value: Boolean) =
        uiPreferencesGateway.setHideRecentActivity(value)

    override fun monitorMediaDiscoveryView(): Flow<Int?> =
        uiPreferencesGateway.monitorMediaDiscoveryView()

    override suspend fun setMediaDiscoveryView(value: Int) =
        uiPreferencesGateway.setMediaDiscoveryView(value)

    override fun isCameraSyncPreferenceEnabled(): Boolean =
        databaseHandler.preferences?.camSyncEnabled.toBoolean()

    override suspend fun setEnableCameraUpload(enable: Boolean) {
        megaLocalStorageGateway.setCamSyncEnabled(enable)
    }

    override suspend fun setCameraUploadVideoQuality(quality: VideoQuality) {
        megaLocalStorageGateway.setCameraUploadVideoQuality(videoQualityIntMapper(quality))
    }

    override suspend fun setCameraUploadFileType(syncVideo: Boolean) {
        val fileUpload =
            if (syncVideo) MegaPreferences.PHOTOS_AND_VIDEOS else MegaPreferences.ONLY_PHOTOS
        megaLocalStorageGateway.setCamSyncFileUpload(fileUpload)
    }

    override suspend fun setCamSyncWifi(wifiOnly: Boolean) {
        megaLocalStorageGateway.setCamSyncWifi(wifiOnly)
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

    override suspend fun getStorageDownloadAskAlways(): Boolean {
        return megaLocalStorageGateway.getStorageAskAlways()
    }

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) =
        megaLocalStorageGateway.setStorageAskAlways(isStorageAskAlways)

    override suspend fun setDefaultStorageDownloadLocation() {
        val defaultDownloadLocation = cacheFolderGateway.buildDefaultDownloadDir()
        defaultDownloadLocation.mkdirs()
        megaLocalStorageGateway.setStorageDownloadLocation(defaultDownloadLocation.absolutePath)
    }

    override suspend fun getStorageDownloadLocation(): String? {
        return megaLocalStorageGateway.getStorageDownloadLocation()
    }

    override suspend fun setStorageDownloadLocation(storageDownloadLocation: String) =
        megaLocalStorageGateway.setStorageDownloadLocation(storageDownloadLocation)

    override suspend fun setShowCopyright() {
        if (megaApiGateway.getPublicLinks().isEmpty()) {
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

    override fun getLastContactPermissionDismissedTime(): Flow<Long> =
        chatPreferencesGateway.getLastContactPermissionRequestedTime()

    override suspend fun setLastContactPermissionDismissedTime(time: Long) {
        chatPreferencesGateway.setLastContactPermissionRequestedTime(time)
    }

    override fun monitorPreferredStartScreen() =
        uiPreferencesGateway.monitorPreferredStartScreen().map { startScreenMapper(it) }

    override suspend fun setPreferredStartScreen(screen: StartScreen) {
        uiPreferencesGateway.setPreferredStartScreen(screen.id)
    }

    override suspend fun backupTimestampsAndFolderHandle(
        primaryUploadFolderHandle: Long,
        secondaryUploadFolderHandle: Long,
        camSyncTimeStamp: Long?,
        camVideoSyncTimeStamp: Long?,
        secSyncTimeStamp: Long?,
        secVideoSyncTimeStamp: Long?,
    ) {
        withContext(ioDispatcher) {
            cameraTimestampsPreferenceGateway.backupTimestampsAndFolderHandle(
                primaryUploadFolderHandle,
                secondaryUploadFolderHandle,
                camSyncTimeStamp?.toString(),
                camVideoSyncTimeStamp?.toString(),
                secSyncTimeStamp?.toString(),
                secVideoSyncTimeStamp?.toString()
            )
        }
    }

    override suspend fun getPrimaryHandle() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getPrimaryHandle()
    }

    override suspend fun getSecondaryHandle() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getSecondaryHandle()
    }

    override suspend fun getPrimaryFolderPhotoSyncTime() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getPrimaryFolderPhotoSyncTime()
    }

    override suspend fun getSecondaryFolderPhotoSyncTime() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getSecondaryFolderPhotoSyncTime()
    }

    override suspend fun getPrimaryFolderVideoSyncTime() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getPrimaryFolderVideoSyncTime()
    }

    override suspend fun getSecondaryFolderVideoSyncTime() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.getSecondaryFolderVideoSyncTime()
    }

    override suspend fun clearPrimaryCameraSyncRecords() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.clearPrimaryCameraSyncRecords()
    }

    override suspend fun clearSecondaryCameraSyncRecords() = withContext(ioDispatcher) {
        cameraTimestampsPreferenceGateway.clearSecondaryCameraSyncRecords()
    }

    override suspend fun enableFileVersionsOption(enabled: Boolean) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener {
                return@getRequestListener
            }
            megaApiGateway.setFileVersionsOption(enabled.not(), listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun buildDefaultDownloadDir(): File {
        return cacheFolderGateway.buildDefaultDownloadDir()
    }

    override suspend fun isMobileDataAllowed() = withContext(ioDispatcher) {
        fileManagementPreferencesGateway.isMobileDataAllowed()
    }

    override suspend fun getExportMasterKey(): String? = withContext(ioDispatcher) {
        megaApiGateway.getExportMasterKey()
    }

    override suspend fun setMasterKeyExported() = withContext(ioDispatcher) {
        megaApiGateway.setMasterKeyExported(null)
    }

    override suspend fun enableMultiFactorAuth(pin: String): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener { it.flag }
            megaApiGateway.enableMultiFactorAuth(pin, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun isMasterKeyExported(): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            megaApiGateway.isMasterKeyExported(
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onIsMasterKeyExportedRequestFinished(
                        continuation = continuation
                    )
                )
            )
        }
    }

    private fun onIsMasterKeyExportedRequestFinished(continuation: Continuation<Boolean>) =
        { request: MegaRequest, error: MegaError ->
            if (request.access == TYPE_GET_ATTR_USER
                && request.paramType == MegaApiJava.USER_ATTR_PWD_REMINDER
            ) {
                Timber.d("TYPE_GET_ATTR_USER")
                if (error.errorCode == MegaError.API_OK
                    || error.errorCode == MegaError.API_ENOENT
                ) {
                    Timber.d("TYPE_GET_ATTR_USER API_OK")
                    continuation.resumeWith(Result.success(request.flag))
                } else continuation.failWithError(error)
            }
        }
}
