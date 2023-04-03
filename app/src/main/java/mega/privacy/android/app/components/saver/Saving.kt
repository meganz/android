package mega.privacy.android.app.components.saver

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.SDCardOperator
import nz.mega.sdk.MegaApiAndroid

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
     * @param megaApi MegaApi instance
     * @param megaApiFolder MegaApi instance for folder link
     * @param parentPath the parent path where the file should be inside
     * @param externalSDCard whether it's download into external sdcard
     * @param sdCardOperator SDCardOperator used when download to external sdcard,
     * will be null if download to internal storage
     * @param snackbarShower Valid interface to show snackbar, null if no snackbar is needed.
     * @return info about auto play
     */
    abstract fun doDownload(
        megaApi: MegaApiAndroid,
        megaApiFolder: MegaApiAndroid,
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower?,
    ): AutoPlayInfo


    abstract fun isDownloadForPreview(): Boolean

    companion object {
        @Parcelize
        object NOTHING : Saving() {
            override fun totalSize() = 0L

            override fun hasUnsupportedFile(context: Context): Boolean = false

            override fun fromMediaViewer() = false

            override fun doDownload(
                megaApi: MegaApiAndroid,
                megaApiFolder: MegaApiAndroid,
                parentPath: String,
                externalSDCard: Boolean,
                sdCardOperator: SDCardOperator?,
                snackbarShower: SnackbarShower?,
            ) = AutoPlayInfo.NO_AUTO_PLAY

            override fun isDownloadForPreview(): Boolean = false
        }
    }
}
