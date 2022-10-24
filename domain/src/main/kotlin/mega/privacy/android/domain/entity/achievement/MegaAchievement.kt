package mega.privacy.android.domain.entity.achievement

/**
 * MegaAchievement to include all the  common values for an achievement
 *
 * @property grantedStorage: Long : The granted storage (in bytes) for a specific type of achievement
 * @property grantedTransferQuota: Long : The granted transfer quota (in bytes) for a specific type of achievement
 * @property unlockedAwardsCount: Int : The number of awards unlocked for this account
 * @property achievementType: @AchievementType : Achievement type
 * @property awardId: Int : Award Id
 * @property awardExpirationTimeStamp: Long : Award expiration time stamp
 * @property rewardAwardId: Int : Reward Id
 * @property rewardStorageByAwardId: Long : Storage rewarded by specific award Id
 * @property rewardTransferByAwardId: Long : Transfer rewarded by specific award Id
 */
sealed interface MegaAchievement {
    val grantedStorage: Long
    val grantedTransferQuota: Long
    val unlockedAwardsCount: Long
    val achievementType: AchievementType
    val awardId: Int
    val awardExpirationTimeStamp: Long
    val rewardAwardId: Int
    val rewardStorageByAwardId: Long
    val rewardTransferByAwardId: Long
}

/**
 * DefaultMegaAchievement
 *
 * @property invitedEmails : List of emails referred by user
 */
data class DefaultMegaAchievement(
    val invitedEmails: List<String> = emptyList(),
    override val awardId: Int = 0,
    override val grantedStorage: Long = 0L,
    override val grantedTransferQuota: Long = 0L,
    override val unlockedAwardsCount: Long = 0L,
    override val achievementType: AchievementType = AchievementType.INVALID_ACHIEVEMENT,
    override val awardExpirationTimeStamp: Long = 0L,
    override val rewardAwardId: Int = 0,
    override val rewardStorageByAwardId: Long = 0L,
    override val rewardTransferByAwardId: Long = 0L,
) : MegaAchievement

