package mega.privacy.android.feature.myaccount.presentation.achievements.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.feature.myaccount.presentation.achievements.model.AchievementsUIState
import mega.privacy.android.feature.myaccount.presentation.achievements.model.AwardAchievementExpirationStatus
import mega.privacy.android.feature.myaccount.presentation.achievements.util.getTextByDurationInDays
import mega.privacy.android.feature.myaccount.presentation.achievements.util.toStorageString
import mega.privacy.android.feature.myaccount.presentation.achievements.util.toUnitString
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.StartMEGAPWMFreeTrialEvent
import mega.privacy.mobile.analytics.event.StartMEGAVPNFreeTrialEvent

internal object AchievementsOverviewViewTestTags {
    private const val ACHIEVEMENTS_VIEW = "achievements_info_view"
    const val TOOLBAR = "${ACHIEVEMENTS_VIEW}:toolbar"
    const val HEADER = "${ACHIEVEMENTS_VIEW}:header"
    const val INVITE_BONUS_SECTION = "${ACHIEVEMENTS_VIEW}:invite_bonus_section"
    const val REGISTRATION_BONUS_SECTION = "${ACHIEVEMENTS_VIEW}:registration_bonus_section"
    const val INSTALL_MOBILE_SECTION = "${ACHIEVEMENTS_VIEW}:install_mobile_section"
    const val INSTALL_DESKTOP_SECTION = "${ACHIEVEMENTS_VIEW}:install_desktop_section"
    const val START_MEGA_VPN_FREE_TRIAL_SECTION =
        "${ACHIEVEMENTS_VIEW}:start_mega_vpn_free_trail_section"
    const val START_MEGA_PASS_FREE_TRIAL_SECTION =
        "${ACHIEVEMENTS_VIEW}:start_mega_vpn_pass_trail_section"
}

/**
 * Stateless Achievements overview screen in Jetpack Compose.
 */
