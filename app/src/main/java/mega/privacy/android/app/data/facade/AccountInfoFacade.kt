package mega.privacy.android.app.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.facade.AccountInfoWrapper
import javax.inject.Inject

/**
 * Account info facade
 *
 * Implements [AccountInfoWrapper] and provides a facade over [MyAccountInfo]
 *
 * @property myAccountInfo
 */
class AccountInfoFacade @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @ApplicationContext private val context: Context,
) : AccountInfoWrapper {
    override val storageCapacityUsedAsFormattedString: String
        get() = myAccountInfo.usedFormatted
    override val accountTypeId: Int
        get() = myAccountInfo.accountType
    override val accountTypeString: String
        get() = getAccountTypeLabel(myAccountInfo.accountType)

    override fun requestAccountDetails() {
        (context as MegaApplication).askForAccountDetails()
    }

    private fun getAccountTypeLabel(accountType: Int?) = with(context) {
        when (accountType) {
            Constants.FREE -> getString(R.string.my_account_free)
            Constants.PRO_I -> getString(R.string.my_account_pro1)
            Constants.PRO_II -> getString(R.string.my_account_pro2)
            Constants.PRO_III -> getString(R.string.my_account_pro3)
            Constants.PRO_LITE -> getString(R.string.my_account_prolite_feedback_email)
            Constants.BUSINESS -> getString(R.string.business_label)
            else -> getString(R.string.my_account_free)
        }
    }
}