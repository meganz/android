package mega.privacy.android.domain.entity.achievement

/**
 * Overview of all the existing achievements and rewards.
 *
 * @property allAchievements List of all of the achievements that a user can unlock
 * @property awardedAchievements List of achievements that a user has unlocked
 * @property currentStorageInBytes all the storage granted as result of the unlocked achievements
 * @property achievedStorageFromReferralsInBytes All the storage granted as result of
 * the successful invitations (referrals)
 * @property achievedTransferFromReferralsInBytes All the transfer quota granted as result
 * of the successful invitations (referrals)
 */
data class AchievementsOverview(
    val allAchievements: List<Achievement>,
    val awardedAchievements: List<AwardedAchievement>,
    val currentStorageInBytes: Long,
    val achievedStorageFromReferralsInBytes: Long,
    val achievedTransferFromReferralsInBytes: Long,
)

/**
 * Represents an achievement that a user can unlock.
 *
 * @property grantStorageInBytes The storage that will be granted to the user
 * if the achievement is unlocked
 * @property grantTransferQuotaInBytes The transfer quota that will be granted to the user
 * if the achievement is unlocked
 * @property type Type of the achievement
 * @property durationInDays Number of days after which the storage and transfer quota
 * will expire after the achievement is unlocked
 */
data class Achievement(
    val grantStorageInBytes: Long,
    val grantTransferQuotaInBytes: Long,
    val type: AchievementType,
    val durationInDays: Int,
)

/**
 * Represents an achievement that a user has unlocked.
 *
 * @property awardId Id of the award
 * @property type Type of the achievement
 * @property expirationTimestampInDays After this moment, the storage and transfer quota granted
 * as result of the award will not be valid anymore.
 * @property rewardedStorageInBytes The storage rewarded by the award
 * @property rewardedTransferInBytes The transfer quota rewarded by the award
 */
open class AwardedAchievement(
    open val awardId: Int,
    open val type: AchievementType,
    open val expirationTimestampInDays: Long,
    open val rewardedStorageInBytes: Long,
    open val rewardedTransferInBytes: Long,
)

/**
 * AwardedAchievement of type INVITE. It adds an additional field to store the referred emails
 *
 * @property referredEmails field that is specific for the achievements of
 * class MEGA_ACHIEVEMENT_INVITE. It contains the list of referred emails for the award.
 */
data class AwardedAchievementInvite(
    override val awardId: Int,
    override val expirationTimestampInDays: Long,
    override val rewardedStorageInBytes: Long,
    override val rewardedTransferInBytes: Long,
    val referredEmails: List<String>,
) : AwardedAchievement(awardId,
    AchievementType.MEGA_ACHIEVEMENT_INVITE,
    expirationTimestampInDays,
    rewardedStorageInBytes,
    rewardedTransferInBytes
) {
    constructor(awardedAchievement: AwardedAchievement, referredEmails: List<String>) : this(
        awardId = awardedAchievement.awardId,
        expirationTimestampInDays = awardedAchievement.expirationTimestampInDays,
        rewardedStorageInBytes = awardedAchievement.rewardedStorageInBytes,
        rewardedTransferInBytes = awardedAchievement.rewardedTransferInBytes,
        referredEmails = referredEmails
    )
}
