package mega.privacy.android.core.ui.controls.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.core.ui.controls.preview.PreviewStringParameters
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Test Tags for the Confirmation Dialog with Illustration
 */
internal const val CONFIRMATION_DIALOG_WITH_ILLUSTRATION_TITLE =
    "confirmation_dialog_with_illustration:text_dialog_title"
internal const val CONFIRMATION_DIALOG_WITH_ILLUSTRATION_IMAGE =
    "confirmation_dialog_with_illustration:image_dialog_illustration"
internal const val CONFIRMATION_DIALOG_WITH_ILLUSTRATION_BODY =
    "confirmation_dialog_with_illustration:text_dialog_body"

internal val DrawableResId = SemanticsPropertyKey<Int>("DrawableResId")
internal var SemanticsPropertyReceiver.drawableId by DrawableResId

/**
 * A Composable variation of the [AlertDialog] that displays an [Image] between the Title and Body
 *
 * @param title The Alert Dialog Title
 * @param illustrationId The [DrawableRes] Image to be displayed. This ID should reference a Vector
 * Drawable
 * @param body The Alert Dialog Body
 * @param confirmButtonText The Text used to perform a specific action
 * @param cancelButtonText The Text used to cancel the Alert Dialog. The [TextMegaButton] is not
 * shown when the Text is empty
 * @param onDismiss Lambda that is invoked when the Alert Dialog is dismissed
 * @param onConfirm Lambda that is invoked when the [TextMegaButton] used to perform a specific
 * action is clicked
 * @param modifier The default [Modifier]
 * @param onDismissWhenClickedOutside Allows or disallows the Alert Dialog to be dismissed when a
 * click event is performed outside the Alert Dialog bounds
 * @param onDismissWhenBackPressed Allows or disallows the Alert Dialog to be dismissed when a Back
 * Press event is detected
 * @param onCancel Lambda that is invoked when the [TextMegaButton] used to cancel an action is
 * clicked
 */
@Composable
fun ConfirmationDialogWithIllustration(
    title: String,
    @DrawableRes illustrationId: Int,
    body: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissWhenClickedOutside: Boolean = true,
    onDismissWhenBackPressed: Boolean = true,
    onCancel: () -> Unit = onDismiss,
) = BaseMegaAlertDialog(
    content = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                modifier = Modifier
                    .testTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_TITLE)
                    .fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
            )
            Image(
                imageVector = ImageVector.vectorResource(illustrationId),
                contentDescription = "Dialog Illustration",
                modifier = Modifier
                    .semantics { drawableId = illustrationId }
                    .testTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_IMAGE)
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                modifier = Modifier.testTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_BODY),
                text = body,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorSecondary,
            )
        }
    },
    confirmButtonText = confirmButtonText,
    cancelButtonText = cancelButtonText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    onCancel = onCancel,
    modifier = modifier,
    dismissOnClickOutside = onDismissWhenClickedOutside,
    dismissOnBackPress = onDismissWhenBackPressed,
)

/**
 * A Preview Composable that displays the [ConfirmationDialogWithIllustration]
 */
@CombinedThemeRtlPreviews
@Composable
private fun ConfirmationDialogWithIllustrationPreview(
    @PreviewParameter(PreviewStringsParametersProviderWithTitle::class) texts: PreviewStringParameters,
) {
    AndroidTheme {
        DialogBox {
            ConfirmationDialogWithIllustration(
                title = texts.title?.getText() ?: "",
                illustrationId = R.drawable.ic_sync,
                body = texts.text.getText(),
                confirmButtonText = texts.confirmButtonText.getText(),
                cancelButtonText = texts.cancelButtonText?.getText(),
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

/**
 * A Composable used by Alert Dialog Previews to simulate the boundaries of the Alert Dialog
 */
@Composable
private fun DialogBox(content: @Composable BoxScope.() -> Unit) = Box(
    modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
    content = content
)