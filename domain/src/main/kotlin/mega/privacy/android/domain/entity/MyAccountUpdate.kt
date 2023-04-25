package mega.privacy.android.domain.entity

/**
 * My Account Action Data Holder
 * @property action
 * @property storageState
 */
data class MyAccountUpdate(
    val action: Action,
    val storageState: StorageState?,
) {
    /**
     * My Account Action Type
     */
    enum class Action {
        /**
         * Storage State Changed
         */
        STORAGE_STATE_CHANGED,

        /**
         * Update Account Details
         */
        UPDATE_ACCOUNT_DETAILS
    }
}