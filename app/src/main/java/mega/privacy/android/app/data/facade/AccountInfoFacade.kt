package mega.privacy.android.app.data.facade

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.iab.BillingConstant.PAYMENT_GATEWAY
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.feature.payment.model.AccountTypeInt
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account info facade
 *
 * Implements [AccountInfoWrapper] and provides a facade over [MyAccountInfo]
 *
 * @property myAccountInfo
 */
@Singleton
class AccountInfoFacade @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val db: Lazy<DatabaseHandler>,
) : AccountInfoWrapper {
    private val submittedPurchaseTokens = mutableSetOf<String>()

    override val storageCapacityUsedAsFormattedString: String
        get() = myAccountInfo.usedFormatted
    override val accountTypeId: Int
        get() = myAccountInfo.accountType
    override val accountTypeString: String
        get() = getAccountTypeLabel(myAccountInfo.accountType)

    override suspend fun handleAccountDetail(request: MegaRequest) {
        val storage = request.numDetails and MyAccountInfo.HAS_STORAGE_DETAILS != 0
        if (storage && megaApiGateway.getRootNode() != null) {
            db.get().setAccountDetailsTimeStamp()
        }
        val megaAccountDetails = request.megaAccountDetails ?: return
        // backward compatible, it will replace by new AccountDetail domain entity
        myAccountInfo.setAccountDetails(megaAccountDetails, request.numDetails, context)
        val sessions =
            request.numDetails and MyAccountInfo.HAS_SESSIONS_DETAILS != 0
        if (sessions) {
            val megaAccountSession = megaAccountDetails.getSession(0) ?: return
            Timber.d("getMegaAccountSESSION not Null")
            db.get().setExtendedAccountDetailsTimestamp()
            val mostRecentSession: Long = megaAccountSession.mostRecentUsage
            val date: String = TimeUtils.formatDateAndTime(
                context,
                mostRecentSession,
                TimeUtils.DATE_LONG_FORMAT
            )
            myAccountInfo.lastSessionFormattedDate = date
            myAccountInfo.createSessionTimeStamp = megaAccountSession.creationTimestamp
            Timber.d("onRequest TYPE_ACCOUNT_DETAILS: %s", myAccountInfo.usedPercentage)
        }
    }

    override suspend fun resetAccountInfo() = myAccountInfo.resetDefaults()

    override fun submitSubscriptions(purchase: MegaPurchase) {
        Timber.d("submit subscription: $purchase")
        val token = purchase.token
        if (token in submittedPurchaseTokens) {
            Timber.d("Purchase token already submitted, skipping: $token")
            return
        }
        val json = purchase.receipt

        val listener = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _, error ->
                when (error.errorCode) {
                    MegaError.API_OK, MegaError.API_EEXIST, MegaError.API_EEXPIRED -> {
                        token?.let { submittedPurchaseTokens.add(it) }
                    }

                    else -> {
                        Timber.e("PURCHASE WRONG: ${error.errorString} (${error.errorCode})")
                    }
                }
            }
        )

        Timber.d("megaApi.submitPurchaseReceipt is invoked")
        megaApiGateway.submitPurchaseReceipt(PAYMENT_GATEWAY, json, listener)
    }

    private fun getAccountTypeLabel(accountType: Int?) = with(context) {
        when (accountType) {
            AccountTypeInt.FREE -> getString(R.string.my_account_free)
            AccountTypeInt.PRO_I -> getString(R.string.my_account_pro1)
            AccountTypeInt.PRO_II -> getString(R.string.my_account_pro2)
            AccountTypeInt.PRO_III -> getString(R.string.my_account_pro3)
            AccountTypeInt.PRO_LITE -> getString(R.string.my_account_prolite_feedback_email)
            AccountTypeInt.STARTER -> getString(sharedR.string.starter_account)
            AccountTypeInt.BASIC -> getString(sharedR.string.basic_account)
            AccountTypeInt.ESSENTIAL -> getString(sharedR.string.essential_account)
            Constants.BUSINESS -> getString(R.string.business_label)
            else -> getString(R.string.my_account_free)
        }
    }
}