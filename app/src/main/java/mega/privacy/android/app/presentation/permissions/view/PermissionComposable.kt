package mega.privacy.android.app.presentation.permissions.view

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R

@Composable
fun CameraBackupPermissionsScreen(
    modifier: Modifier = Modifier,
    onEnablePermission: () -> Unit,
    onSkipPermission: () -> Unit,
) {
    NewPermissionsScreen(
        attributes = PermissionAttributes(
            title = stringResource(R.string.camera_backup_permission_screen_title),
            description = stringResource(R.string.camera_backup_permission_screen_description),
            bannerText = SpannableText(
                text = stringResource(R.string.camera_backup_permission_banner_info_description),
                annotations = mapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        megaSpanStyle = MegaSpanStyle.DefaultColorStyle(
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                        ),
                        annotation = "A"
                    )
                )
            ),
            image = painterResource(id = IconPackR.drawable.illustration_camera_backup_permission),
            primaryButton = stringResource(R.string.camera_backup_permission_enable_button_text) to onEnablePermission,
            secondaryButton = stringResource(R.string.permission_screen_skip_permission_request_button_text) to onSkipPermission,
        ),
        modifier = modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun CameraBackupPermissionsScreenPreview() {
    AndroidThemeForPreviews {
        CameraBackupPermissionsScreen(
            modifier = Modifier,
            onEnablePermission = {},
            onSkipPermission = {},
        )
    }
}