package mega.privacy.android.domain.entity.achievement

import mega.privacy.android.domain.entity.contacts.ContactItem

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
 * @property expirationTimestampInSeconds After this moment, the storage and transfer quota granted.
 * as result of the award will not be valid anymore. For example "expirationTimestampInDays = 86400" is 1 day.
 * 1 day = 86400 seconds
 * No timestamp conversion needed
 * @property rewardedStorageInBytes The storage rewarded by the award
 * @property rewardedTransferInBytes The transfer quota rewarded by the award
 */
open class AwardedAchievement(
    open val awardId: Int,
    open val type: AchievementType,
    open val expirationTimestampInSeconds: Long,
    open val rewardedStorageInBytes: Long,
    open val rewardedTransferInBytes: Long,
)

/**
 * AwardedAchievement of type INVITE. It adds an additional field to store the referred emails
 *
 * @property referredEmails field that is specific for the achievements of
 * class MEGA_ACHIEVEMENT_INVITE. It contains the list of referred emails for the award.
 */
open class AwardedAchievementInvite(
    override val awardId: Int,
    override val expirationTimestampInSeconds: Long,
    override val rewardedStorageInBytes: Long,
    override val rewardedTransferInBytes: Long,
    open val referredEmails: List<String>,
) : AwardedAchievement(
    awardId,
    AchievementType.MEGA_ACHIEVEMENT_INVITE,
    expirationTimestampInSeconds,
    rewardedStorageInBytes,
    rewardedTransferInBytes
) {
    constructor(awardedAchievement: AwardedAchievement, referredEmails: List<String>) : this(
        awardId = awardedAchievement.awardId,
        expirationTimestampInSeconds = awardedAchievement.expirationTimestampInSeconds,
        rewardedStorageInBytes = awardedAchievement.rewardedStorageInBytes,
        rewardedTransferInBytes = awardedAchievement.rewardedTransferInBytes,
        referredEmails = referredEmails
    )
}

/**
 * Extension class to include
 * @property contact the contact of the user you receive an achievements for
 * @property expirationInDays the number of days left to an expired referral bonus
 * This class is only an extension to modify the existing [AwardedAchievementInvite]
 */
data class ReferralBonusAchievements(
    val contact: ContactItem?,
    val expirationInDays: Long,
    override val awardId: Int,
    override val expirationTimestampInSeconds: Long,
    override val rewardedStorageInBytes: Long,
    override val rewardedTransferInBytes: Long,
    override val referredEmails: List<String>,
) : AwardedAchievementInvite(
    awardId,
    expirationTimestampInSeconds,
    rewardedStorageInBytes,
    rewardedTransferInBytes,
    referredEmails
)
