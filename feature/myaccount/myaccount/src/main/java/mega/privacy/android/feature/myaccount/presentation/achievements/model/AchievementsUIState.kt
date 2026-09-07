package mega.privacy.android.feature.myaccount.presentation.achievements.model

import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.achievement.AchievementsOverview

/**
 * UI State for achievement composable
 *
 * @property achievementsOverview Overview for all achievements
 * @property currentStorage Current storage from achievements
 * @property areAllRewardsExpired Are all rewards expired
 * @property errorMessage error message to be shown to the user
 * @property hasReferrals User has any referrals accomplished
 * @property referralsStorage Storage by referrals
 * @property referralsAwardStorage Storage awarded by referrals
 * @property referralsDurationInDays Duration in days for referrals
 * @property installAppStorage Storage by installing app
 * @property installAppAwardDaysLeft Storage awarded by installing app days left
 * @property installAppAwardStorage Storage awarded by installing app left
 * @property installAppDurationInDays Duration in days for installing app
 * @property installDesktopStorage Storage by installing desktop
 * @property installDesktopAwardDaysLeft Storage awarded by installing desktop days left
 * @property installDesktopAwardStorage Storage awarded by installing desktop left
 * @property installDesktopDurationInDays Duration in days for installing desktop
 * @property hasRegistrationAward User got award for registration
 * @property registrationAwardDaysLeft Storage awarded by registration days left
 * @property registrationAwardStorage Storage awarded by registration left
 * @property hasMegaPassTrial User has a Mega Pass trial
 * @property hasMegaVPNTrial User has a Mega VPN trial
 * @property megaPassTrialAwardDaysLeft Storage awarded by Mega Pass trial days left
 * @property megaPassTrialAwardStorage Storage awarded by Mega Pass trial left
 * @property megaVPNTrialAwardDaysLeft Storage awarded by Mega VPN trial days left
 * @property megaVPNTrialAwardStorage Storage awarded by Mega VPN trial left
 * @property megaPassTrialStorage Storage by Mega Pass trial
 * @property megaVPNTrialStorage Storage by Mega VPN trial
 * @property megaPassTrialDurationInDays Duration in days for Mega Pass trial
 * @property megaVPNTrialDurationInDays Duration in days for Mega VPN trial
 *
 **/
data class AchievementsUIState(
    val achievementsOverview: AchievementsOverview?,
    val currentStorage: Long?,
    val areAllRewardsExpired: Boolean,
    val errorMessage: StateEventWithContent<Int>,
    val hasReferrals: Boolean,
    val referralsStorage: Long?,
    val referralsAwardStorage: Long,
    val referralsDurationInDays: Int,
    val installAppStorage: Long?,
    val installAppAwardDaysLeft: Long?,
    val installAppAwardStorage: Long,
    val installAppDurationInDays: Int,
    val installDesktopStorage: Long?,
    val installDesktopAwardDaysLeft: Long?,
    val installDesktopAwardStorage: Long,
    val installDesktopDurationInDays: Int,
    val hasRegistrationAward: Boolean,
    val registrationAwardDaysLeft: Long?,
    val registrationAwardStorage: Long,
    val hasMegaPassTrial: Boolean,
    val megaPassTrialStorage: Long?,
    val megaPassTrialAwardDaysLeft: AwardAchievementExpirationStatus?,
    val megaPassTrialAwardStorage: Long,
    val megaPassTrialDurationInDays: Int,
    val hasMegaVPNTrial: Boolean,
    val megaVPNTrialStorage: Long?,
    val megaVPNTrialAwardDaysLeft: AwardAchievementExpirationStatus?,
    val megaVPNTrialAwardStorage: Long,
    val megaVPNTrialDurationInDays: Int,
)
