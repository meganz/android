package mega.privacy.android.app.presentation.qrcode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white

/**
 * App bar for QR code screen
 */
@Composable
internal fun QRCodeTopBar(
    isQRCodeAvailable: Boolean,
    showMoreMenu: Boolean,
    onShowMoreClicked: () -> Unit,
    onMenuDismissed: () -> Unit,
    onSave: () -> Unit,
    onGotoSettings: () -> Unit,
    onResetQRCode: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.section_qr_code),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = stringResource(id = R.string.general_back_button),
                    tint = MaterialTheme.colors.black_white
                )
            }
        },
        actions = {
            if (isQRCodeAvailable) {
                IconButton(
                    modifier = Modifier.testTag(SHARE_TAG),
                    onClick = onShare
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_social_share_white),
                        contentDescription = stringResource(id = R.string.general_share),
                        tint = MaterialTheme.colors.black_white
                    )
                }

                IconButton(
                    modifier = Modifier.testTag(MORE_TAG),
                    onClick = onShowMoreClicked
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = stringResource(id = R.string.label_more),
                        tint = MaterialTheme.colors.black_white,
                    )
                }

                DropdownMenu(
                    modifier = Modifier.testTag(DROPDOWN_TAG),
                    expanded = showMoreMenu,
                    onDismissRequest = onMenuDismissed,
                ) {
                    DropdownMenuItem(onClick = onSave) {
                        Text(text = stringResource(id = R.string.save_action))
                    }
                    DropdownMenuItem(onClick = onGotoSettings) {
                        Text(text = stringResource(id = R.string.action_settings))
                    }
                    DropdownMenuItem(onClick = onResetQRCode) {
                        Text(text = stringResource(id = R.string.action_reset_qr))
                    }
                    DropdownMenuItem(onClick = onDeleteQRCode) {
                        Text(text = stringResource(id = R.string.action_delete_qr))
                    }
                }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewQRCodeTopBar(
    @PreviewParameter(BooleanProvider::class) qrCodeAvailable: Boolean
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        QRCodeTopBar(
            isQRCodeAvailable = qrCodeAvailable,
            showMoreMenu = false,
            onShowMoreClicked = { },
            onMenuDismissed = { },
            onSave = { },
            onGotoSettings = { },
            onResetQRCode = { },
            onDeleteQRCode = { },
            onBackPressed = { },
            onShare = { }
        )
    }
}

internal const val SHARE_TAG = "qr_code_top_bar:icon_share"
internal const val MORE_TAG = "qr_code_top_bar:icon_more"
internal const val DROPDOWN_TAG = "qr_code_top_bar:menu_dropdown"