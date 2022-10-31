package mega.privacy.android.app.presentation.achievements

import mega.privacy.android.domain.entity.achievement.MegaAchievement

/**
 * UIMegaAchievementMapper
 */
typealias UIMegaAchievementMapper = (
    @JvmSuppressWildcards MegaAchievement,
) -> @JvmSuppressWildcards UIMegaAchievement

/**
 * Mapper for mapping [MegaAchievement] to [UIMegaAchievement]
 *
 * @param megaAchievement : [MegaAchievement]
 * @return [UIMegaAchievement]
 */
fun toUIMegaAchievement(
    megaAchievement: MegaAchievement,
): UIMegaAchievement = with(megaAchievement) {
    return UIMegaAchievement(
        invitedEmails = invitedEmails,
        grantedStorage = grantedStorage,
        grantedTransferQuota = grantedTransferQuota,
        unlockedAwardsCount = unlockedAwardsCount,
        achievementType = achievementType,
        awardId = awardId,
        awardExpirationTimeStamp = awardExpirationTimeStamp,
        rewardAwardId = rewardAwardId,
        rewardStorageByAwardId = rewardStorageByAwardId,
        rewardTransferByAwardId = rewardTransferByAwardId
    )
}