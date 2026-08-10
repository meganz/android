package mega.privacy.android.app.presentation.imagepreview

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Initial destination for [ImagePreviewActivity]'s navigation scaffold. The image viewer hosts a
 * nav3 back stack so it can navigate to other destinations (e.g. the video editor) in-place.
 */
@Serializable
internal data object ImagePreviewNavKey : NavKey
