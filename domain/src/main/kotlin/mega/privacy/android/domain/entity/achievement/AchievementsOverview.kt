package mega.privacy.android.domain.entity.achievement

data class AchievementsOverview(
    val allAchievements: List<Achievement>,
    val awardedAchievements: List<AwardedAchievement>,
    val currentStorageInBytes: Long,
    val achievedStorageFromReferralsInBytes: Long,
    val achievedTransferFromReferralsInBytes: Long,
)

data class Achievement(
    val grantStorageInBytes: Long,
    val grantTransferQuotaInBytes: Long,
    val type: AchievementType,
    val durationInDays: Int,
)

open class AwardedAchievement(
    open val awardId: Int,
    open val type: AchievementType,
    open val expirationTimestampInDays: Long,
    open val rewardedStorageInBytes: Long,
    open val rewardedTransferInBytes: Long,
)

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
