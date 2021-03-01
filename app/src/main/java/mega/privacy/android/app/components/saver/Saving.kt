package mega.privacy.android.app.components.saver

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.SDCardOperator

abstract class Saving : Parcelable {
    var unsupportedFileName = ""
        protected set

    /**
     * Get the total size of this saving.
     *
     * @return total size
     */
    abstract fun totalSize(): Long

    /**
     * Check if there is any unsupported file in this Saving.
     *
     * @param context Android context
     */
    abstract fun hasUnsupportedFile(context: Context): Boolean

    abstract fun fromMediaViewer(): Boolean

    /**
     * The final step to download a node into a file.
     *
     * @param parentPath the parent path where the file should be inside
     * @param externalSDCard whether it's download into external sdcard
     * @param sdCardOperator SDCardOperator used when download to external sdcard,
     * will be null if download to internal storage
     * @param snackbarShower interface to show snackbar
     * @return info about auto play
     */
    abstract fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower
    ): AutoPlayInfo

    companion object {
        val NOTHING = object : Saving() {
            override fun totalSize() = 0L

            override fun hasUnsupportedFile(context: Context): Boolean = false

            override fun fromMediaViewer() = false

            override fun doDownload(
                parentPath: String,
                externalSDCard: Boolean,
                sdCardOperator: SDCardOperator?,
                snackbarShower: SnackbarShower
            ) = AutoPlayInfo.NO_AUTO_PLAY

            override fun describeContents(): Int {
                return 0
            }

            override fun writeToParcel(dest: Parcel?, flags: Int) {
            }
        }
    }
}
