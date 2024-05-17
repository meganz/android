package mega.privacy.android.shared.original.core.ui.controls.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.shared.original.core.ui.preview.PreviewTextValue

internal open class PreviewAlertDialogParametersProvider :
    PreviewParameterProvider<PreviewStringParameters> {
    override val values: Sequence<PreviewStringParameters>
        get() = sequenceOf(
            PreviewStringParameters(
                text = PreviewTextValue("Discard draft?"),
                confirmButtonText = PreviewTextValue("Discard"),
                cancelButtonText = PreviewTextValue("Cancel"),
            )
        )
}