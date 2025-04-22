package mega.privacy.android.app.presentation.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import mega.privacy.android.app.utils.Constants

/**
 * Hides keyboard.
 */
fun Activity.hideKeyboard() {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

/**
 * Start an [Intent] to select a folder to be uploaded
 */
fun Activity.uploadFolderManually() {
    this.startActivityForResult(
        Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
             null
        ), Constants.REQUEST_CODE_GET_FOLDER
    )
}