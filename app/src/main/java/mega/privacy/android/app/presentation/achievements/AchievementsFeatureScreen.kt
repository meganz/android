package mega.privacy.android.app.presentation.achievements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.achievements.info.achievementsInfoScreen
import mega.privacy.android.app.presentation.achievements.info.navigateToAchievementsInfo
import mega.privacy.android.app.presentation.achievements.invites.inviteFriendsScreen
import mega.privacy.android.app.presentation.achievements.invites.navigateToInviteFriends
import mega.privacy.android.app.presentation.achievements.referral.navigateToReferralBonus
import mega.privacy.android.app.presentation.achievements.referral.referralBonusScreen
import mega.privacy.android.core.ui.theme.extensions.black_white

/**
 * Scaffold for the Achievements Flow Screen
 */
@Composable
fun AchievementsFeatureScreen(
    viewModel: AchievementsOverviewViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val navHostController = rememberNavController()

    EventEffect(uiState.errorMessage, viewModel::resetErrorState) {
        snackbarHostState.showSnackbar(context.resources.getString(it))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier,
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.black_white
                )
            }
        }
    ) { padding ->
        AchievementsNavHostController(
            modifier = Modifier
                .padding(padding),
            navHostController = navHostController,
        )
    }
}

@Composable
internal fun AchievementsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = achievementsRoute
    ) {
        achievementScreen(
            onNavigateToInfoAchievements = navHostController::navigateToAchievementsInfo,
            onNavigateToInviteFriends = navHostController::navigateToInviteFriends,
            onNavigateToReferralBonuses = navHostController::navigateToReferralBonus
        )
        achievementsInfoScreen()
        referralBonusScreen()
        inviteFriendsScreen()
    }
}
