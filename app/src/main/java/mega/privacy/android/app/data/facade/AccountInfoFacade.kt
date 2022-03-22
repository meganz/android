package mega.privacy.android.app.data.facade

import mega.privacy.android.app.globalmanagement.MyAccountInfo
import javax.inject.Inject

/**
 * Account info facade
 *
 * Implements [AccountInfoWrapper] and provides a facade over [MyAccountInfo]
 *
 * @property myAccountInfo
 */
class AccountInfoFacade @Inject constructor(
    private val myAccountInfo: MyAccountInfo
) : AccountInfoWrapper {
    override val storageCapacityUsedAsFormattedString: String
        get() = myAccountInfo.usedFormatted
    override val accountTypeId: Int
        get() = myAccountInfo.accountType
}