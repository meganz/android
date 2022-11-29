package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.isType
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.SubscriptionOptionListMapper
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NoLoggedInUserException
import mega.privacy.android.domain.exception.NotMasterBusinessAccountException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [AccountRepository]
 *
 * @property myAccountInfoFacade
 * @property megaApiGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 * @property userUpdateMapper
 * @property localStorageGateway
 * @property userAccountMapper
 * @property accountTypeMapper
 * @property currencyMapper
 * @property subscriptionOptionListMapper
 */
@ExperimentalContracts
internal class DefaultAccountRepository @Inject constructor(
    private val myAccountInfoFacade: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val dbHandler: DatabaseHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userUpdateMapper: UserUpdateMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val userAccountMapper: UserAccountMapper,
    private val accountTypeMapper: AccountTypeMapper,
    private val currencyMapper: CurrencyMapper,
    private val subscriptionOptionListMapper: SubscriptionOptionListMapper,
    private val megaAchievementMapper: MegaAchievementMapper,
) : AccountRepository {
    override suspend fun getUserAccount(): UserAccount = withContext(ioDispatcher) {
        val user = megaApiGateway.getLoggedInUser()
        userAccountMapper(
            user?.let { UserId(it.handle) },
            user?.email ?: "",
            megaApiGateway.isBusinessAccount,
            megaApiGateway.isMasterBusinessAccount,
            accountTypeMapper(myAccountInfoFacade.accountTypeId),
            myAccountInfoFacade.accountTypeString,
        )
    }

    override fun storageCapacityUsedIsBlank() =
        myAccountInfoFacade.storageCapacityUsedAsFormattedString.isBlank()

    override suspend fun requestAccount() = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request))
                    } else {
                        continuation.failWithError(error)
                    }
                },
            )
            megaApiGateway.getAccountDetails(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
        myAccountInfoFacade.handleAccountDetail(request)
    }

    override suspend fun setUserHasLoggedIn() {
        localStorageGateway.setUserHasLoggedIn()
    }

    override fun isMultiFactorAuthAvailable() = megaApiGateway.multiFactorAuthAvailable()

    @Throws(MegaException::class)
    override suspend fun isMultiFactorAuthEnabled(): Boolean = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.multiFactorAuthEnabled(
                megaApiGateway.accountEmail,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onMultiFactorAuthCheckRequestFinish(continuation)
                )
            )
        }
    }

    private fun onMultiFactorAuthCheckRequestFinish(
        continuation: Continuation<Boolean>,
    ) = { request: MegaRequest, error: MegaError ->
        if (request.isType(MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK)) {
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else continuation.failWithError(error)
        }
    }

    override suspend fun requestDeleteAccountLink() = withContext<Unit>(ioDispatcher) {
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
                    else -> continuation.failWithError(error)
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

    override suspend fun getSession(): String? =
        localStorageGateway.getUserCredentials()?.session

    override suspend fun retryPendingConnections(disconnect: Boolean) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.retryPendingConnections()
                megaChatApiGateway.retryPendingConnections(
                    disconnect = disconnect,
                    listener = OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (request.type == MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS) {
                                when (error.errorCode) {
                                    MegaChatError.ERROR_OK -> continuation.resumeWith(Result.success(
                                        Unit))
                                    MegaChatError.ERROR_ACCESS -> continuation.resumeWith(Result.failure(
                                        ChatNotInitializedException()))
                                    else -> continuation.failWithError(error)
                                }
                            }
                        }
                    )
                )
            }
        }

    override suspend fun isBusinessAccountActive(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isBusinessAccountActive()
    }

    override suspend fun getSubscriptionOptions(): List<SubscriptionOption> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getPricing(OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(subscriptionOptionListMapper(
                                request,
                                currencyMapper,
                            )))
                        } else {
                            continuation.failWithError(error)
                        }
                    }
                ))
            }
        }

    override suspend fun isAccountAchievementsEnabled(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isAccountAchievementsEnabled()
    }

    override suspend fun getAccountAchievements(
        achievementType: AchievementType,
        awardIndex: Long,
    ): MegaAchievement =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getAccountAchievements(OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(megaAchievementMapper(
                                request.megaAchievementsDetails,
                                achievementType,
                                awardIndex
                            )))
                        } else {
                            continuation.failWithError(error)
                        }
                    }))
            }
        }

    override suspend fun getAccountDetailsTimeStampInSeconds(): String? =
        withContext(ioDispatcher) {
            dbHandler.attributes?.accountDetailsTimeStamp
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
                        continuation.failWithError(error)
                    }
                },
            )
            megaApiGateway.getSpecificAccountDetails(storage, transfer, pro, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
        myAccountInfoFacade.handleAccountDetail(request)
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
                        continuation.failWithError(error)
                    }
                },
            )
            megaApiGateway.getExtendedAccountDetails(sessions, purchases, transactions, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
        myAccountInfoFacade.handleAccountDetail(request)
    }

    override suspend fun getMyCredentials() = withContext(ioDispatcher) {
        megaApiGateway.myCredentials
    }

    override suspend fun resetAccountDetailsTimeStamp() = withContext(ioDispatcher) {
        dbHandler.resetAccountDetailsTimeStamp()
    }

    override suspend fun resetExtendedAccountDetailsTimestamp() = withContext(ioDispatcher) {
        dbHandler.resetExtendedAccountDetailsTimestamp()
    }

    override suspend fun areAchievementsEnabled() = withContext(ioDispatcher) {
        megaApiGateway.isAchievementsEnabled()
    }
}
