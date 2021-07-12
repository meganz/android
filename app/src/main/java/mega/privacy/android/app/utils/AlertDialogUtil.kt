package mega.privacy.android.app.utils

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout
import mega.privacy.android.app.R

object AlertDialogUtil {

    @JvmStatic
    fun isAlertDialogShown(dialog: AlertDialog?): Boolean = dialog?.isShowing == true

    @JvmStatic
    fun dismissAlertDialogIfShown(dialog: AlertDialog?) {
        dialog?.dismiss()
    }

    /**
     * Enables or disabled a dialog button in a customized way.
     *
     * @param context Current context.
     * @param enable  True if should enable, false if should disable.
     * @param button  The button to enable or disable.
     */
    @JvmStatic
    fun enableOrDisableDialogButton(context: Context, enable: Boolean, button: Button) {
        button.isEnabled = enable
        button.setTextColor(
            if (enable) ColorUtils.getThemeColor(
                context,
                R.attr.colorSecondary
            ) else ContextCompat.getColor(context, R.color.teal_300_alpha_038)
        )
    }

    @JvmStatic
    fun setEditTextError(error: String?, editTextLayout: TextInputLayout, errorIcon: ImageView) {
        if (error.isNullOrEmpty()) return

        editTextLayout.apply {
            setError(error)
            setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
        }

        errorIcon.isVisible = true
    }

    @JvmStatic
    fun quitEditTextError(editTextLayout: TextInputLayout, errorIcon: ImageView) {
        editTextLayout.apply {
            error = null
            setHintTextAppearance(R.style.TextAppearance_Design_Hint)
        }

        errorIcon.isVisible = false
    }
}