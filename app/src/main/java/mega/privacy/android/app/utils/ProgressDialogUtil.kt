package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.R
import mega.privacy.android.app.components.MegaProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString

/**
 * Provides utility for Progress Dialog
 */
object ProgressDialogUtil {

    @JvmField
    var shouldShowDialog = false

    private var isPl = false

    /**
     * Show process dialog when processing files, such as selecting files from device
     *
     * @param context – The parent context
     * @param intent - A description of an operation to be performed
     */
    @JvmStatic
    fun showProcessFileDialog(context: Context, intent: Intent?): MegaProgressDialog {
        val dialog = MegaProgressDialog(context)

        dialog.let {
            it.setCancelable(false)
            it.setCanceledOnTouchOutside(false)
        }

        if (intent != null) {
            val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            isPl = imageUris != null && imageUris.size > 1
            if (!isPl) {
                val clipData = intent.clipData
                isPl = clipData != null && clipData.itemCount > 1
            }
        }

        dialog.let {
            it.setMessage(getQuantityString(R.plurals.upload_prepare, if (isPl) 2 else 1))
            shouldShowDialog = true
            it.show()
        }
        return dialog
    }

    /**
     * Dismiss this dialog, removing it from the screen.
     *
     * @param dialog - The dialog instance for dismissing
     */
    @JvmStatic
    fun dismissDialog(dialog: MegaProgressDialog?) {
        shouldShowDialog = false
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    /**
     * Create a instance of MegaProgressDialog
     *
     * @param context – The parent context
     * @param message - The message to display in the dialog.
     */
    @JvmStatic
    fun getMegaProgressDialog(context: Context?, message: String): MegaProgressDialog? {
        var temp: MegaProgressDialog? = null

        try {
            temp = MegaProgressDialog(context)
            temp.setMessage(message)
            temp.show()
        } catch (e: Exception) {
            LogUtil.logWarning("Exception creating progress dialog: $message", e)
        }

        return temp
    }
}