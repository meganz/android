package mega.privacy.android.app.presentation.achievements.model

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

/**
 * UI State for achievement composable
 *
 * @property achievementsOverview Overview for all achievements
 * @property currentStorage Current storage from achievements
 * @property areAllRewardsExpired Are all rewards expired
 * @property showError Show error state
 * @property hasReferrals User has any referrals accomplished
 * @property referralsStorage Storage by referrals
 * @property referralsAwardStorage Storage awarded by referrals
 * @property installAppStorage Storage by installing app
 * @property installAppAwardDaysLeft Storage awarded by installing app days left
 * @property installAppAwardStorage Storage awarded by installing app left
 * @property installDesktopStorage Storage by installing desktop
 * @property installDesktopAwardDaysLeft Storage awarded by installing desktop days left
 * @property installDesktopAwardStorage Storage awarded by installing desktop left
 * @property hasRegistrationAward User got award for registration
 * @property registrationAwardDaysLeft Storage awarded by registration days left
 * @property registrationAwardStorage Storage awarded by registration left
 *
 **/
data class AchievementsUIState(
    val achievementsOverview: AchievementsOverview? = null,
    val currentStorage: Long? = null,
    val areAllRewardsExpired: Boolean = false,
    val showError: Boolean = false,
    val hasReferrals: Boolean = false,
    val referralsStorage: Long? = null,
    val referralsAwardStorage: Long = 0,
    val installAppStorage: Long? = null,
    val installAppAwardDaysLeft: Long? = null,
    val installAppAwardStorage: Long = 0,
    val installDesktopStorage: Long? = null,
    val installDesktopAwardDaysLeft: Long? = null,
    val installDesktopAwardStorage: Long = 0,
    val hasRegistrationAward: Boolean = false,
    val registrationAwardDaysLeft: Long? = null,
    val registrationAwardStorage: Long = 0,
)
