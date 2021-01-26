package mega.privacy.android.app.components

import android.text.Editable
import android.text.TextWatcher

/**
 * Class which implements TextWatcher to avoid repeated lines when an input field needs to implement
 * it, but only needs to override some of the interface methods.
 */
open class CustomTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(editable: Editable?) {

    }
}