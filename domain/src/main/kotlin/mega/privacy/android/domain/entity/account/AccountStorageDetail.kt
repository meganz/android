package mega.privacy.android.domain.entity.account

/**
 * Account storage detail
 *
 * @property usedCloudDrive
 * @property usedRubbish
 * @property usedIncoming
 * @property totalStorage
 * @property usedStorage
 * @property subscriptionMethodId
 */
data class AccountStorageDetail(
    val usedCloudDrive: Long,
    val usedRubbish: Long,
    val usedIncoming: Long,
    val totalStorage: Long,
    val usedStorage: Long,
    val subscriptionMethodId: Int,
) {
    /**
     * Used percentage
     */
    val usedPercentage: Int
        get() = (100 * usedStorage / totalStorage).toInt()

    /**
     * Available space
     */
    val availableSpace: Long
        get() = totalStorage - usedStorage
}