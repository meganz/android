package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.failWithException
import mega.privacy.android.app.data.extensions.isType
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.SkuMapper
import mega.privacy.android.data.mapper.SubscriptionPlanListMapper
import mega.privacy.android.data.mapper.SubscriptionPlanMapper
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.SubscriptionPlan
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NoLoggedInUserException
import mega.privacy.android.domain.exception.NotMasterBusinessAccountException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
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
 * @property monitorMultiFactorAuth
 * @property ioDispatcher
 * @property userUpdateMapper
 * @property localStorageGateway
 * @property userAccountMapper
 * @property accountTypeMapper
 * @property subscriptionPlanMapper
 * @property currencyMapper
 * @property skuMapper
 * @property subscriptionPlanListMapper
 */
@ExperimentalContracts
class DefaultAccountRepository @Inject constructor(
    private val myAccountInfoFacade: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val monitorMultiFactorAuth: MonitorMultiFactorAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userUpdateMapper: UserUpdateMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val userAccountMapper: UserAccountMapper,
    private val accountTypeMapper: AccountTypeMapper,
    private val subscriptionPlanMapper: SubscriptionPlanMapper,
    private val currencyMapper: CurrencyMapper,
    private val skuMapper: SkuMapper,
    private val subscriptionPlanListMapper: SubscriptionPlanListMapper,
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

    override fun isAccountDataStale(): Boolean =
        databaseEntryIsStale() || storageCapacityUsedIsBlank()

    private fun databaseEntryIsStale() = DBUtil.callToAccountDetails().also {
        Timber.d("Check the last call to getAccountDetails")
    }

    private fun storageCapacityUsedIsBlank() =
        myAccountInfoFacade.storageCapacityUsedAsFormattedString.isBlank()

    override fun requestAccount() = myAccountInfoFacade.requestAccountDetails()

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

    override fun monitorMultiFactorAuthChanges() =
        monitorMultiFactorAuth.getEvents()

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

    override fun retryPendingConnections(disconnect: Boolean) {
        megaApiGateway.retryPendingConnections()
        megaChatApiGateway.retryPendingConnections(disconnect)
    }

    override suspend fun isBusinessAccountActive(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isBusinessAccountActive()
    }

    override suspend fun getSubscriptionPlans(): List<SubscriptionPlan> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getPricing(OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(subscriptionPlanListMapper(
                                request,
                                subscriptionPlanMapper,
                                currencyMapper,
                                skuMapper)))
                        } else {
                            continuation.failWithError(error)
                        }
                    }
                ))
            }
        }
}