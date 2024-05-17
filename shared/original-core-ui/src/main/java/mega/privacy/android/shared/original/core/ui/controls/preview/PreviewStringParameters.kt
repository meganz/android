package mega.privacy.android.shared.original.core.ui.controls.preview

import mega.privacy.android.shared.original.core.ui.preview.PreviewTextValue

internal data class PreviewStringParameters(
    val text: PreviewTextValue,
    val confirmButtonText: PreviewTextValue,
    val title: PreviewTextValue? = null,
    val cancelButtonText: PreviewTextValue? = null,
)