@Composable
fun AchievementsOverviewView(
    uiState: AchievementsUIState,
    modifier: Modifier = Modifier,
    onInviteFriendsClicked: (Long) -> Unit = {},
    onShowInfoAchievementsClicked: (achievementType: AchievementType) -> Unit = {},
    onReferBonusesClicked: () -> Unit = {},
    onMegaVPNFreeTrialClicked: (Boolean, Long, Long, Int, Int?) -> Unit = { _, _, _, _, _ -> },
    onMegaPassFreeTrialClicked: (Boolean, Long, Long, Int, Int?) -> Unit = { _, _, _, _, _ -> },
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(AchievementsOverviewViewTestTags.TOOLBAR),
                titleId = sharedR.string.general_section_achievements,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colors.grey_020_dark_grey)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AchievementHeader(
                storageQuota = uiState.currentStorage?.toStorageString(LocalContext.current),
                AchievementsOverviewViewTestTags.HEADER
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {

                    Row(
                        Modifier
                            .testTag(AchievementsOverviewViewTestTags.INVITE_BONUS_SECTION)
                            .clickable {
                                if (uiState.hasReferrals) onReferBonusesClicked() else onInviteFriendsClicked(
                                    uiState.currentStorage ?: 0
                                )
                            }
                    ) {
                        AchievementListItem(
                            iconId = iconPackR.drawable.ic_referral_bonuses,
                            titleId = sharedR.string.title_referral_bonuses,
                            alphaLevel = if (uiState.areAllRewardsExpired) 0.5f else 1.0f,
                            zeroFiguresTitle = if (!uiState.hasReferrals) {
                                uiState.referralsStorage?.let {
                                    uiState.referralsDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_referrals_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_referrals_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (uiState.hasReferrals) {
                                uiState.referralsAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            buttonTitleId = sharedR.string.contact_invite,
                            onButtonClick = {
                                onInviteFriendsClicked(uiState.currentStorage ?: 0)
                            }
                        )
                    }

                    Divider(
                        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                        thickness = 1.dp
                    )

                    Row(
                        Modifier
                            .testTag(AchievementsOverviewViewTestTags.INSTALL_MOBILE_SECTION)
                            .clickable {
                                onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL)
                            }
                    ) {
                        AchievementListItem(
                            iconId = iconPackR.drawable.ic_install_mobile,
                            titleId = sharedR.string.title_install_app,
                            alphaLevel = uiState.installAppAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                ?: 1.0f,
                            zeroFiguresTitle = if (uiState.installAppAwardStorage <= 0) {
                                uiState.installAppStorage?.let {
                                    uiState.installAppDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (uiState.installAppAwardStorage > 0) {
                                uiState.installAppAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            daysLeft = uiState.installAppAwardDaysLeft,
                        )
                    }

                    Divider(
                        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                        thickness = 1.dp
                    )

                    if (uiState.hasRegistrationAward) {
                        Row(
                            Modifier
                                .testTag(AchievementsOverviewViewTestTags.REGISTRATION_BONUS_SECTION)
                                .clickable {
                                    onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
                                }
                        ) {
                            AchievementListItem(
                                iconId = iconPackR.drawable.ic_registration,
                                titleId = sharedR.string.title_regitration,
                                alphaLevel = uiState.registrationAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                    ?: 1.0f,
                                hasFiguresTitle = if (uiState.registrationAwardStorage > 0) {
                                    uiState.registrationAwardStorage.toUnitString(LocalContext.current)
                                } else null,
                                daysLeft = uiState.registrationAwardDaysLeft,
                            )
                        }

                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )
                    }

                    Row(
                        Modifier
                            .testTag(AchievementsOverviewViewTestTags.INSTALL_DESKTOP_SECTION)
                            .clickable {
                                onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)
                            }
                    ) {
                        AchievementListItem(
                            iconId = iconPackR.drawable.ic_install_mega,
                            titleId = sharedR.string.title_install_desktop,
                            alphaLevel = uiState.installDesktopAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                ?: 1.0f,
                            zeroFiguresTitle = if (uiState.installDesktopAwardStorage <= 0) {
                                uiState.installDesktopStorage?.let {
                                    uiState.installDesktopDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (uiState.installDesktopAwardStorage > 0) {
                                uiState.installDesktopAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            daysLeft = uiState.installDesktopAwardDaysLeft,
                        )
                    }

                    if (uiState.hasMegaVPNTrial) {
                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )

                        Row(
                            Modifier
                                .testTag(AchievementsOverviewViewTestTags.START_MEGA_VPN_FREE_TRIAL_SECTION)
                                .clickable {
                                    Analytics.tracker.trackEvent(StartMEGAVPNFreeTrialEvent)
                                    onMegaVPNFreeTrialClicked(
                                        uiState.megaVPNTrialAwardStorage > 0,
                                        uiState.megaVPNTrialStorage ?: 0,
                                        uiState.megaVPNTrialAwardStorage,
                                        uiState.megaVPNTrialDurationInDays,
                                        when (val daysLeft = uiState.megaVPNTrialAwardDaysLeft) {
                                            is AwardAchievementExpirationStatus.Valid ->
                                                daysLeft.daysLeft.toInt()

                                            is AwardAchievementExpirationStatus.Expired -> 0
                                            else -> null
                                        }
                                    )
                                }
                        ) {
                            TrialAchievementItem(
                                iconId = iconPackR.drawable.ic_mega_vpn_free_trial,
                                titleId = sharedR.string.title_start_mega_vpn_free_trial,
                                trialAwardStorage = uiState.megaVPNTrialAwardStorage,
                                trialStorage = uiState.megaVPNTrialStorage,
                                durationInDays = uiState.megaVPNTrialDurationInDays,
                                awardDaysLeft = uiState.megaVPNTrialAwardDaysLeft
                            )
                        }
                    }

                    if (uiState.hasMegaPassTrial) {
                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )

                        Row(
                            Modifier
                                .testTag(AchievementsOverviewViewTestTags.START_MEGA_PASS_FREE_TRIAL_SECTION)
                                .clickable {
                                    Analytics.tracker.trackEvent(StartMEGAPWMFreeTrialEvent)
                                    onMegaPassFreeTrialClicked(
                                        uiState.megaPassTrialAwardStorage > 0,
                                        uiState.megaPassTrialStorage ?: 0,
                                        uiState.megaPassTrialAwardStorage,
                                        uiState.megaPassTrialDurationInDays,
                                        when (val daysLeft = uiState.megaPassTrialAwardDaysLeft) {
                                            is AwardAchievementExpirationStatus.Valid ->
                                                daysLeft.daysLeft.toInt()

                                            is AwardAchievementExpirationStatus.Expired -> 0
                                            else -> null
                                        }
                                    )
                                }
                        ) {
                            TrialAchievementItem(
                                iconId = iconPackR.drawable.ic_mega_pass_free_trial,
                                titleId = sharedR.string.title_start_mega_pass_free_trial,
                                trialAwardStorage = uiState.megaPassTrialAwardStorage,
                                trialStorage = uiState.megaPassTrialStorage,
                                durationInDays = uiState.megaPassTrialDurationInDays,
                                awardDaysLeft = uiState.megaPassTrialAwardDaysLeft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialAchievementItem(
    @StringRes titleId: Int,
    @DrawableRes iconId: Int,
    trialAwardStorage: Long,
    trialStorage: Long?,
    durationInDays: Int,
    awardDaysLeft: AwardAchievementExpirationStatus?
) {
    if (trialAwardStorage > 0) {
        AwardedTrialAchievementItem(
            titleId = titleId,
            iconId = iconId,
            trialAwardStorage = trialAwardStorage,
            awardDaysLeft = awardDaysLeft
        )
    } else {
        UnawardedTrialAchievementItem(
            titleId = titleId,
            iconId = iconId,
            trialAwardStorage = trialAwardStorage,
            trialStorage = trialStorage,
            durationInDays = durationInDays
        )
    }
}

@Composable
private fun UnawardedTrialAchievementItem(
    @StringRes titleId: Int,
    @DrawableRes iconId: Int,
    trialAwardStorage: Long,
    trialStorage: Long?,
    durationInDays: Int,
) {
    AchievementListItem(
        iconId = iconId,
        titleId = titleId,
        zeroFiguresTitle =
            trialStorage?.let {
                durationInDays.getTextByDurationInDays(
                    context = LocalContext.current,
                    daysStringId = sharedR.string.figures_storage_achievements_text,
                    permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                    storage = it.toUnitString(LocalContext.current)
                )
            },
        hasFiguresTitle = null,
        applied = trialAwardStorage > 0
    )
}

@Composable
private fun AwardedTrialAchievementItem(
    @StringRes titleId: Int,
    @DrawableRes iconId: Int,
    trialAwardStorage: Long,
    awardDaysLeft: AwardAchievementExpirationStatus?,
) {
    val resources = LocalResources.current
    val isPermanent = awardDaysLeft is AwardAchievementExpirationStatus.Permanent
    AchievementListItem(
        iconId = iconId,
        titleId = titleId,
        zeroFiguresTitle = when (awardDaysLeft) {
            is AwardAchievementExpirationStatus.Valid ->
                resources.getQuantityString(
                    sharedR.plurals.trial_awarded_achievement_days_left_text,
                    awardDaysLeft.daysLeft.toInt(),
                    awardDaysLeft.daysLeft.toInt()
                )

            is AwardAchievementExpirationStatus.Expired ->
                resources.getString(sharedR.string.expired_label)

            else -> null
        },
        hasFiguresTitle = if (isPermanent) {
            trialAwardStorage.toUnitString(LocalContext.current)
        } else null,
        applied = isPermanent
    )
}

/**
 * Light Theme
 */
@Preview(showBackground = true)
@Composable
private fun AchievementsOverviewPreview() {
    OriginalTheme(false) {
        AchievementsOverviewView(
            uiState = AchievementsUIState(
                achievementsOverview = null,
                errorMessage = consumed(),
                currentStorage = 1337000000000,
                hasReferrals = true,
                areAllRewardsExpired = false,
                referralsStorage = 123,
                referralsAwardStorage = 1024000000,
                installAppStorage = 123,
                installAppAwardDaysLeft = 0,
                installAppAwardStorage = 12,
                hasRegistrationAward = true,
                registrationAwardDaysLeft = 0,
                registrationAwardStorage = 12,
                installDesktopStorage = 123,
                installDesktopAwardDaysLeft = 10,
                installDesktopAwardStorage = 0,
                hasMegaVPNTrial = true,
                hasMegaPassTrial = true,
                megaPassTrialStorage = 1024 * 1024 * 1024,
                megaPassTrialAwardStorage = 0,
                megaVPNTrialStorage = 1024 * 1024 * 1024,
                megaVPNTrialAwardStorage = 0,
                referralsDurationInDays = 365,
                installAppDurationInDays = 365,
                installDesktopDurationInDays = 365,
                megaPassTrialDurationInDays = 0,
                megaVPNTrialDurationInDays = 365,
                megaVPNTrialAwardDaysLeft = null,
                megaPassTrialAwardDaysLeft = null,
            )
        )
    }
}

/**
 * Dark Theme
 */
@Preview(showBackground = true)
@Composable
private fun AchievementsOverviewPreviewDark() {
    OriginalTheme(true) {
        AchievementsOverviewView(
            uiState = AchievementsUIState(
                achievementsOverview = null,
                errorMessage = consumed(),
                currentStorage = 13376969,
                hasReferrals = false,
                areAllRewardsExpired = false,
                referralsStorage = 0,
                referralsAwardStorage = 123000,
                installAppStorage = null,
                installAppAwardDaysLeft = 33,
                installAppAwardStorage = 12,
                hasRegistrationAward = false,
                registrationAwardDaysLeft = 333,
                registrationAwardStorage = 12,
                installDesktopStorage = null,
                installDesktopAwardDaysLeft = 14,
                installDesktopAwardStorage = 12,
                hasMegaVPNTrial = true,
                hasMegaPassTrial = true,
                megaPassTrialStorage = 5L * 1024 * 1024 * 1024,
                megaPassTrialAwardStorage = 5L * 1024 * 1024 * 1024,
                megaVPNTrialStorage = 5L * 1024 * 1024 * 1024,
                megaVPNTrialAwardStorage = 5L * 1024 * 1024 * 1024,
                referralsDurationInDays = 365,
                installAppDurationInDays = 365,
                installDesktopDurationInDays = 365,
                megaPassTrialDurationInDays = 365,
                megaVPNTrialDurationInDays = 365,
                megaVPNTrialAwardDaysLeft = AwardAchievementExpirationStatus.Permanent,
                megaPassTrialAwardDaysLeft = AwardAchievementExpirationStatus.Valid(100),
            )
        )
    }
}
