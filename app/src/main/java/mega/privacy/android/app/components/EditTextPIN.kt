package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import timber.log.Timber

/**
 * Customized EditText view for a digit of the sequence for a PIN or numeric passcode.
 */
class EditTextPIN @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyle) {
    /**
     * EditText view which contains the previous digit of the sequence for the PIN or numeric passcode.
     */
    var previousDigitEditText: AppCompatEditText? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        Timber.tag("EditTextPIN").d("onCreateInputConnection()")
        return super.onCreateInputConnection(outAttrs)?.let {
            PinInputConnection(it, true)
        }
    }

    private inner class PinInputConnection(target: InputConnection?, mutable: Boolean) :
        InputConnectionWrapper(target, mutable) {
        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                Timber.tag("EditTextPIN").d(event.keyCode.toString())
                text?.let { text ->
                    if (text.toString().isNotEmpty()) {
                        text.clear()
                    } else {
                        previousDigitEditText?.apply {
                            this.text?.clear()
                            requestFocus()
                        }
                    }
                }
                return true
            }
            return super.sendKeyEvent(event)
        }
    }
}