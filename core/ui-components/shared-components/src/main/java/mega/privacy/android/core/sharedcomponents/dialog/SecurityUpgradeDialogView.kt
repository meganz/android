package mega.privacy.android.core.sharedcomponents.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.sharedcomponents.R
import mega.privacy.android.icon.pack.IconPack

/**
 * Material 3 version of Security upgrade dialog
 *
 * @param titleText      : Dialog title text
 * @param contentText    : Dialog content text
 * @param okButtonText   : Ok button text
 * @param imageVector    : Security upgrade icon image vector
 * @param onOkClick      : Ok button click listener
 * @param onCloseClick   : Close icon click listener
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityUpgradeDialogView(
    titleText: String,
    contentText: String,
    okButtonText: String,
    imageVector: ImageVector,
    onOkClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onCloseClick) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(28.dp),
            color = DSTokens.colors.background.surface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(LocalSpacing.current.x24),
            ) {
                MegaIcon(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onCloseClick),
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    contentDescription = "CloseIcon",
                    tint = IconColor.Primary
                )

                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(140.dp)
                        .width(114.dp)
                        .testTag("HeaderImage"),
                    imageVector = imageVector,
                    contentDescription = "Security Upgrade Icon"
                )

                MegaText(
                    text = titleText,
                    style = AppTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                MegaText(
                    text = contentText,
                    style = AppTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    textColor = TextColor.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth()
                        .padding(start = 25.dp, end = 25.dp),
                    shape = RoundedCornerShape(8.dp),
                    content = {
                        Text(
                            text = okButtonText,
                            color = Color.White
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC)),
                    onClick = onOkClick
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SecurityUpgradeDialogViewM3Preview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SecurityUpgradeDialogView(
            titleText = "Account security upgrade",
            contentText = "We are upgrading the security of your account. You should only see this message once. If you have seen this before, first make sure that it was for this account and not for another MEGA account that you have.\n\nIf you are sure and this is the second time you are seeing this message for this account, stop using this account.",
            okButtonText = "OK",
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_security_upgrade),
            onOkClick = {},
            onCloseClick = {},
        )
    }
}

