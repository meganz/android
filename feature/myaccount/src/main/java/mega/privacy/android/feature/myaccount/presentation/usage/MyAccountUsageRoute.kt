package mega.privacy.android.feature.myaccount.presentation.usage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.myaccount.presentation.usage.view.MyAccountUsageScreen

/**
 * Stateful entry point for the account usage screen. This is the only account-usage API this module
 * exposes; the screen, its state and its ViewModel stay internal.
 *
 * @param onUpgradeClick Called when the user taps the upgrade button.
 * @param onUsageLoadErrorDismiss Called after the user acknowledges a load failure; the caller is
 * expected to leave the screen, since there is no retry.
 * @param modifier Modifier for the screen.
 */
@Composable
fun MyAccountUsageRoute(
    onUpgradeClick: () -> Unit,
    onUsageLoadErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MyAccountUsageComposeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyAccountUsageScreen(
        uiState = uiState,
        modifier = modifier,
        onUpgradeClick = onUpgradeClick,
        onUsageLoadErrorDismiss = onUsageLoadErrorDismiss,
    )
}
