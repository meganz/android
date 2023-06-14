package mega.privacy.android.app.main.megaachievements

import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements

/**
 * UI State for [ReferralBonusView]
 * @property awardedInviteAchievements list of the Referral Bonus Achievements with the type of [AwardedAchievementInvite]
 */
data class ReferralBonusesUIState(
    val awardedInviteAchievements: List<ReferralBonusAchievements> = emptyList(),
)