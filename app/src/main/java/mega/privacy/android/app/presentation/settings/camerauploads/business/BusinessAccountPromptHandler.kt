package mega.privacy.android.app.presentation.settings.camerauploads.business

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.presentation.account.business.BusinessAccountSuspendedDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CameraUploadsBusinessAccountDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] to handle what kind of Business Account Dialog should be displayed, depending on
 * the [businessAccountPromptType] passed
 *
 * @param businessAccountPromptType The specific Business Account prompt denoted by [EnableCameraUploadsStatus].
 * It is null if the User is not on any type of Business Account
 * @param onBusinessAccountPromptDismissed Lambda to execute when the User dismisses the Business
 * Account prompt
 * @param onRegularBusinessAccountSubUserPromptAcknowledged Lambda to execute when the Business
 * Account Sub-User acknowledges that the Business Account Administrator can access the content
 * in Camera Uploads
 */
@Composable
internal fun BusinessAccountPromptHandler(
    businessAccountPromptType: EnableCameraUploadsStatus?,
    onBusinessAccountPromptDismissed: () -> Unit,
    onRegularBusinessAccountSubUserPromptAcknowledged: () -> Unit,
) {
    businessAccountPromptType?.let { cameraUploadsStatus ->
        when (cameraUploadsStatus) {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> Unit
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                CameraUploadsBusinessAccountDialog(
                    onAlertAcknowledged = onRegularBusinessAccountSubUserPromptAcknowledged,
                    onAlertDismissed = onBusinessAccountPromptDismissed,
                )
            }

            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
            EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
            -> {
                BusinessAccountSuspendedDialog(
                    isBusinessAdministratorAccount = cameraUploadsStatus == EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
                    onAlertAcknowledged = onBusinessAccountPromptDismissed,
                    onAlertDismissed = onBusinessAccountPromptDismissed,
                )
            }
        }
    }
}

/**
 * A Preview [Composable] for [BusinessAccountPromptHandler]
 */
@CombinedThemePreviews
@Composable
private fun BusinessAccountPromptHandlerPreview(
    @PreviewParameter(BusinessAccountPromptHandlerParameterProvider::class) businessAccountPromptType: EnableCameraUploadsStatus?,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        BusinessAccountPromptHandler(
            businessAccountPromptType = businessAccountPromptType,
            onBusinessAccountPromptDismissed = {},
            onRegularBusinessAccountSubUserPromptAcknowledged = {},
        )
    }
}

private class BusinessAccountPromptHandlerParameterProvider :
    PreviewParameterProvider<EnableCameraUploadsStatus?> {
    override val values: Sequence<EnableCameraUploadsStatus?>
        get() = sequenceOf(
            null,
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT,
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
            EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
        )
}