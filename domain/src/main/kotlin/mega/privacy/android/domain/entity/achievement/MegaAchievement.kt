package mega.privacy.android.domain.entity.achievement


/**
 * MegaAchievement to include all the  common values for an achievement
 *
 * @property baseStorage    Base storage
 * @property invitedEmails : List of emails referred by user
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
data class MegaAchievement(
    val baseStorage: Long? = null,
    val invitedEmails: List<String> = emptyList(),
    val awardId: Int = 0,
    val grantedStorage: Long = 0L,
    val grantedTransferQuota: Long = 0L,
    val unlockedAwardsCount: Long = 0L,
    val achievementType: AchievementType = AchievementType.INVALID_ACHIEVEMENT,
    val awardExpirationTimeStamp: Long = 0L,
    val rewardAwardId: Int = 0,
    val rewardStorageByAwardId: Long = 0L,
    val rewardTransferByAwardId: Long = 0L,
)
