package mega.privacy.android.data.repository.account

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.isType
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CredentialsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.EphemeralCredentialsGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.AccountDetailMapper
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.AchievementsOverviewMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.mapper.SubscriptionOptionListMapper
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.account.RecoveryKeyToFileMapper
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapper
import mega.privacy.android.data.mapper.contact.MyAccountCredentialsMapper
import mega.privacy.android.data.mapper.contact.UserMapper
import mega.privacy.android.data.mapper.login.AccountSessionMapper
import mega.privacy.android.data.mapper.login.UserCredentialsMapper
import mega.privacy.android.data.mapper.settings.CookieSettingsIntMapper
import mega.privacy.android.data.mapper.settings.CookieSettingsMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.qualifier.ExcludeFileName
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.ChangeEmailException
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NoLoggedInUserException
import mega.privacy.android.domain.exception.NotMasterBusinessAccountException
import mega.privacy.android.domain.exception.QRCodeException
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.exception.account.ConfirmCancelAccountException
import mega.privacy.android.domain.exception.account.ConfirmChangeEmailException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.exception.account.QueryCancelLinkException
import mega.privacy.android.domain.exception.account.QueryChangeEmailLinkException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [AccountRepository]
 *
 * @property context                      [Context]
 * @property myAccountInfoFacade          [AccountInfoWrapper]
 * @property megaApiGateway               [MegaApiGateway]
 * @property megaChatApiGateway           [MegaChatApiGateway]
 * @property megaApiFolderGateway         [MegaApiFolderGateway]
 * @property dbHandler                    [DatabaseHandler]
 * @property ioDispatcher                 [CoroutineDispatcher]
 * @property userUpdateMapper             [UserUpdateMapper]
 * @property localStorageGateway          [MegaLocalStorageGateway]
 * @property userAccountMapper            [UserAccountMapper]
 * @property accountTypeMapper            [AccountTypeMapper]
 * @property currencyMapper               [CurrencyMapper]
 * @property subscriptionOptionListMapper [SubscriptionOptionListMapper]
 * @property megaAchievementMapper        [MegaAchievementMapper]
 * @property myAccountCredentialsMapper   [MyAccountCredentialsMapper]
 * @property accountDetailMapper          [AccountDetailMapper]
 * @property userCredentialsMapper        [UserCredentialsMapper]
 * @property accountSessionMapper         [AccountSessionMapper]
 * @property chatPreferencesGateway       [chatPreferencesGateway]
 * @property callsPreferencesGateway      [CallsPreferencesGateway]
 * @property cacheGateway                 [CacheGateway]
 * @property appEventGateway              [AppEventGateway]
 * @property ephemeralCredentialsGateway  [EphemeralCredentialsGateway]
 * @property megaLocalRoomGateway         [MegaLocalRoomGateway]
 * @property fileGateway                  [FileGateway]
 * @property recoveryKeyToFileMapper      [RecoveryKeyToFileMapper]
 * @property cameraUploadsSettingsPreferenceGateway [CameraUploadsSettingsPreferenceGateway]
 * @property cookieSettingsMapper         [CookieSettingsMapper]
 * @property storageStateMapper           [StorageStateMapper]
 * @property uiPreferencesGateway         [UIPreferencesGateway]
 * @property userLoginPreferenceGateway   [UserLoginPreferenceGateway]
 */
