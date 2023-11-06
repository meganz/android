package mega.privacy.android.core.ui.controls.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.PreviewTextValue

internal class PreviewAlertDialogParametersProvider :
    PreviewParameterProvider<PreviewStringParameters> {
    override val values: Sequence<PreviewStringParameters>
        get() = sequenceOf(
            PreviewStringParameters(
                text = PreviewTextValue(R.string.dialog_text),
                confirmButtonText = PreviewTextValue(R.string.discard),
                cancelButtonText = PreviewTextValue(R.string.cancel),
            ),
            PreviewStringParameters(
                text = PreviewTextValue(R.string.dialog_text),
                confirmButtonText = PreviewTextValue(R.string.action_long),
                cancelButtonText = PreviewTextValue(R.string.cancel_long),
            ),
            PreviewStringParameters(
                title = PreviewTextValue(R.string.dialog_title),
                text = PreviewTextValue(R.string.dialog_text_long),
                confirmButtonText = PreviewTextValue(R.string.discard),
                cancelButtonText = PreviewTextValue(R.string.cancel),
            )
        )
}