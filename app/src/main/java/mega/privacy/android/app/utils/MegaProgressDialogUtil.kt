package mega.privacy.android.app.utils

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString

/**
 * A tool static instance that provide functions for building AlertDialog that can display the circle progress and message.
 */
object MegaProgressDialogUtil {

    /**
     * Creates an instance of AlertDialog.
     *
     * @param context   The parent context.
     * @param message   The message to display in the dialog.
     * @return The created AlertDialog.
     */
    @JvmStatic
    fun createProgressDialog(context: Context, message: CharSequence?): AlertDialog {
        val inflater = LayoutInflater.from(context).inflate(R.layout.progress_bar, null);
        val processDialog = MaterialAlertDialogBuilder(context).create()

        processDialog.setView(inflater)

        message?.let {
            val txtMessage: TextView? = inflater.findViewById(R.id.progress_msg)
            txtMessage?.text = it
        }

        return processDialog
    }


    /**
     * Shows a process dialog informing is processing files, such as selecting files from device.
     *
     * @param context   The parent context.
     * @param intent    A description of an operation to be performed.
     * @return The created AlertDialog.
     */
    @JvmStatic
    fun showProcessFileDialog(context: Context, intent: Intent?): AlertDialog {
        var isPlural = false

        if (intent != null) {
            val imageUris = with(intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
            }

            isPlural = imageUris != null && imageUris.size > 1

            if (!isPlural) {
                val clipData = intent.clipData
                isPlural = clipData != null && clipData.itemCount > 1
            }
        }

        val dialog = createProgressDialog(
            context,
            getQuantityString(R.plurals.upload_prepare, if (isPlural) 2 else 1)
        )

        dialog.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }

        return dialog
    }
}