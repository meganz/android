package mega.privacy.android.data.repository

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.decodeBase64
import mega.privacy.android.data.extensions.encodeBase64
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getValueFor
import mega.privacy.android.data.extensions.hasParam
import mega.privacy.android.data.extensions.isTypeWithParam
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.qualifier.FileVersionsOption
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON.JSON_KEY_ANDROID
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON.JSON_SENSITIVES
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON.JSON_VAL_SHOW_HIDDEN_NODES
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SettingsRepository
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER
import nz.mega.sdk.MegaStringMap
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default settings repository implementation
 *
 * @property databaseHandler [DatabaseHandler]
 * @property context [Context]
 * @property megaApiGateway [MegaApiGateway]
 * @property megaLocalStorageGateway [MegaLocalStorageGateway]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property chatPreferencesGateway [ChatPreferencesGateway]
 * @property callsPreferencesGateway [CallsPreferencesGateway]
 * @property appPreferencesGateway [AppPreferencesGateway]
 * @property fileGateway [FileGateway]
 * @property uiPreferencesGateway [UIPreferencesGateway]
 * @property startScreenMapper [StartScreenMapper]
 * @property fileManagementPreferencesGateway [FileManagementPreferencesGateway]
 */
@ExperimentalContracts
internal class DefaultSettingsRepository @Inject constructor(
    private val databaseHandler: Lazy<DatabaseHandler>,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val chatPreferencesGateway: ChatPreferencesGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val fileGateway: FileGateway,
    private val uiPreferencesGateway: UIPreferencesGateway,
    private val startScreenMapper: StartScreenMapper,
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway,
    private val myAccountInfoFacade: AccountInfoWrapper,
    @FileVersionsOption private val fileVersionsOptionCache: Cache<Boolean>,
) : SettingsRepository {
    private val showHiddenNodesFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Volatile
    private var isShowHiddenNodesPopulated: Boolean = false

    override suspend fun getContactLinksOption(): Boolean = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.getContactLinksOption(
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
                    API_OK -> {
                        continuation.resumeWith(Result.success(request.flag))
                    }

                    API_ENOENT -> continuation.failWithException(
                        SettingNotFoundException(
                            error.errorCode, error.errorString
                        )
                    )

                    else -> continuation.failWithError(
                        error,
                        "onGetContactLinksOptionRequestFinished"
                    )
                }
            }
        }

    private fun isFetchAutoAcceptQRResponse(request: MegaRequest?) = request.isTypeWithParam(
        TYPE_GET_ATTR_USER, MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION
    )

    override suspend fun setContactLinksOption(enable: Boolean): Boolean =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.setContactLinksOption(
                    enable, OptionalMegaRequestListenerInterface(
                        onRequestFinish = onSetContactLinksOptionRequestFinished(
                            continuation
                        )
                    )
                )
            }
        }

    private fun onSetContactLinksOptionRequestFinished(
        continuation: Continuation<Boolean>,
    ) = { request: MegaRequest, error: MegaError ->
        if (isSetAutoAcceptQRResponse(request)) {
            when (error.errorCode) {
                API_OK -> {
                    continuation.resumeWith(Result.success(request.flag))
                }

                else -> continuation.failWithError(error, "onSetContactLinksOptionRequestFinished")
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

    override fun monitorSubfolderMediaDiscoveryEnabled(): Flow<Boolean?> =
        uiPreferencesGateway.monitorSubfolderMediaDiscoveryEnabled()

    override suspend fun setSubfolderMediaDiscoveryEnabled(enabled: Boolean) =
        uiPreferencesGateway.setSubfolderMediaDiscoveryEnabled(enabled)

    override fun monitorShowHiddenItems(): Flow<Boolean> = showHiddenNodesFlow
        .onStart {
            if (!isShowHiddenNodesPopulated) {
                isShowHiddenNodesPopulated = true
                populateShowHiddenNodesPreference()
            }
        }

    private suspend fun populateShowHiddenNodesPreference() {
        val isEnabled = getShowHiddenNodesPreference()
        showHiddenNodesFlow.update { isEnabled }
    }

    override suspend fun setShowHiddenItems(enabled: Boolean) {
        showHiddenNodesFlow.update { enabled }
        setShowHiddenNodesPreference(enabled)
    }

    override suspend fun setOfflineWarningMessageVisibility(isVisible: Boolean) =
        uiPreferencesGateway.setOfflineWarningMessageVisibility(isVisible)

    override fun monitorOfflineWarningMessageVisibility(): Flow<Boolean?> =
        uiPreferencesGateway.monitorOfflineWarningMessageVisibility()

    override suspend fun setDefaultDownloadLocation() {
        val defaultDownloadLocation = fileGateway.buildDefaultDownloadDir()
        defaultDownloadLocation.mkdirs()
        megaLocalStorageGateway.setDownloadLocation(defaultDownloadLocation.absolutePath)
    }

    override suspend fun getDownloadLocation(): String? {
        return megaLocalStorageGateway.getDownloadLocation()
    }

    override suspend fun isAskForDownloadLocation(): Boolean {
        return megaLocalStorageGateway.isAskForDownloadLocation()
    }

    override suspend fun setAskForDownloadLocation(askForDownloadLocation: Boolean) =
        megaLocalStorageGateway.setAskForDownloadLocation(askForDownloadLocation)

    override suspend fun isShouldPromptToSaveDestination() = withContext(ioDispatcher) {
        megaLocalStorageGateway.isShouldPromptToSaveDestination()
    }

    override suspend fun setShouldPromptToSaveDestination(value: Boolean) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.setShouldPromptToSaveDestination(value)
        }

    override suspend fun setDownloadLocation(downloadLocation: String?) =
        megaLocalStorageGateway.setDownloadLocation(downloadLocation)

    override suspend fun isAskBeforeLargeDownloads() =
        megaLocalStorageGateway.isAskBeforeLargeDownloads()

    override suspend fun setAskBeforeLargeDownloads(askForConfirmation: Boolean) =
        megaLocalStorageGateway.setAskBeforeLargeDownloads(askForConfirmation)

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
        databaseHandler.get().useHttpsOnly.toBoolean()

    override suspend fun setUseHttpsPreference(enabled: Boolean) {
        databaseHandler.get().setUseHttpsOnly(enabled)
    }

    override fun getChatImageQuality(): Flow<ChatImageQuality> =
        chatPreferencesGateway.getChatImageQualityPreference()

    override suspend fun getChatVideoQualityPreference(): VideoQuality =
        chatPreferencesGateway.getChatVideoQualityPreference()

    override suspend fun setChatImageQuality(quality: ChatImageQuality) =
        withContext(ioDispatcher) { chatPreferencesGateway.setChatImageQualityPreference(quality) }

    override fun getCallsSoundNotifications(): Flow<CallsSoundEnabledState> =
        callsPreferencesGateway.getCallsSoundNotificationsPreference()

    override suspend fun setCallsSoundNotifications(soundNotifications: CallsSoundEnabledState) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setCallsSoundNotificationsPreference(soundNotifications)
        }

    override fun getCallsMeetingInvitations(): Flow<CallsMeetingInvitations> =
        callsPreferencesGateway.getCallsMeetingInvitationsPreference()

    override suspend fun setCallsMeetingInvitations(callsMeetingInvitations: CallsMeetingInvitations) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setCallsMeetingInvitationsPreference(callsMeetingInvitations)
        }

    override fun getCallsMeetingReminders(): Flow<CallsMeetingReminders> =
        callsPreferencesGateway.getCallsMeetingRemindersPreference()

    override suspend fun setCallsMeetingReminders(callsMeetingReminders: CallsMeetingReminders) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setCallsMeetingRemindersPreference(callsMeetingReminders)
        }

    override fun getWaitingRoomReminders(): Flow<WaitingRoomReminders> =
        callsPreferencesGateway.getWaitingRoomRemindersPreference()

    override suspend fun setWaitingRoomReminders(waitingRoomReminders: WaitingRoomReminders) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setWaitingRoomRemindersPreference(waitingRoomReminders)
        }

    override fun getUsersCallLimitReminders(): Flow<UsersCallLimitReminders> =
        callsPreferencesGateway.getUsersCallLimitRemindersPreference()

    override suspend fun setUsersCallLimitReminders(usersCallLimitReminders: UsersCallLimitReminders) =
        withContext(ioDispatcher) {
            callsPreferencesGateway.setUsersCallLimitRemindersPreference(usersCallLimitReminders)
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

    override suspend fun enableFileVersionsOption(enabled: Boolean) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("enableFileVersionsOption") {
                return@getRequestListener
            }
            megaApiGateway.setFileVersionsOption(enabled.not(), listener)
        }
    }

    override suspend fun buildDefaultDownloadDir(): File {
        return fileGateway.buildDefaultDownloadDir()
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
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = onEnableMultiFactorAuthRequestFinished(continuation)
            )
            megaApiGateway.enableMultiFactorAuth(pin, listener)
        }
    }

    private fun onEnableMultiFactorAuthRequestFinished(
        continuation: Continuation<Boolean>,
    ) =
        { request: MegaRequest, error: MegaError ->
            if (request.flag && error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else if (request.flag && error.errorCode == MegaError.API_EFAILED) {
                continuation.failWithException(
                    EnableMultiFactorAuthException(
                        error.errorCode, error.errorString
                    )
                )
            } else continuation.failWithError(error, "onEnableMultiFactorAuthRequestFinished")
        }

    override suspend fun getMultiFactorAuthCode(): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getMultiFactorAuthCode") { it.text }
            megaApiGateway.getMultiFactorAuthCode(listener)
        }
    }

    override suspend fun isMasterKeyExported(): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = onIsMasterKeyExportedRequestFinished(
                    continuation = continuation
                )
            )
            megaApiGateway.isMasterKeyExported(listener)
        }
    }

    private fun onIsMasterKeyExportedRequestFinished(continuation: Continuation<Boolean>) =
        { request: MegaRequest, error: MegaError ->
            if (request.hasParam(MegaApiJava.USER_ATTR_PWD_REMINDER)
                && (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT)
            ) {
                continuation.resumeWith(
                    Result.success(
                        error.errorCode == MegaError.API_OK && request.access == 1
                    )
                )
            } else {
                continuation.failWithError(error, "onIsMasterKeyExportedRequestFinished")
            }
        }

    override suspend fun resetSetting() {
        setSubfolderMediaDiscoveryEnabled(true)

        // Hidden items setting
        showHiddenNodesFlow.update { false }
        isShowHiddenNodesPopulated = false
    }

    override suspend fun getIsFirstLaunch() = withContext(ioDispatcher) {
        megaLocalStorageGateway.getFirstTime()
    }

    private suspend fun getShowHiddenNodesPreference() = withContext(ioDispatcher) {
        try {
            getCCPreferences().getValueFor(JSON_KEY_CONTENT_CONSUMPTION.value)
                ?.decodeBase64()
                ?.let { JSONObject(it) }
                ?.getJSONObject(JSON_KEY_ANDROID.value)
                ?.getJSONObject(JSON_SENSITIVES.value)
                ?.getBoolean(JSON_VAL_SHOW_HIDDEN_NODES.value)
                ?: false
        } catch (e: Throwable) {
            false
        }
    }

    private suspend fun setShowHiddenNodesPreference(enabled: Boolean) = withContext(ioDispatcher) {
        val prefs = getCCPreferences()
        val ccJson = try {
            prefs?.getValueFor(JSON_KEY_CONTENT_CONSUMPTION.value)
                ?.decodeBase64()
                ?.let { JSONObject(it) }
                ?: JSONObject()
        } catch (e: Exception) {
            JSONObject()
        }

        val androidJson = try {
            ccJson.getJSONObject(JSON_KEY_ANDROID.value) ?: JSONObject()
        } catch (e: Exception) {
            JSONObject()
        }
        val sensitivesJson = JSONObject(mapOf(JSON_VAL_SHOW_HIDDEN_NODES.value to enabled))

        androidJson.put(JSON_SENSITIVES.value, sensitivesJson)
        ccJson.put(JSON_KEY_ANDROID.value, androidJson)

        val newPrefs = prefs ?: MegaStringMap.createInstance()
        newPrefs[JSON_KEY_CONTENT_CONSUMPTION.value] = ccJson.toString().encodeBase64()

        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, _ -> continuation.resumeWith(Result.success(Unit)) },
            )

            megaApiGateway.setUserAttribute(
                type = MegaApiJava.USER_ATTR_CC_PREFS,
                value = newPrefs,
                listener = listener,
            )

        }
    }

    private suspend fun getCCPreferences() = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(request))
                        }

                        MegaError.API_ENOENT -> {
                            continuation.resumeWith(Result.success(null))
                        }

                        else -> {
                            continuation.failWithError(error, "getCCPreferences")
                        }
                    }
                }
            )

            megaApiGateway.getUserAttribute(
                attributeIdentifier = MegaApiJava.USER_ATTR_CC_PREFS,
                listener = listener,
            )

        }

        request?.megaStringMap
    }

    override suspend fun isRaiseToHandSuggestionShown() = withContext(ioDispatcher) {
        callsPreferencesGateway.getRaiseToHandSuggestionPreference()
    }

    override suspend fun setRaiseToHandSuggestionShown() = withContext(ioDispatcher) {
        callsPreferencesGateway.setRaiseToHandSuggestionPreference()
    }

    override suspend fun getFileVersionsOption(forceRefresh: Boolean) = withContext(ioDispatcher) {
        fileVersionsOptionCache.get()?.takeUnless { forceRefresh }
            ?: fetchFileVersionsOption().also {
                fileVersionsOptionCache.set(it)
            }
    }

    private suspend fun fetchFileVersionsOption(): Boolean = withContext(ioDispatcher) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        API_OK -> continuation.resumeWith(Result.success(request.flag))
                        API_ENOENT -> continuation.resumeWith(Result.success(false))
                        else -> continuation.failWithError(error, "fetchFileVersionsOption")
                    }
                }
            )
            megaApiGateway.getFileVersionsOption(listener)
        }
    }

    override suspend fun enableGeoTagging(enabled: Boolean) = withContext(ioDispatcher) {
        uiPreferencesGateway.enableGeoTagging(enabled)
    }

    override fun monitorGeoTaggingStatus(): Flow<Boolean?> =
        uiPreferencesGateway.monitorGeoTaggingStatus()
            .flowOn(ioDispatcher)

    override suspend fun setRubbishBinAutopurgePeriod(days: Int) = withContext(ioDispatcher) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setRubbishBinAutopurgePeriod") {}
            megaApiGateway.setRubbishBinAutopurgePeriod(days, listener)
        }
    }

    override suspend fun getRubbishBinAutopurgePeriod() = withContext(ioDispatcher) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        API_ENOENT -> if (myAccountInfoFacade.accountTypeId == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                            continuation.resumeWith(Result.success(DAYS_USER_FREE))
                        } else {
                            continuation.resumeWith(Result.success(DAYS_USER_PRO))
                        }

                        API_OK -> continuation.resumeWith(Result.success(request.number.toInt()))
                        else -> continuation.failWithError(error, "getRubbishBinAutopurgePeriod")
                    }
                }
            )
            megaApiGateway.getRubbishBinAutopurgePeriod(listener)
        }
    }

    override suspend fun isRubbishBinAutopurgeEnabled(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.serverSideRubbishBinAutopurgeEnabled()
    }

    override suspend fun setStartScreenPreferenceDestination(startScreenDestinationPreference: StartScreenDestinationPreference) {
        withContext(ioDispatcher) {
            uiPreferencesGateway.setSerialisedStartScreenPreferenceDestination(
                startScreenDestinationPreference.serialisedDestination
            )
        }
    }

    override fun monitorStartScreenPreferenceDestination() =
        uiPreferencesGateway.monitorSerialisedStartScreenPreferenceDestination()
            .map { it?.let { StartScreenDestinationPreference(it) } }
            .flowOn(ioDispatcher)

    override fun monitorColoredFoldersOnboardingShown(): Flow<Boolean> =
        appPreferencesGateway.monitorBoolean(COLORED_FOLDERS_ONBOARDING_SHOWN_KEY, false)
            .flowOn(ioDispatcher)

    override suspend fun setColoredFoldersOnboardingShown(shown: Boolean) {
        withContext(ioDispatcher) {
            appPreferencesGateway.putBoolean(COLORED_FOLDERS_ONBOARDING_SHOWN_KEY, shown)
        }
    }

    companion object {
        private const val COLORED_FOLDERS_ONBOARDING_SHOWN_KEY = "colored_folders_onboarding_shown"
        private const val DAYS_USER_FREE = 30
        private const val DAYS_USER_PRO = 90
    }
}
