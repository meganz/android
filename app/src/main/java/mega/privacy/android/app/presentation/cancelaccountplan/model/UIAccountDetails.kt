package mega.privacy.android.app.presentation.cancelaccountplan.model

/**
 * Data class to hold account details .
 *
 * @property accountType The account type.
 * @property freeStorageQuota The free storage quota.
 * @property rewindDaysQuota The rewind days quota.
 * @property usedStorageSize The used storage size.
 * @property storageQuotaSize The storage quota size.
 * @property transferQuotaSize The transfer quota size.
 */
data class UIAccountDetails(
    val accountType: String,
    val freeStorageQuota: String,
    val rewindDaysQuota: String,
    val usedStorageSize: String,
    val storageQuotaSize: String,
    val transferQuotaSize: String,
)
