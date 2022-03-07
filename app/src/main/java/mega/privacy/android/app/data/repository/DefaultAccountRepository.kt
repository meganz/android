package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.failWithException
import mega.privacy.android.app.data.extensions.isType
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.exception.NoLoggedInUserException
import mega.privacy.android.app.domain.exception.NotMasterBusinessAccountException
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@ExperimentalContracts
class DefaultAccountRepository @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val sdk: MegaApiAndroid,
    @ApplicationContext private val context: Context,
    private val monitorMultiFactorAuth: MonitorMultiFactorAuth,
) : AccountRepository {
    override fun getUserAccount(): UserAccount {
        return UserAccount(
            sdk.myEmail,
            sdk.isBusinessAccount,
            sdk.isMasterBusinessAccount,
            myAccountInfo.accountType
        )
    }

    override fun isAccountDataStale(): Boolean {
        LogUtil.logDebug("Check the last call to getAccountDetails")
        return DBUtil.callToAccountDetails() || myAccountInfo.usedFormatted.isBlank()
    }

    override fun requestAccount() {
        (context as MegaApplication).askForAccountDetails()
    }

    override fun getRootNode(): MegaNode? = sdk.rootNode

    override fun isMultiFactorAuthAvailable(): Boolean {
        return sdk.multiFactorAuthAvailable()
    }

    override suspend fun isMultiFactorAuthEnabled(): Boolean {
        return suspendCoroutine { continuation ->
            sdk.multiFactorAuthCheck(
                sdk.myEmail,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onMultiFactorAuthCheckRequestFinish(continuation)
                )
            )
        }
    }

    private fun onMultiFactorAuthCheckRequestFinish(
        continuation: Continuation<Boolean>
    ) = { request: MegaRequest, error: MegaError ->
        if (request.isType(MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK)) {
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else continuation.failWithError(error)
        }
    }

    override fun monitorMultiFactorAuthChanges(): Flow<Boolean> {
        return monitorMultiFactorAuth.getEvents()
    }

    override suspend fun requestDeleteAccountLink() {
        return suspendCoroutine { continuation ->
            sdk.cancelAccount(
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


}