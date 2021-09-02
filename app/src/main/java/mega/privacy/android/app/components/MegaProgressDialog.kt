package mega.privacy.android.app.components

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.TextView
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString

/**
 * A subclass of AlertDialog that can display the circle progress and message.
 *
 * @param context   The parent context.
 */
class MegaProgressDialog(context: Context?) : AlertDialog(context) {

    private var txtMessage: TextView? = null

    init {
        val inflater = LayoutInflater.from(context).inflate(R.layout.progress_bar, null);
        setView(inflater)
        txtMessage = inflater.findViewById(R.id.progress_msg)
    }

    override fun setMessage(message: CharSequence?) {
        txtMessage?.text = message
    }

    companion object {
        /**
         * Shows a process dialog informing is processing files, such as selecting files from device.
         *
         * @param context   The parent context.
         * @param intent    A description of an operation to be performed.
         * @return The created MegaProgressDialog.
         */
        @JvmStatic
        fun showProcessFileDialog(context: Context, intent: Intent?): MegaProgressDialog {
            var isPlural = false

            if (intent != null) {
                val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                isPlural = imageUris != null && imageUris.size > 1

                if (!isPlural) {
                    val clipData = intent.clipData
                    isPlural = clipData != null && clipData.itemCount > 1
                }
            }

            val dialog = MegaProgressDialog(context)

            dialog.apply {
                setMessage(getQuantityString(R.plurals.upload_prepare, if (isPlural) 2 else 1))
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }

            return dialog
        }

        /**
         * Creates an instance of MegaProgressDialog.
         *
         * @param context   The parent context.
         * @param message   The message to display in the dialog.
         * @return The created MegaProgressDialog.
         */
        @JvmStatic
        fun getMegaProgressDialog(context: Context, message: String): MegaProgressDialog =
            MegaProgressDialog(context).apply {
                setMessage(message)
                show()
            }

        /**
         * Checks if a MegaProgressDialog is shown.
         *
         * @param dialog    The dialog to check.
         * @return True if the dialog is showing, false otherwise.
         */
        @JvmStatic
        fun isMegaProgressDialogShown(dialog: MegaProgressDialog?): Boolean =
            dialog?.isShowing == true

        /**
         * Dismisses a MegaProgressDialog.
         *
         * @param dialog    MegaProgressDialog to dismiss.
         */
        @JvmStatic
        fun dismissMegaProgressDialogIfExists(dialog: MegaProgressDialog?) {
            dialog?.dismiss()
        }
    }
}