package mega.privacy.android.app.camera.state

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Camera Selector.
 *
 * @param selector internal camera selector from CameraX
 * @see CameraSelector
 * */
enum class CamSelector(
    internal val selector: CameraSelector,
) {
    Front(CameraSelector.DEFAULT_FRONT_CAMERA),
    Back(CameraSelector.DEFAULT_BACK_CAMERA);

    /**
     * Inverse camera selector. Works only with default Front & Back Selector.
     * */
    val inverse: CamSelector
        get() = when (this) {
            Front -> Back
            Back -> Front
        }

    internal companion object {

        @SuppressLint("RestrictedApi")
        internal val Saver: Saver<MutableState<CamSelector>, *> = listSaver(
            save = { listOf(it.value.selector.lensFacing) },
            restore = {
                mutableStateOf(
                    when (it[0]) {
                        CameraSelector.LENS_FACING_FRONT -> Front
                        else -> Back
                    }
                )
            }
        )
    }
}
