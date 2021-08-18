package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.R
import mega.privacy.android.app.components.MegaProgressDialog

object ProgressDialogUtil {
    @SuppressLint("StaticFieldLeak")
    private var dialog: MegaProgressDialog? = null
    @JvmField
    var shouldShowDialog = false
    private var isPl = false
    @JvmStatic
    fun showProcessFileDialog(context: Context, intent: Intent?) {
        dialog = object : MegaProgressDialog(context) {
            override fun onDetachedFromWindow() {
                dismiss()
            }
        }
        dialog?.let{
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
        val i = if (isPl) 2 else 1
        val message = context.resources.getQuantityString(R.plurals.upload_prepare, i)

        dialog?.let{
            it.setMessage(message)
            shouldShowDialog = true
            it.show()
        }
    }

    @JvmStatic
    fun dissmisDialog() {
        shouldShowDialog = false
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }

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