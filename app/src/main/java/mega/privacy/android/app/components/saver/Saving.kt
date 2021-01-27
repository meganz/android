package mega.privacy.android.app.components.saver

import android.content.Context
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.SDCardOperator

abstract class Saving(val totalSize: Long) {
    var unsupportedFileName = ""
        protected set

    /**
     * Check if there is any unsupported file in this Saving.
     *
     * @param context Android context
     */
    abstract fun hasUnsupportedFile(context: Context): Boolean

    /**
     * The final step to download a node into a file.
     *
     * @param parentPath the parent path where the file should be inside
     * @param externalSDCard whether it's download into external sdcard
     * @param sdCardOperator SDCardOperator used when download to external sdcard,
     * will be null if download to internal storage
     * @param snackbarShower interface to show snackbar
     */
    abstract fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower
    )

    companion object {
        val NOTHING = object : Saving(0) {
            override fun hasUnsupportedFile(context: Context): Boolean = false

            override fun doDownload(
                parentPath: String,
                externalSDCard: Boolean,
                sdCardOperator: SDCardOperator?,
                snackbarShower: SnackbarShower
            ) {
            }
        }
    }
}
