package mega.privacy.android.app.presentation.achievements

import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * UIMegaAchievement
 *
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
data class UIMegaAchievement(
    val invitedEmails: List<String>,
    val grantedStorage: Long,
    val grantedTransferQuota: Long,
    val unlockedAwardsCount: Long,
    val achievementType: AchievementType,
    val awardId: Int,
    val awardExpirationTimeStamp: Long,
    val rewardAwardId: Int,
    val rewardStorageByAwardId: Long,
    val rewardTransferByAwardId: Long,
)
