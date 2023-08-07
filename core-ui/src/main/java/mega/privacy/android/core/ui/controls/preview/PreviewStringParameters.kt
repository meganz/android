package mega.privacy.android.core.ui.controls.preview

import mega.privacy.android.core.ui.preview.PreviewTextValue

internal data class PreviewStringParameters(
    val text: PreviewTextValue,
    val confirmButtonText: PreviewTextValue,
    val title: PreviewTextValue? = null,
    val cancelButtonText: PreviewTextValue? = null,
)