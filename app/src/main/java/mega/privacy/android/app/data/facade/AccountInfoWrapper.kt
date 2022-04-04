package mega.privacy.android.app.data.facade

/**
 * Account info wrapper to create an interface to Account information functionality
 *
 */
interface AccountInfoWrapper {
    /**
     * Storage capacity used as a formatted string
     */
    val storageCapacityUsedAsFormattedString: String

    /**
     * Account type id
     *
     * Options:
     *
     * Default/Invalid = -1
     *
     * MegaAccountDetails.ACCOUNT_TYPE_FREE = 0
     * MegaAccountDetails.ACCOUNT_TYPE_PROI = 1
     * MegaAccountDetails.ACCOUNT_TYPE_PROII = 2
     * MegaAccountDetails.ACCOUNT_TYPE_PROIII = 3
     * MegaAccountDetails.ACCOUNT_TYPE_LITE = 4
     * MegaAccountDetails.ACCOUNT_TYPE_BUSINESS = 100
     */
    val accountTypeId: Int
}
