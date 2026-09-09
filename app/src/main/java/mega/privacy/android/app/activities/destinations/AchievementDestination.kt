package mega.privacy.android.app.activities.destinations

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.myaccount.presentation.achievements.AchievementsOverviewViewModel
import mega.privacy.android.feature.myaccount.presentation.achievements.view.AchievementsOverviewView
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.AchievementsInfoNavKey
import mega.privacy.android.navigation.destination.InviteFriendsNavKey
import mega.privacy.android.navigation.destination.MegaPassFreeTrialNavKey
import mega.privacy.android.navigation.destination.MegaVpnFreeTrialNavKey
import mega.privacy.android.navigation.destination.ReferralBonusNavKey

/**
 * Registers the [AchievementNavKey] destination. Behind [ApiFeatures.ContactComposeFeature] either
 * renders the migrated Compose achievements overview (flag on) — routing each row to its sub-screen
 * NavKey — or launches the legacy [AchievementsFeatureActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.achievement(navigationHandler: NavigationHandler) {
    entry<AchievementNavKey> {
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    context.startActivity(Intent(context, AchievementsFeatureActivity::class.java))
                    navigationHandler.back()
                }
            },
            enabled = {
                val viewModel = hiltViewModel<AchievementsOverviewViewModel>()
                val uiState by viewModel.state.collectAsStateWithLifecycle()

                AchievementsOverviewView(
                    uiState = uiState,
                    onInviteFriendsClicked = { grantStorageInBytes ->
                        navigationHandler.navigate(
                            InviteFriendsNavKey(grantStorageInBytes = grantStorageInBytes)
                        )
                    },
                    onShowInfoAchievementsClicked = { achievementType ->
                        navigationHandler.navigate(
                            AchievementsInfoNavKey(achievementType = achievementType)
                        )
                    },
                    onReferBonusesClicked = {
                        navigationHandler.navigate(ReferralBonusNavKey)
                    },
                    onMegaVPNFreeTrialClicked = { isReceivedAward, storageAmount, awardStorageAmount, durationInDays, awardDaysLeft ->
                        navigationHandler.navigate(
                            MegaVpnFreeTrialNavKey(
                                isReceivedAward = isReceivedAward,
                                storageAmount = storageAmount,
                                awardStorageAmount = awardStorageAmount,
                                durationInDays = durationInDays,
                                awardDaysLeft = awardDaysLeft,
                            )
                        )
                    },
                    onMegaPassFreeTrialClicked = { isReceivedAward, storageAmount, awardStorageAmount, durationInDays, awardDaysLeft ->
                        navigationHandler.navigate(
                            MegaPassFreeTrialNavKey(
                                isReceivedAward = isReceivedAward,
                                storageAmount = storageAmount,
                                awardStorageAmount = awardStorageAmount,
                                durationInDays = durationInDays,
                                awardDaysLeft = awardDaysLeft,
                            )
                        )
                    },
                )
            },
        )
    }
}
