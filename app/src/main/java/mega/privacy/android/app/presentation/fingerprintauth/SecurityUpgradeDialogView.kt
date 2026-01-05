package mega.privacy.android.app.presentation.fingerprintauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.core.sharedcomponents.dialog.SecurityUpgradeDialogView as SecurityUpgradeDialogViewBase
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Security upgrade dialog with ViewModel integration
 *
 * @param viewModel ViewModel for handling security upgrade logic
 * @param onDismiss Callback when dialog should be dismissed
 */
@Composable
fun SecurityUpgradeDialogView(
    viewModel: SecurityUpgradeViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldFinishScreen) {
        if (uiState.shouldFinishScreen) {
            onDismiss()
        }
    }

    SecurityUpgradeDialogViewBase(
        titleText = stringResource(id = R.string.shared_items_security_upgrade_dialog_title),
        contentText = stringResource(id = R.string.shared_items_security_upgrade_dialog_content),
        okButtonText = stringResource(id = sharedResR.string.general_ok),
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_security_upgrade),
        onOkClick = {
            viewModel.upgradeAccountSecurity()
        },
        onCloseClick = onDismiss,
    )
}
