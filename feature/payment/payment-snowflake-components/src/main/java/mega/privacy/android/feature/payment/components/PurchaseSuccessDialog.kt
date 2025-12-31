package mega.privacy.android.feature.payment.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.DialogButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog to show purchase success result
 *
 * @param accountName The name of the account type (e.g., "Pro I", "Pro Lite")
 * @param accountImage The drawable resource ID for the account type icon
 * @param message The success message to display
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSuccessDialog(
    accountName: String,
    @DrawableRes accountImage: Int,
    message: String,
    onDismiss: () -> Unit,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        BasicAlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DSTokens.colors.background.surface1
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    MegaText(
                        text = accountName,
                        style = AppTheme.typography.headlineSmall,
                        textColor = TextColor.Primary,
                    )

                    Image(
                        painter = painterResource(id = accountImage),
                        contentDescription = accountName,
                        modifier = Modifier.padding(top = 24.dp)
                    )

                    MegaText(
                        text = message,
                        style = AppTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.padding(top = 36.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        DialogButton(
                            buttonText = stringResource(id = sharedR.string.general_ok),
                            onButtonClicked = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PurchaseSuccessDialogPreview() {
    AndroidThemeForPreviews {
        PurchaseSuccessDialog(
            accountName = "Pro I",
            accountImage = android.R.drawable.ic_dialog_info,
            message = "Thank you for subscribing to Pro I monthly!",
            onDismiss = {}
        )
    }
}