@ExperimentalContracts
internal class DefaultAccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val myAccountInfoFacade: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val dbHandler: Lazy<DatabaseHandler>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userUpdateMapper: UserUpdateMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val userAccountMapper: UserAccountMapper,
    private val accountTypeMapper: AccountTypeMapper,
    private val currencyMapper: CurrencyMapper,
    private val subscriptionOptionListMapper: SubscriptionOptionListMapper,
    private val megaAchievementMapper: MegaAchievementMapper,
    private val achievementsOverviewMapper: AchievementsOverviewMapper,
    private val myAccountCredentialsMapper: MyAccountCredentialsMapper,
    private val accountDetailMapper: AccountDetailMapper,
    private val userCredentialsMapper: UserCredentialsMapper,
    private val accountSessionMapper: AccountSessionMapper,
    private val chatPreferencesGateway: ChatPreferencesGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val cacheGateway: CacheGateway,
    private val accountPreferencesGateway: AccountPreferencesGateway,
    private val passwordStrengthMapper: PasswordStrengthMapper,
    private val appEventGateway: AppEventGateway,
    private val ephemeralCredentialsGateway: Lazy<EphemeralCredentialsGateway>,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val fileGateway: FileGateway,
    private val recoveryKeyToFileMapper: RecoveryKeyToFileMapper,
    private val cameraUploadsSettingsPreferenceGateway: CameraUploadsSettingsPreferenceGateway,
    private val cookieSettingsMapper: CookieSettingsMapper,
    private val cookieSettingsIntMapper: CookieSettingsIntMapper,
    private val credentialsPreferencesGateway: Lazy<CredentialsPreferencesGateway>,
    private val userMapper: UserMapper,
    private val storageStateMapper: StorageStateMapper,
    private val uiPreferencesGateway: UIPreferencesGateway,
    @ExcludeFileName val excludeFileNames: Set<String>,
    private val getDomainNameUseCase: GetDomainNameUseCase,
) : AccountRepository {
    override suspend fun getUserAccount(): UserAccount = withContext(ioDispatcher) {
        val user = megaApiGateway.getLoggedInUser()
        val email = user?.email ?: megaChatApiGateway.getMyEmail() ?: ""
        userAccountMapper(
            userId = user?.let { UserId(it.handle) },
            email = email,
            fullName = megaChatApiGateway.getMyFullname(),
            isBusinessAccount = megaApiGateway.isBusinessAccount,
            isMasterBusinessAccount = megaApiGateway.isMasterBusinessAccount(),
            accountTypeIdentifier = accountTypeMapper(myAccountInfoFacade.accountTypeId),
            accountTypeString = myAccountInfoFacade.accountTypeString,
        )
    }

    override fun storageCapacityUsedIsBlank() =
        myAccountInfoFacade.storageCapacityUsedAsFormattedString.isBlank()

    override suspend fun requestAccount() {
        withContext(ioDispatcher) {
            val request = suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(request))
                        } else {
                            continuation.failWithError(error, "requestAccount")
                        }
                    },
                )
                megaApiGateway.getAccountDetails(listener)
            }
            // Legacy support, will remove completely once refactor to flow done
            myAccountInfoFacade.handleAccountDetail(request)
            handleAccountDetail(request)
        }
    }

    override suspend fun setUserHasLoggedIn() = withContext(ioDispatcher) {
        localStorageGateway.setUserHasLoggedIn()
    }

    override fun isMultiFactorAuthAvailable(): Boolean = megaApiGateway.multiFactorAuthAvailable()

    @Throws(MegaException::class)
    override suspend fun isMultiFactorAuthEnabled(): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = onMultiFactorAuthCheckRequestFinish(continuation)
            )
            megaApiGateway.multiFactorAuthEnabled(
                megaApiGateway.accountEmail,
                listener
            )
        }
    }

    private fun onMultiFactorAuthCheckRequestFinish(
        continuation: Continuation<Boolean>,
    ) = { request: MegaRequest, error: MegaError ->
        if (request.isType(MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK)) {
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else continuation.failWithError(error, "onMultiFactorAuthCheckRequestFinish")
        }
    }

    override suspend fun requestDeleteAccountLink() = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.cancelAccount(
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onDeleteAccountRequestFinished(continuation)
                )
            )
        }
    }

    private fun onDeleteAccountRequestFinished(continuation: Continuation<Unit>) =
        { request: MegaRequest, error: MegaError ->
            if (request.isType(MegaRequest.TYPE_GET_CANCEL_LINK)) {
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        continuation.resumeWith(Result.success(Unit))
                    }

                    MegaError.API_EACCESS -> continuation.failWithException(
                        NoLoggedInUserException(
                            error.errorCode,
                            error.errorString
                        )
                    )

                    MegaError.API_EMASTERONLY -> continuation.failWithException(
                        NotMasterBusinessAccountException(
                            error.errorCode,
                            error.errorString
                        )
                    )

                    else -> continuation.failWithError(error, "onDeleteAccountRequestFinished")
                }
            }
        }

    override fun monitorUserUpdates() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull { it.users }
        .map { userUpdateMapper(it) }

    override suspend fun getNumUnreadUserAlerts(): Int = withContext(ioDispatcher) {
        megaApiGateway.getNumUnreadUserAlerts()
    }

    override suspend fun getSession(): String? = withContext(ioDispatcher) {
        getAccountCredentials()?.session
    }

    override suspend fun retryChatPendingConnections(disconnect: Boolean) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        when (error.errorCode) {
                            MegaChatError.ERROR_OK -> continuation.resumeWith(
                                Result.success(Unit)
                            )

                            MegaChatError.ERROR_ACCESS -> continuation.resumeWith(
                                Result.failure(ChatNotInitializedErrorStatus())
                            )

                            else -> continuation.failWithError(
                                error,
                                "retryChatPendingConnections"
                            )
                        }
                    }
                )
                megaChatApiGateway.retryPendingConnections(
                    disconnect = disconnect,
                    listener = listener
                )
            }
        }


    override suspend fun getSubscriptionOptions(): List<SubscriptionOption> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(
                                Result.success(
                                    subscriptionOptionListMapper(request)
                                )
                            )
                        } else {
                            continuation.failWithError(error, "getSubscriptionOptions")
                        }
                    }
                )
                megaApiGateway.getPricing(listener)
            }
        }

    override suspend fun areAccountAchievementsEnabled(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.areAccountAchievementsEnabled()
    }

    override suspend fun getAccountAchievements(
        achievementType: AchievementType,
        awardIndex: Long,
    ): MegaAchievement =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getAccountAchievements(
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(
                                    Result.success(
                                        megaAchievementMapper(
                                            request.megaAchievementsDetails,
                                            achievementType,
                                            awardIndex
                                        )
                                    )
                                )
                            } else {
                                continuation.failWithError(error, "getAccountAchievements")
                            }
                        })
                )
            }
        }

    override suspend fun getAccountDetailsTimeStampInSeconds(): String? =
        withContext(ioDispatcher) {
            dbHandler.get().attributes?.accountDetailsTimeStamp
        }

    override suspend fun getExtendedAccountDetailsTimeStampInSeconds(): String? =
        withContext(ioDispatcher) {
            dbHandler.get().attributes?.extendedAccountDetailsTimeStamp
        }

    override suspend fun getSpecificAccountDetail(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
    ) = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error, "getSpecificAccountDetail")
                    }
                },
            )
            megaApiGateway.getSpecificAccountDetails(storage, transfer, pro, listener)
        }
        myAccountInfoFacade.handleAccountDetail(request)
        handleAccountDetail(request)
    }

    override suspend fun getExtendedAccountDetails(
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
    ) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error, "getExtendedAccountDetails")
                    }
                },
            )
            megaApiGateway.getExtendedAccountDetails(sessions, purchases, transactions, listener)
        }
        myAccountInfoFacade.handleAccountDetail(request)
        handleAccountDetail(request)
    }

    override suspend fun getMyCredentials() = withContext(ioDispatcher) {
        myAccountCredentialsMapper(megaApiGateway.myCredentials)
    }

    override suspend fun resetAccountDetailsTimeStamp() = withContext(ioDispatcher) {
        dbHandler.get().resetAccountDetailsTimeStamp()
    }

    override suspend fun resetExtendedAccountDetailsTimestamp() = withContext(ioDispatcher) {
        dbHandler.get().resetExtendedAccountDetailsTimestamp()
    }

    override suspend fun createContactLink(renew: Boolean): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val value = megaApiGateway.handleToBase64(request.nodeHandle)
                        continuation.resumeWith(Result.success("https://${getDomainNameUseCase()}/C!$value"))
                    } else {
                        if (renew) {
                            continuation.resumeWith(
                                Result.failure(
                                    QRCodeException.ResetFailed(
                                        error.errorCode,
                                        error.errorString
                                    )
                                )
                            )
                        } else {
                            continuation.resumeWith(
                                Result.failure(
                                    QRCodeException.CreateFailed(
                                        error.errorCode,
                                        error.errorString
                                    )
                                )
                            )
                        }
                    }
                }
            )
            megaApiGateway.contactLinkCreate(renew, listener)
        }
    }

    override suspend fun deleteContactLink(handle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(
                            Result.failure(
                                QRCodeException.DeleteFailed(
                                    error.errorCode,
                                    error.errorString
                                )
                            )
                        )
                    }

                }
            )
            megaApiGateway.contactLinkDelete(handle, listener)
        }
    }

    override suspend fun getAccountAchievementsOverview(): AchievementsOverview =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(
                                Result.success(
                                    achievementsOverviewMapper(request.megaAchievementsDetails)
                                )
                            )
                        } else {
                            continuation.failWithError(error, "getAccountAchievementsOverview")
                        }
                    })
                megaApiGateway.getAccountAchievements(listener)
            }
        }

    override suspend fun getAccountEmail(forceRefresh: Boolean): String? = withTimeout(5000L) {
        withContext(ioDispatcher) {
            if (forceRefresh) {
                return@withContext megaApiGateway.accountEmail
                    .also {
                        if (!it.isNullOrBlank()) {
                            credentialsPreferencesGateway.get().saveEmail(it)
                        }
                    }
            }
            return@withContext getAccountCredentials()?.email
        }
    }

    private suspend fun handleAccountDetail(request: MegaRequest): AccountDetail {
        val newDetail = accountDetailMapper(
            request.megaAccountDetails,
            request.numDetails,
            megaApiGateway.getRootNode(),
            megaApiGateway.getRubbishBinNode(),
            megaApiGateway.getIncomingSharesNode(null),
        )
        // keep previous info if new info null
        myAccountInfoFacade.handleAccountDetail(newDetail)

        // Send broadcast to to App Event
        appEventGateway.broadcastMyAccountUpdate(
            MyAccountUpdate(
                action = Action.UPDATE_ACCOUNT_DETAILS,
                storageState = null
            )
        )
        return newDetail
    }

    override fun monitorAccountDetail(): Flow<AccountDetail> =
        myAccountInfoFacade.monitorAccountDetail()

    override suspend fun isMegaApiLoggedIn(): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.isMegaApiLoggedIn() > 0
        }

    override suspend fun isEphemeralPlusPlus(): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.isEphemeralPlusPlus
        }

    override suspend fun saveAccountCredentials() = withContext(ioDispatcher) {
        var myUserHandle: Long? = null
        var email: String? = null
        megaApiGateway.myUser?.let { myUser ->
            email = myUser.email
            myUserHandle = myUser.handle
        }

        val session = megaApiGateway.dumpSession
        val credentials = userCredentialsMapper(email, session, null, null, myUserHandle.toString())
        credentialsPreferencesGateway.get().save(credentials)
        ephemeralCredentialsGateway.get().clear()

        accountSessionMapper(email, session, myUserHandle)
    }

    override suspend fun getAccountCredentials() = withContext(ioDispatcher) {
        credentialsPreferencesGateway.get().monitorCredentials().firstOrNull()
    }

    override suspend fun changeEmail(email: String): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> continuation.resumeWith(Result.success(request.email))
                        MegaError.API_EACCESS -> continuation.resumeWith(
                            Result.failure(ChangeEmailException.EmailInUse)
                        )

                        MegaError.API_EEXIST -> continuation.resumeWith(
                            Result.failure(ChangeEmailException.AlreadyRequested)
                        )

                        else -> continuation.resumeWith(
                            Result.failure(ChangeEmailException.Unknown(error.errorCode))
                        )
                    }
                }
            )

            megaApiGateway.changeEmail(email, listener)
        }
    }

    override suspend fun querySignupLink(signupLink: String): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            Timber.d("MegaRequest.TYPE_QUERY_SIGNUP_LINK MegaError API_OK")
                            continuation.resumeWith(Result.success(request.email))
                        }

                        MegaError.API_ENOENT -> {
                            Timber.w("MegaRequest.TYPE_QUERY_SIGNUP_LINK link no longer available.")
                            continuation.resumeWith(
                                Result.failure(QuerySignupLinkException.LinkNoLongerAvailable)
                            )
                        }

                        else -> {
                            Timber.w("MegaRequest.TYPE_QUERY_SIGNUP_LINK error $error")
                            continuation.resumeWith(
                                Result.failure(QuerySignupLinkException.Unknown(error.toException("querySignupLink")))
                            )
                        }
                    }
                }
            )

            megaApiGateway.querySignupLink(signupLink, listener)

        }
    }

    override suspend fun resetAccountAuth() = withContext(ioDispatcher) {
        megaApiFolderGateway.setAccountAuth(null)
    }

    override suspend fun clearAccountPreferences() = withContext(ioDispatcher) {
        with(localStorageGateway) {
            clearPreferences()
            setFirstTime(false)
            clearContacts()
            clearNonContacts()
            clearChatItems()
            clearAttributes()
            clearChatSettings()
        }
        credentialsPreferencesGateway.get().clear()
        megaLocalRoomGateway.deleteAllBackups()
        megaLocalRoomGateway.deleteAllCompletedTransfers()
        megaLocalRoomGateway.clearOffline()
        callsPreferencesGateway.clearPreferences()
        chatPreferencesGateway.clearPreferences()
        accountPreferencesGateway.clearPreferences()
        cameraUploadsSettingsPreferenceGateway.clearPreferences()
    }

    override suspend fun clearSharedPreferences() = withContext(ioDispatcher) {
        with(context) {
            // Remove time stamp preference
            getSharedPreferences(LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE).edit { clear() }

            //Remove UI preferences
            getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE).edit { clear() }

            //Remove sms dialog time checker preference
            getSharedPreferences(LAST_SHOW_SMS_FILE, Context.MODE_PRIVATE).edit { clear() }

            //Remove Text editor, Offline warning, mobile data resolution and Audio player preferences
            PreferenceManager.getDefaultSharedPreferences(this).edit {
                remove(SHOW_LINE_NUMBERS)
                remove(SHOW_OFFLINE_WARNING)
                remove(KEY_MOBILE_DATA_HIGH_RESOLUTION)
                remove(KEY_AUDIO_BACKGROUND_PLAY_ENABLED)
                remove(KEY_AUDIO_SHUFFLE_ENABLED)
                remove(KEY_AUDIO_REPEAT_MODE)
            }
        }
    }

    override suspend fun clearAppDataAndCache() = withContext(ioDispatcher) {
        with(cacheGateway) {
            clearCacheDirectory()
            clearAppData(excludeFileNames)
            clearSdkCache()
        }
    }

    override suspend fun cancelAllNotifications() = withContext(ioDispatcher) {
        try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancelAll()
        } catch (e: Exception) {
            Timber.e(e, "EXCEPTION removing all the notifications")
        }
    }

    override suspend fun isCurrentPassword(password: String) = withContext(ioDispatcher) {
        megaApiGateway.isCurrentPassword(password)
    }

    override suspend fun changePassword(newPassword: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("changePassword") { it.newPassword == newPassword }
            megaApiGateway.changePassword(newPassword, listener)
        }
    }

    override suspend fun resetPasswordFromLink(
        link: String?,
        newPassword: String,
        masterKey: String?,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("resetPasswordFromLink") { it.email.isNotBlank() }
            megaApiGateway.resetPasswordFromLink(link, newPassword, masterKey, listener)
        }
    }

    override suspend fun getPasswordStrength(password: String) = withContext(ioDispatcher) {
        passwordStrengthMapper(megaApiGateway.getPasswordStrength(password))
    }

    override suspend fun queryResetPasswordLink(link: String): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                Timber.d("MegaRequest.TYPE_QUERY_RECOVERY_LINK MegaError API_OK")
                                continuation.resumeWith(Result.success(request.email.orEmpty()))
                            }

                            MegaError.API_EEXPIRED -> {
                                Timber.w("MegaRequest.TYPE_QUERY_RECOVERY_LINK link no longer available.")
                                continuation.resumeWith(
                                    Result.failure(ResetPasswordLinkException.LinkExpired)
                                )
                            }

                            MegaError.API_EACCESS -> {
                                Timber.w("MegaRequest.TYPE_QUERY_RECOVERY_LINK unrelated link $error")
                                continuation.resumeWith(
                                    Result.failure(ResetPasswordLinkException.LinkAccessDenied)
                                )
                            }

                            else -> {
                                Timber.w("MegaRequest.TYPE_QUERY_RECOVERY_LINK error $error")
                                continuation.resumeWith(
                                    Result.failure(
                                        ResetPasswordLinkException.LinkInvalid
                                    )
                                )
                            }
                        }
                    }
                )

                megaApiGateway.queryResetPasswordLink(link, listener)
            }
        }

    override suspend fun resetAccountInfo() = myAccountInfoFacade.resetAccountInfo()
    override suspend fun update2FADialogPreference(show2FA: Boolean) =
        withContext(ioDispatcher) { accountPreferencesGateway.setDisplay2FADialog(show2FA) }

    override suspend fun get2FADialogPreference() =
        withContext(ioDispatcher) { accountPreferencesGateway.monitorShow2FADialog().first() }

    override suspend fun is2FAEnabled() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("is2FAEnabled") { it.flag }
            megaApiGateway.multiFactorAuthEnabled(megaApiGateway.accountEmail, listener)
        }
    }

    override suspend fun setLatestTargetPathCopyPreference(path: Long) = withContext(ioDispatcher) {
        accountPreferencesGateway.setLatestTargetPathCopyPreference(path)
        accountPreferencesGateway.setLatestTargetTimestampCopyPreference(System.currentTimeMillis())
    }

    override suspend fun getLatestTargetPathCopyPreference(): Long? {
        val timestamp =
            accountPreferencesGateway.getLatestTargetTimestampCopyPreference().firstOrNull()
        return timestamp?.let {
            if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - it) > LATEST_TARGET_PATH_VALID_DURATION)
                null
            else
                accountPreferencesGateway.getLatestTargetPathCopyPreference().firstOrNull()
        }
    }

    override suspend fun setLatestTargetPathMovePreference(path: Long) {
        accountPreferencesGateway.setLatestTargetPathMovePreference(path)
        accountPreferencesGateway.setLatestTargetTimestampMovePreference(System.currentTimeMillis())
    }

    override suspend fun getLatestTargetPathMovePreference(): Long? {
        val timestamp =
            accountPreferencesGateway.getLatestTargetTimestampMovePreference().firstOrNull()
        return timestamp?.let {
            if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - it) > LATEST_TARGET_PATH_VALID_DURATION)
                null
            else
                accountPreferencesGateway.getLatestTargetPathMovePreference().firstOrNull()
        }
    }

    override suspend fun skipPasswordReminderDialog() =
        onPasswordReminderAction("skipPasswordReminderDialog") { listener ->
            megaApiGateway.skipPasswordReminderDialog(listener)
        }

    override suspend fun blockPasswordReminderDialog() =
        onPasswordReminderAction("blockPasswordReminderDialog") { listener ->
            megaApiGateway.blockPasswordReminderDialog(listener)
        }

    override suspend fun notifyPasswordChecked() =
        onPasswordReminderAction("successPasswordReminderDialog") { listener ->
            megaApiGateway.successPasswordReminderDialog(listener)
        }

    private suspend fun onPasswordReminderAction(
        methodName: String,
        block: (MegaRequestListenerInterface) -> Unit,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (request.paramType == MegaApiJava.USER_ATTR_PWD_REMINDER &&
                        (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT)
                    ) {
                        continuation.resumeWith(Result.success(Unit))
                        Timber.d("$methodName: New value of attribute USER_ATTR_PWD_REMINDER: ${request.text}")
                    } else {
                        continuation.failWithError(error, "onPasswordReminderAction")
                        Timber.e("$methodName: MegaRequest.TYPE_SET_ATTR_USER | MegaApiJava.USER_ATTR_PWD_REMINDER ${error.errorString}")
                    }
                },
            )
            block(listener)
        }
    }


    override suspend fun upgradeSecurity() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("upgradeSecurity") { return@getRequestListener }
            megaApiGateway.upgradeSecurity(listener)
        }
    }

    override fun monitorSecurityUpgrade(): Flow<Boolean> =
        appEventGateway.monitorSecurityUpgrade()

    override suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean) =
        appEventGateway.setUpgradeSecurity(isSecurityUpgrade)

    override fun monitorMyAccountUpdate() = appEventGateway.monitorMyAccountUpdate()

    override suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate) =
        appEventGateway.broadcastMyAccountUpdate(data)

    override fun monitorEphemeralCredentials() =
        ephemeralCredentialsGateway.get().monitorEphemeralCredentials()

    override suspend fun saveEphemeral(ephemeral: EphemeralCredentials) =
        ephemeralCredentialsGateway.get().save(ephemeral)

    override suspend fun clearEphemeral() = ephemeralCredentialsGateway.get().clear()

    override fun monitorRefreshSession() = appEventGateway.monitorRefreshSession()

    override suspend fun broadcastRefreshSession() = appEventGateway.broadcastRefreshSession()

    override fun getAccountType(): AccountType =
        accountTypeMapper(myAccountInfoFacade.accountTypeId)

    override suspend fun retryPendingConnections() = withContext(ioDispatcher) {
        Timber.d("Retrying pending connections...")
        megaApiGateway.retryPendingConnections()
    }

    override suspend fun getLoggedInUserId() = withContext(ioDispatcher) {
        megaApiGateway.myUser?.handle?.let { UserId(it) }
    }

    override suspend fun getUserAlias(handle: Long): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("getUserAlias") { request -> request.name }
            megaApiGateway.getUserAlias(userHandle = handle, listener = listener)
        }
    }

    override suspend fun getUserAliasFromCache(userHandle: Long): String? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getUserAliasFromCache(userHandle)
        }

    override suspend fun isAchievementsEnabled() = withContext(ioDispatcher) {
        megaApiGateway.isAchievementsEnabled
    }

    override suspend fun renameRecoveryKeyFile(relativePath: String, newName: String) =
        withContext(ioDispatcher) {
            val oldFile = fileGateway.buildExternalStorageFile(relativePath)
            fileGateway.renameFile(oldFile, newName)
        }

    override suspend fun getRecoveryKeyFile(): File? = withContext(ioDispatcher) {
        megaApiGateway.getExportMasterKey()?.let(recoveryKeyToFileMapper::invoke)
    }

    /**
     * Get a boolean value that represent whether the user account is new or not
     *
     * @return if the account is new or not
     */
    override suspend fun isAccountNew() = withContext(ioDispatcher) {
        megaApiGateway.isAccountNew()
    }

    override suspend fun isCookieBannerEnabled() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resume(megaApiGateway.isCookieBannerEnabled())
                        }

                        else -> {
                            continuation.failWithError(
                                error,
                                "failed to get cookie banner state: ${error.errorString}"
                            )
                        }
                    }
                }
            )
            megaApiGateway.getUserData(listener)
        }
    }

    override suspend fun getMiscFlags() = withContext(ioDispatcher) {
        megaApiGateway.getMiscFlags()
    }

    override suspend fun getCookieSettings() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resume(
                                cookieSettingsMapper(request.numDetails)
                            )
                        }

                        MegaError.API_ENOENT -> {
                            // Cookie Settings has not been set before
                            continuation.resume(emptySet<CookieType>())
                        }

                        else -> {
                            continuation.failWithError(error, "failed to get cookie settings")
                        }
                    }
                }
            )
            megaApiGateway.getCookieSettings(listener)
        }
    }

    override suspend fun setCookieSettings(enabledCookieSettings: Set<CookieType>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val cookieDecimal = cookieSettingsIntMapper(enabledCookieSettings)
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resume(Unit)
                            }

                            else -> {
                                continuation.failWithError(error, "failed to set cookie settings")
                            }
                        }
                    }
                )
                megaApiGateway.setCookieSettings(cookieDecimal, listener)
            }
        }

    override suspend fun broadcastCookieSettings(enabledCookieSettings: Set<CookieType>) {
        appEventGateway.broadcastCookieSettings(enabledCookieSettings)
    }

    override fun monitorCookieSettingsSaved(): Flow<Set<CookieType>> =
        appEventGateway.monitorCookieSettings

    override suspend fun shouldShowCopyright(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.shouldShowCopyright()
    }

    override suspend fun killOtherSessions() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.failWithError(error, "killOtherSessions")
                    }
                }
            )
            // INVALID_HANDLE is used to kill all other active Sessions except the current Session
            megaApiGateway.killSession(megaApiGateway.getInvalidHandle(), listener)
        }
    }

    override suspend fun confirmCancelAccount(cancellationLink: String, accountPassword: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(Unit))
                            }

                            MegaError.API_ENOENT -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        ConfirmCancelAccountException.IncorrectPassword(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }

                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        ConfirmCancelAccountException.Unknown(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }
                        }
                    }
                )
                megaApiGateway.confirmCancelAccount(cancellationLink, accountPassword, listener)
            }
        }

    override suspend fun confirmChangeEmail(
        changeEmailLink: String,
        accountPassword: String,
    ): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(request.email))
                        }

                        MegaError.API_EEXIST -> {
                            continuation.resumeWith(
                                Result.failure(
                                    ConfirmChangeEmailException.EmailAlreadyInUse(
                                        error.errorCode,
                                        error.errorString,
                                    )
                                )
                            )
                        }

                        MegaError.API_ENOENT -> {
                            continuation.resumeWith(
                                Result.failure(
                                    ConfirmChangeEmailException.IncorrectPassword(
                                        error.errorCode,
                                        error.errorString,
                                    )
                                )
                            )
                        }

                        else -> {
                            continuation.resumeWith(
                                Result.failure(
                                    ConfirmChangeEmailException.Unknown(
                                        error.errorCode,
                                        error.errorString,
                                    )
                                )
                            )
                        }
                    }
                }
            )
            megaApiGateway.confirmChangeEmail(changeEmailLink, accountPassword, listener)
        }
    }

    override suspend fun getUserData() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resume(value = Unit)
                        }

                        else -> {
                            continuation.failWithError(
                                error = error,
                                methodName = "getUserData"
                            )
                        }
                    }
                }
            )
            megaApiGateway.getUserData(listener = listener)
        }
    }

    override suspend fun broadcastUpdateUserData() {
        appEventGateway.broadcastUpdateUserData()
    }

    override fun monitorUpdateUserData(): Flow<Unit> {
        return appEventGateway.monitorUpdateUserData()
    }

    override suspend fun getLastRegisteredEmail() = withContext(ioDispatcher) {
        accountPreferencesGateway.monitorLastRegisteredEmail().firstOrNull()
    }

    override suspend fun saveLastRegisteredEmail(email: String) = withContext(ioDispatcher) {
        accountPreferencesGateway.setLastRegisteredEmail(email)
    }

    override suspend fun clearLastRegisteredEmail() = withContext(ioDispatcher) {
        accountPreferencesGateway.clearLastRegisteredEmail()
    }

    override suspend fun queryCancelLink(accountCancellationLink: String): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(request.link))
                            }

                            MegaError.API_EACCESS -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        QueryCancelLinkException.UnrelatedAccountCancellationLink(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }

                            MegaError.API_EEXPIRED -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        QueryCancelLinkException.ExpiredAccountCancellationLink(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }

                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        QueryCancelLinkException.Unknown(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }
                        }
                    }
                )
                megaApiGateway.queryCancelLink(accountCancellationLink, listener)
            }
        }

    override suspend fun queryChangeEmailLink(changeEmailLink: String): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(request.link))
                            }

                            MegaError.API_EACCESS -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        QueryChangeEmailLinkException.LinkNotGenerated(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }

                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        QueryChangeEmailLinkException.Unknown(
                                            error.errorCode,
                                            error.errorString,
                                        )
                                    )
                                )
                            }
                        }
                    }
                )
                megaApiGateway.queryChangeEmailLink(changeEmailLink, listener)
            }
        }

    override suspend fun cancelCreateAccount(): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("cancelCreateAccount") {
                it.email
            }
            megaApiGateway.cancelCreateAccount(listener)
        }
    }

    override suspend fun getUsedStorage(): Long = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error, "requestAccount")
                    }
                },
            )
            megaApiGateway.getAccountDetails(listener)
        }
        val accountDetails = request.megaAccountDetails
        return@withContext accountDetails.storageUsed
    }

    override suspend fun getMaxStorage(): Long = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error, "requestAccount")
                    }
                },
            )
            megaApiGateway.getAccountDetails(listener)
        }
        val accountDetails = request.megaAccountDetails
        return@withContext accountDetails.storageMax
    }

    override suspend fun setCredentials(credentials: UserCredentials) = withContext(ioDispatcher) {
        credentialsPreferencesGateway.get().save(credentials)
    }

    override fun monitorCredentials(): Flow<UserCredentials?> =
        credentialsPreferencesGateway.get().monitorCredentials()
            .flowOn(ioDispatcher)

    override suspend fun clearCredentials() = withContext(ioDispatcher) {
        credentialsPreferencesGateway.get().clear()
    }

    override suspend fun getCurrentUser(): User? = withContext(ioDispatcher) {
        megaApiGateway.myUser?.let { userMapper(it) }
    }

    override suspend fun getStorageState(): StorageState =
        withContext(ioDispatcher) {
            val storageStateUserAttribute = MegaApiJava.USER_ATTR_STORAGE_STATE
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(
                                Result.success(
                                    storageStateMapper(request.number.toInt())
                                )
                            )
                        } else {
                            continuation.failWithError(error, "getStorageState")
                        }
                    }
                )
                megaApiGateway.getUserAttribute(storageStateUserAttribute, listener)
            }
        }

    override suspend fun setAlmostFullStorageBannerClosingTimestamp(timestamp: Long) {
        withContext(ioDispatcher) {
            uiPreferencesGateway.setAlmostFullStorageBannerClosingTimestamp(
                timestamp
            )
        }
    }

    override fun monitorAlmostFullStorageBannerClosingTimestamp(): Flow<Long?> =
        uiPreferencesGateway.monitorAlmostFullStorageBannerClosingTimestamp().flowOn(ioDispatcher)

    override suspend fun createAccount(
        email: String, password: String, firstName: String, lastName: String,
    ): EphemeralCredentials = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(
                                Result.success(
                                    EphemeralCredentials(
                                        request.email,
                                        request.password,
                                        request.sessionKey,
                                        request.name,
                                        request.text
                                    )
                                )
                            )
                        }

                        MegaError.API_EEXIST -> {
                            continuation.resumeWith(
                                Result.failure(CreateAccountException.AccountAlreadyExists)
                            )
                        }

                        else -> {
                            continuation.resumeWith(
                                Result.failure(
                                    CreateAccountException.Unknown(error.toException("createAccount"))
                                )
                            )
                        }
                    }
                }
            )
            megaApiGateway.createAccount(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                listener = listener
            )
        }
    }

    override fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override fun getInvalidAffiliateType(): Int = megaApiGateway.getInvalidAffiliateType()

    override fun monitorMiscLoaded() = appEventGateway.monitorMiscLoaded()

    override suspend fun broadcastMiscLoaded() = appEventGateway.broadcastMiscLoaded()

    override suspend fun broadcastMiscUnLoaded() = appEventGateway.broadcastMiscUnloaded()

    override suspend fun resendVerificationEmail() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(
                            Result.success(Unit)
                        )
                    } else {
                        continuation.failWithError(error, "resendVerificationEmail")
                    }
                }
            )

            megaApiGateway.resendVerificationEmail(listener)
        }
    }

    override suspend fun resumeCreateAccount(session: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("resumeCreateAccount") {}
            megaApiGateway.resumeCreateAccount(session, listener)
        }
    }

    override suspend fun checkRecoveryKey(link: String, recoveryKey: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("checkRecoveryKey") { }
                megaApiGateway.checkRecoveryKey(link, recoveryKey, listener)
            }
        }

    override suspend fun setLoggedOutFromAnotherLocation(isLoggedOut: Boolean) {
        appEventGateway.setLoggedOutFromAnotherLocation(isLoggedOut)
    }

    override fun monitorLoggedOutFromAnotherLocation(): Flow<Boolean> {
        return appEventGateway.monitorLoggedOutFromAnotherLocation()
    }

    override suspend fun setIsUnverifiedBusinessAccount(isUnverified: Boolean) {
        appEventGateway.setIsUnverifiedBusinessAccount(isUnverified)
    }

    override fun monitorIsUnverifiedBusinessAccount(): Flow<Boolean> {
        return appEventGateway.monitorIsUnverifiedBusinessAccount()
    }

    companion object {
        private const val LAST_SYNC_TIMESTAMP_FILE = "last_sync_timestamp"
        private const val USER_INTERFACE_PREFERENCES = "USER_INTERFACE_PREFERENCES"
        private const val SHOW_LINE_NUMBERS = "SHOW_LINE_NUMBERS"
        private const val SHOW_OFFLINE_WARNING = "SHOW_OFFLINE_WARNING"
        private const val KEY_MOBILE_DATA_HIGH_RESOLUTION = "setting_mobile_data_high_resolution"
        private const val LAST_SHOW_SMS_FILE = "last_show_sms_timestamp_sp"
        private const val KEY_AUDIO_BACKGROUND_PLAY_ENABLED =
            "settings_audio_background_play_enabled"
        private const val KEY_AUDIO_SHUFFLE_ENABLED = "settings_audio_shuffle_enabled"
        private const val KEY_AUDIO_REPEAT_MODE = "settings_audio_repeat_mode"
        private const val LATEST_TARGET_PATH_VALID_DURATION = 60
    }
}
