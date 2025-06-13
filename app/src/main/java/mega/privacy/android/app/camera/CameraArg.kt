package mega.privacy.android.app.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Camera arg
 *
 * @property title title of the preview screen
 * @property buttonText text of the button in the preview screen
 */
@Parcelize
data class CameraArg(
    val title: String,
    val buttonText: String
) : Parcelable