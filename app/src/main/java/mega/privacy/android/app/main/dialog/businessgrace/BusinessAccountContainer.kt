package mega.privacy.android.app.main.dialog.businessgrace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.shared.resources.R

/**
 * Business account container
 *
 */
@Composable
fun BusinessAccountContainer(
    viewModel: BusinessAccountViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    var showUnverifiedBusinessAccountDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.unverifiedBusinessAccountState.collect { unverifiedBusinessAccount ->
            if (unverifiedBusinessAccount) {
                showUnverifiedBusinessAccountDialog = true
            }
        }
    }
    content()
    if (showUnverifiedBusinessAccountDialog) {
        BasicDialog(
            title = stringResource(R.string.unverified_business_account_dialog_title),
            description = stringResource(R.string.unverified_business_account_dialog_description),
            positiveButtonText = stringResource(mega.privacy.android.app.R.string.general_close),
            onPositiveButtonClicked = {
                showUnverifiedBusinessAccountDialog = false
            },
            onDismiss = {
                showUnverifiedBusinessAccountDialog = false
            }
        )
    }
}