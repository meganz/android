package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.exception.NoLoggedInUserException
import mega.privacy.android.app.domain.exception.NotMasterBusinessAccountException
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
                                    MegaException(
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

    override fun monitorMultiFactorAuthChanges(): Flow<Boolean> {
        return monitorMultiFactorAuth.getEvents()
    }

    override suspend fun requestDeleteAccountLink() {
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
                                        NoLoggedInUserException(
                                            e.errorCode,
                                            e.errorString
                                        )
                                    )
                                )
                            }
                            MegaError.API_EMASTERONLY -> {
                                continuation.resumeWith(
                                    Result.failure(
                                        NotMasterBusinessAccountException(
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
}