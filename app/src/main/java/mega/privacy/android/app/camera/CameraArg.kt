package mega.privacy.android.app.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Camera arg
 *
 * @property title
 */
@Parcelize
data class CameraArg(val title: String) : Parcelable