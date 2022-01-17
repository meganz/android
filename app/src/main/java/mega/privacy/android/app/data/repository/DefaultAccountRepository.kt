package mega.privacy.android.app.data.repository

import android.content.Context
import androidx.lifecycle.Observer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants.EVENT_2FA_UPDATED
import mega.privacy.android.app.data.facade.EventBusFacade
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.exception.MegaError
import mega.privacy.android.app.domain.exception.NoLoggedInUserError
import mega.privacy.android.app.domain.exception.NotMasterBusinessAccountError
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class DefaultAccountRepository @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val sdk: MegaApiAndroid,
    @ApplicationContext private val context: Context,
    private val eventBusFacade: EventBusFacade,
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

    override fun getRootNode(): MegaNode? {
        return sdk.rootNode
    }

    override fun isMultiFactorAuthAvailable(): Boolean {
        return sdk.multiFactorAuthAvailable()
    }

    override suspend fun isMultiFactorAuthEnabled(): Boolean {
        return suspendCoroutine { continuation ->
            sdk.multiFactorAuthCheck(sdk.myEmail, object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                    if (request?.type == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK) {
                        if (e?.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(request.flag))
                        } else {
                            continuation.resumeWith(
                                Result.failure(
                                    mega.privacy.android.app.domain.exception.MegaError(
                                        e?.errorCode,
                                        e?.errorString
                                    )
                                )
                            )
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun monitorMultiFactorAuthChanges(): Flow<Boolean> {
        val eventObservable =
            eventBusFacade.getEventObservable(EVENT_2FA_UPDATED, Boolean::class.java)

        return callbackFlow {
            val flowObserver = Observer(this::trySend)
            eventObservable.observeForever(flowObserver)
            awaitClose { eventObservable.removeObserver(flowObserver) }
        }

    }

    override suspend fun requestDeleteAccountLink() {
        /**
         * Initialize the cancellation of an account.
         *
         * The associated request type with this request is MegaRequest::TYPE_GET_CANCEL_LINK.
         *
         * If this request succeed, a cancellation link will be sent to the email address of the user.
         * If no user is logged in, you will get the error code MegaError::API_EACCESS in onRequestFinish().
         *
         * If the MEGA account is a sub-user business account, onRequestFinish will
         * be called with the error code MegaError::API_EMASTERONLY.
         *
         * @see MegaApi::confirmCancelAccount
         *
         * @param listener MegaRequestListener to track this request
         */

        return suspendCoroutine { continuation ->
            sdk.cancelAccount(object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                    if (request?.type == MegaRequest.TYPE_GET_CANCEL_LINK) {
                        when (e?.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(Unit))
                            }
                            MegaError.API_EACCESS -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        NoLoggedInUserError(
                                            e.errorCode,
                                            e.errorString
                                        )
                                    )
                                )
                            }
                            MegaError.API_EMASTERONLY -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        NotMasterBusinessAccountError(
                                            e.errorCode,
                                            e.errorString
                                        )
                                    )
                                )
                            }
                            else -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        mega.privacy.android.app.domain.exception.MegaError(
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
}