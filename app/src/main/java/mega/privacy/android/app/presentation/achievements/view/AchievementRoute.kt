package mega.privacy.android.app.presentation.achievements.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.getTextByDurationInDays
import mega.privacy.android.app.data.extensions.toStorageString
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.presentation.achievements.AchievementsOverviewViewModel
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.StartMEGAPWMFreeTrialEvent
import mega.privacy.mobile.analytics.event.StartMEGAVPNFreeTrialEvent

internal object AchievementViewTestTags {
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

@Composable
internal fun AchievementRoute(
    viewModel: AchievementsOverviewViewModel = hiltViewModel(),
    onNavigateToInviteFriends: (Long) -> Unit,
    onNavigateToInfoAchievements: (achievementType: AchievementType) -> Unit,
    onNavigateToReferralBonuses: () -> Unit,
    onNavigateToMegaVPNFreeTrial: (Boolean, Long, Long, Int) -> Unit,
    onNavigateToMegaPassFreeTrial: (Boolean, Long, Long, Int) -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    AchievementView(
        modifier = Modifier
            .background(MaterialTheme.colors.grey_020_dark_grey),
        currentStorage = uiState.currentStorage,
        hasReferrals = uiState.hasReferrals,
        areAllRewardsExpired = uiState.areAllRewardsExpired,
        referralsStorage = uiState.referralsStorage,
        referralsAwardStorage = uiState.referralsAwardStorage,
        installAppStorage = uiState.installAppStorage,
        installAppAwardDaysLeft = uiState.installAppAwardDaysLeft,
        installAppAwardStorage = uiState.installAppAwardStorage,
        hasRegistrationAward = uiState.hasRegistrationAward,
        registrationAwardDaysLeft = uiState.registrationAwardDaysLeft,
        registrationAwardStorage = uiState.registrationAwardStorage,
        installDesktopStorage = uiState.installDesktopStorage,
        installDesktopAwardDaysLeft = uiState.installDesktopAwardDaysLeft,
        installDesktopAwardStorage = uiState.installDesktopAwardStorage,
        onInviteFriendsClicked = onNavigateToInviteFriends,
        onShowInfoAchievementsClicked = onNavigateToInfoAchievements,
        onReferBonusesClicked = onNavigateToReferralBonuses,
        onMegaVPNFreeTrialClicked = onNavigateToMegaVPNFreeTrial,
        onMegaPassFreeTrialClicked = onNavigateToMegaPassFreeTrial,
        hasMegaPassTrial = uiState.hasMegaPassTrial,
        megaPassTrialStorage = uiState.megaPassTrialStorage,
        megaPassTrialAwardStorage = uiState.megaPassTrialAwardStorage,
        hasMegaVPNTrial = uiState.hasMegaVPNTrial,
        megaVPNTrialStorage = uiState.megaVPNTrialStorage,
        megaVPNTrialAwardStorage = uiState.megaVPNTrialAwardStorage,
        referralsDurationInDays = uiState.referralsDurationInDays,
        installAppDurationInDays = uiState.installAppDurationInDays,
        installDesktopDurationInDays = uiState.installDesktopDurationInDays,
        megaPassTrialDurationInDays = uiState.megaPassTrialDurationInDays,
        megaVPNTrialDurationInDays = uiState.megaVPNTrialDurationInDays,
    )
}

@Composable
internal fun AchievementView(
    modifier: Modifier = Modifier,
    currentStorage: Long?,
    hasReferrals: Boolean,
    areAllRewardsExpired: Boolean,
    referralsStorage: Long?,
    referralsAwardStorage: Long,
    referralsDurationInDays: Int,
    installAppStorage: Long?,
    installAppAwardDaysLeft: Long?,
    installAppAwardStorage: Long,
    installAppDurationInDays: Int,
    hasRegistrationAward: Boolean,
    registrationAwardDaysLeft: Long?,
    registrationAwardStorage: Long,
    installDesktopStorage: Long?,
    installDesktopAwardDaysLeft: Long?,
    installDesktopAwardStorage: Long,
    installDesktopDurationInDays: Int,
    hasMegaPassTrial: Boolean,
    megaPassTrialStorage: Long?,
    megaPassTrialAwardStorage: Long,
    megaPassTrialDurationInDays: Int,
    hasMegaVPNTrial: Boolean,
    megaVPNTrialStorage: Long?,
    megaVPNTrialAwardStorage: Long,
    megaVPNTrialDurationInDays: Int,
    onInviteFriendsClicked: (Long) -> Unit = {},
    onShowInfoAchievementsClicked: (achievementType: AchievementType) -> Unit = {},
    onReferBonusesClicked: () -> Unit = {},
    onMegaVPNFreeTrialClicked: (Boolean, Long, Long, Int) -> Unit = { _, _, _, _ -> },
    onMegaPassFreeTrialClicked: (Boolean, Long, Long, Int) -> Unit = { _, _, _, _ -> },
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(AchievementViewTestTags.TOOLBAR),
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AchievementHeader(
                storageQuota = currentStorage?.toStorageString(LocalContext.current),
                AchievementViewTestTags.HEADER
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {

                    Row(
                        Modifier
                            .testTag(AchievementViewTestTags.INVITE_BONUS_SECTION)
                            .clickable {
                                if (hasReferrals) onReferBonusesClicked() else onInviteFriendsClicked(
                                    currentStorage ?: 0
                                )
                            }
                    ) {
                        AchievementListItem(
                            iconId = R.drawable.ic_referral_bonuses,
                            titleId = R.string.title_referral_bonuses,
                            alphaLevel = if (areAllRewardsExpired) 0.5f else 1.0f,
                            zeroFiguresTitle = if (!hasReferrals) {
                                referralsStorage?.let {
                                    referralsDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_referrals_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_referrals_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (hasReferrals) {
                                referralsAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            buttonTitleId = R.string.contact_invite,
                            onButtonClick = {
                                onInviteFriendsClicked(currentStorage ?: 0)
                            }
                        )
                    }

                    Divider(
                        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                        thickness = 1.dp
                    )

                    Row(
                        Modifier
                            .testTag(AchievementViewTestTags.INSTALL_MOBILE_SECTION)
                            .clickable {
                                onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL)
                            }
                    ) {
                        AchievementListItem(
                            iconId = R.drawable.ic_install_mobile,
                            titleId = R.string.title_install_app,
                            alphaLevel = installAppAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                ?: 1.0f,
                            zeroFiguresTitle = if (installAppAwardStorage <= 0) {
                                installAppStorage?.let {
                                    installAppDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (installAppAwardStorage > 0) {
                                installAppAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            daysLeft = installAppAwardDaysLeft,
                        )
                    }

                    Divider(
                        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                        thickness = 1.dp
                    )

                    if (hasRegistrationAward) {
                        Row(
                            Modifier
                                .testTag(AchievementViewTestTags.REGISTRATION_BONUS_SECTION)
                                .clickable {
                                    onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
                                }
                        ) {
                            AchievementListItem(
                                iconId = R.drawable.ic_registration,
                                titleId = R.string.title_regitration,
                                alphaLevel = registrationAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                    ?: 1.0f,
                                hasFiguresTitle = if (registrationAwardStorage > 0) {
                                    registrationAwardStorage.toUnitString(LocalContext.current)
                                } else null,
                                daysLeft = registrationAwardDaysLeft,
                            )
                        }

                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )
                    }

                    Row(
                        Modifier
                            .testTag(AchievementViewTestTags.INSTALL_DESKTOP_SECTION)
                            .clickable {
                                onShowInfoAchievementsClicked(AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)
                            }
                    ) {
                        AchievementListItem(
                            iconId = R.drawable.ic_install_mega,
                            titleId = R.string.title_install_desktop,
                            alphaLevel = installDesktopAwardDaysLeft?.let { if (it > 0) 1.0f else 0.5f }
                                ?: 1.0f,
                            zeroFiguresTitle = if (installDesktopAwardStorage <= 0) {
                                installDesktopStorage?.let {
                                    installDesktopDurationInDays.getTextByDurationInDays(
                                        context = LocalContext.current,
                                        daysStringId = sharedR.string.figures_storage_achievements_text,
                                        permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                        storage = it.toUnitString(LocalContext.current)
                                    )
                                }
                            } else null,
                            hasFiguresTitle = if (installDesktopAwardStorage > 0) {
                                installDesktopAwardStorage.toUnitString(LocalContext.current)
                            } else null,
                            daysLeft = installDesktopAwardDaysLeft,
                        )
                    }

                    if (hasMegaVPNTrial) {
                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )

                        Row(
                            Modifier
                                .testTag(AchievementViewTestTags.START_MEGA_VPN_FREE_TRIAL_SECTION)
                                .clickable {
                                    Analytics.tracker.trackEvent(StartMEGAVPNFreeTrialEvent)
                                    onMegaVPNFreeTrialClicked(
                                        megaVPNTrialAwardStorage > 0,
                                        megaVPNTrialStorage ?: 0,
                                        megaVPNTrialAwardStorage,
                                        megaVPNTrialDurationInDays
                                    )
                                }
                        ) {
                            AchievementListItem(
                                iconId = iconPackR.drawable.ic_mega_vpn_free_trial,
                                titleId = sharedR.string.title_start_mega_vpn_free_trial,
                                zeroFiguresTitle = if (megaVPNTrialAwardStorage <= 0) {
                                    megaVPNTrialStorage?.let {
                                        megaVPNTrialDurationInDays.getTextByDurationInDays(
                                            context = LocalContext.current,
                                            daysStringId = sharedR.string.figures_storage_achievements_text,
                                            permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                            storage = it.toUnitString(LocalContext.current)
                                        )
                                    }
                                } else null,
                                hasFiguresTitle = if (megaVPNTrialAwardStorage > 0) {
                                    megaVPNTrialAwardStorage.toUnitString(LocalContext.current)
                                } else null,
                                applied = megaVPNTrialAwardStorage > 0
                            )
                        }
                    }

                    if (hasMegaPassTrial) {
                        Divider(
                            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                            thickness = 1.dp
                        )

                        Row(
                            Modifier
                                .testTag(AchievementViewTestTags.START_MEGA_PASS_FREE_TRIAL_SECTION)
                                .clickable {
                                    Analytics.tracker.trackEvent(StartMEGAPWMFreeTrialEvent)
                                    onMegaPassFreeTrialClicked(
                                        megaPassTrialAwardStorage > 0,
                                        megaPassTrialStorage ?: 0,
                                        megaPassTrialAwardStorage,
                                        megaPassTrialDurationInDays
                                    )
                                }
                        ) {
                            AchievementListItem(
                                iconId = iconPackR.drawable.ic_mega_pass_free_trial,
                                titleId = sharedR.string.title_start_mega_pass_free_trial,
                                zeroFiguresTitle = if (megaPassTrialAwardStorage <= 0) {
                                    megaPassTrialStorage?.let {
                                        megaPassTrialDurationInDays.getTextByDurationInDays(
                                            context = LocalContext.current,
                                            daysStringId = sharedR.string.figures_storage_achievements_text,
                                            permanentStringId = sharedR.string.figures_storage_achievements_text_permanent,
                                            storage = it.toUnitString(LocalContext.current)
                                        )
                                    }
                                } else null,
                                hasFiguresTitle = if (megaPassTrialAwardStorage > 0) {
                                    megaPassTrialAwardStorage.toUnitString(LocalContext.current)
                                } else null,
                                applied = megaPassTrialAwardStorage > 0
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Light Theme
 */
@Preview(showBackground = true)
@Composable
fun AchievementPreview() {
    OriginalTheme(false) {
        AchievementView(
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
        )
    }
}

/**
 * Dark Theme
 */
@Preview(showBackground = true)
@Composable
fun AchievementPreviewDark() {
    OriginalTheme(true) {
        AchievementView(
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
            megaPassTrialStorage = 123456789,
            megaPassTrialAwardStorage = 1024 * 1024 * 1024,
            megaVPNTrialStorage = 123456789,
            megaVPNTrialAwardStorage = 1024 * 1024 * 1024,
            referralsDurationInDays = 365,
            installAppDurationInDays = 365,
            installDesktopDurationInDays = 365,
            megaPassTrialDurationInDays = 365,
            megaVPNTrialDurationInDays = 365,
        )
    }
}
