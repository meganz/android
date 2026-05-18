package mega.privacy.android.app.modalbottomsheet

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Util object for modal bottom sheets.
 */
object ModalBottomSheetUtil {

    /**
     * Checks if a bottom sheet dialog fragment is shown.
     *
     * @return True if the bottom sheet is shown, false otherwise.
     */
    @JvmStatic
    fun BottomSheetDialogFragment?.isBottomSheetDialogShown(): Boolean =
        this?.isAdded == true
}