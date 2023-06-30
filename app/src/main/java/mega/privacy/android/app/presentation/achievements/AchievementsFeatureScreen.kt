package mega.privacy.android.app.presentation.achievements

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.info.achievementsInfoScreen
import mega.privacy.android.app.presentation.achievements.info.navigateToAchievementsInfo
import mega.privacy.android.app.presentation.achievements.invites.inviteFriendsScreen
import mega.privacy.android.app.presentation.achievements.invites.navigateToInviteFriends
import mega.privacy.android.app.presentation.achievements.referral.navigateToReferralBonus
import mega.privacy.android.app.presentation.achievements.referral.referralBonusScreen
import mega.privacy.android.core.ui.controls.appbar.SimpleTopAppBar
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
    val scrollState = rememberScrollState()
    val navHostController = rememberNavController()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var toolbarTitleResId by remember {
        mutableStateOf(R.string.achievements_title)
    }

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
        },
        topBar = {
            SimpleTopAppBar(
                titleId = toolbarTitleResId,
                elevation = scrollState.value > 0,
                onBackPressed = { onBackPressedDispatcher?.onBackPressed() }
            )
        },
    ) { padding ->
        AchievementsNavHostController(
            modifier = Modifier
                .padding(padding),
            navHostController = navHostController,
            onSetToolbarTitle = { toolbarTitleResId = it }
        )
    }
}

@Composable
internal fun AchievementsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
    onSetToolbarTitle: (Int) -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = achievementsRoute
    ) {
        achievementScreen(
            onSetToolbarTitle = onSetToolbarTitle,
            onNavigateToInfoAchievements = navHostController::navigateToAchievementsInfo,
            onNavigateToInviteFriends = navHostController::navigateToInviteFriends,
            onNavigateToReferralBonuses = navHostController::navigateToReferralBonus
        )
        achievementsInfoScreen(
            onSetToolbarTitle = onSetToolbarTitle
        )
        referralBonusScreen(
            onSetToolbarTitle = onSetToolbarTitle
        )
        inviteFriendsScreen(
            onSetToolbarTitle = onSetToolbarTitle
        )
    }
}
