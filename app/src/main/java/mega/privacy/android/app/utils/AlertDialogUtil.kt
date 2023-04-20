package mega.privacy.android.app.utils

import com.google.android.material.R as MaterialR
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
    fun dismissAlertDialogIfExists(dialog: AlertDialog?) {
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
                MaterialR.attr.colorSecondary
            ) else ContextCompat.getColor(context, R.color.teal_300_alpha_038)
        )
    }

    /**
     * Sets an error in EditText.
     *
     * @param error          Message to show as error.
     * @param editTextLayout TextInputLayout which should contain the EditText.
     * @param errorIcon      Image icon to show as error indicator.
     */
    @JvmStatic
    fun setEditTextError(error: String?, editTextLayout: TextInputLayout, errorIcon: ImageView) {
        if (error.isNullOrEmpty()) return

        editTextLayout.apply {
            setError(error)
            setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
        }

        errorIcon.isVisible = true
    }

    /**
     * Hides an error in EditText.
     *
     * @param editTextLayout TextInputLayout which should contain the EditText.
     * @param errorIcon      Image icon to hide as error indicator.
     */
    @JvmStatic
    fun quitEditTextError(editTextLayout: TextInputLayout, errorIcon: ImageView) {
        editTextLayout.apply {
            error = null
            setHintTextAppearance(MaterialR.style.TextAppearance_Design_Hint)
        }

        errorIcon.isVisible = false
    }
}