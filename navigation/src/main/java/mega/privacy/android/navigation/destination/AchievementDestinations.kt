package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Navigation key for the referral bonuses screen.
 */
@Serializable
data object ReferralBonusNavKey : NavKey

/**
 * Navigation key for the achievement info screen.
 *
 * @param achievementType The achievement whose details are shown.
 */
@Serializable
data class AchievementsInfoNavKey(
    val achievementType: AchievementType,
) : NavKey

/**
 * Navigation key for the invite friends screen.
 *
 * @param grantStorageInBytes Storage bonus granted for inviting friends, in bytes.
 */
@Serializable
data class InviteFriendsNavKey(
    val grantStorageInBytes: Long,
) : NavKey

/**
 * Navigation key for the MEGA VPN free trial screen.
 *
 * @param isReceivedAward Whether the free trial award has already been received.
 * @param storageAmount Storage amount shown on the screen.
 * @param awardStorageAmount Storage amount granted by the award.
 * @param durationInDays Duration of the free trial in days.
 * @param awardDaysLeft Days left on the award, or null when not applicable.
 */
@Serializable
data class MegaVpnFreeTrialNavKey(
    val isReceivedAward: Boolean,
    val storageAmount: Long,
    val awardStorageAmount: Long,
    val durationInDays: Int,
    val awardDaysLeft: Int?,
) : NavKey

/**
 * Navigation key for the MEGA Pass free trial screen.
 *
 * @param isReceivedAward Whether the free trial award has already been received.
 * @param storageAmount Storage amount shown on the screen.
 * @param awardStorageAmount Storage amount granted by the award.
 * @param durationInDays Duration of the free trial in days.
 * @param awardDaysLeft Days left on the award, or null when not applicable.
 */
@Serializable
data class MegaPassFreeTrialNavKey(
    val isReceivedAward: Boolean,
    val storageAmount: Long,
    val awardStorageAmount: Long,
    val durationInDays: Int,
    val awardDaysLeft: Int?,
) : NavKey
