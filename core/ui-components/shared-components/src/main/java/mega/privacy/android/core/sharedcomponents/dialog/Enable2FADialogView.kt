package mega.privacy.android.core.sharedcomponents.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Material 3 version of Enable 2FA dialog
 *
 * @param titleText Title text for the dialog
 * @param descriptionText Description text for the dialog
 * @param imageRes Drawable resource ID for the 2FA icon
 * @param enableButtonText Text for the enable button
 * @param skipButtonText Text for the skip button
 * @param onDismissRequest Callback when dialog is dismissed
 * @param onEnable2FA Callback when enable button is clicked
 * @param modifier Modifier for the dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Enable2FADialogView(
    titleText: String,
    descriptionText: String,
    @DrawableRes imageRes: Int,
    enableButtonText: String,
    skipButtonText: String,
    onDismissRequest: () -> Unit,
    onEnable2FA: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = DSTokens.colors.background.surface1
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MegaText(
                    text = titleText,
                    style = AppTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Image(
                    modifier = Modifier.padding(top = 8.dp),
                    painter = painterResource(id = imageRes),
                    contentDescription = "Icon 2 FA"
                )
                MegaText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = descriptionText,
                    style = AppTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    textAlign = TextAlign.Center,
                )

                PrimaryFilledButton(
                    modifier = Modifier.padding(top = 32.dp),
                    text = enableButtonText,
                    onClick = onEnable2FA
                )
                TextButton(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = skipButtonText,
                        style = AppTheme.typography.labelLarge,
                        color = DSTokens.colors.text.accent
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun Enable2FADialogViewPreview() {
    AndroidThemeForPreviews {
        Enable2FADialogView(
            titleText = "Enable 2FA",
            descriptionText = "Two-factor authentication adds an extra layer of security to your account.",
            imageRes = android.R.drawable.ic_dialog_info,
            enableButtonText = "Enable",
            skipButtonText = "Skip",
            onDismissRequest = { },
            onEnable2FA = { },
        )
    }
}
