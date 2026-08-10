package mega.privacy.android.app.presentation.business

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.DialogButton
import mega.android.core.ui.components.dialogs.MegaDialog
import mega.android.core.ui.components.dialogs.MegaDialogBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Business Grace Dialog View
 *
 * @param onDismissRequest Lambda to execute when the dialog is dismissed
 * @param modifier Modifier for the dialog
 */
@Composable
fun BusinessGraceDialogView(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaDialog(
        dialogBackground = MegaDialogBackground.Surface1,
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MegaText(
                text = stringResource(id = R.string.general_something_went_wrong_error),
                textColor = TextColor.Primary,
                style = AppTheme.typography.headlineSmall,
            )
            Image(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .size(100.dp),
                painter = painterResource(id = R.drawable.ic_account_expired),
                contentDescription = "Image Account Expired",
            )
            MegaText(
                modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
                text = stringResource(id = R.string.grace_period_admin_alert),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8)
            ) {
                DialogButton(
                    buttonText = stringResource(sharedR.string.general_dismiss_dialog),
                    onButtonClicked = onDismissRequest,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun BusinessGraceDialogViewPreview() {
    AndroidThemeForPreviews {
        BusinessGraceDialogView(
            onDismissRequest = {},
        )
    }
}
