package mega.privacy.android.app.presentation.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
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
 * Changes the status bar elevation depending on if the theme is dark mode and if the
 * view is scrolled.
 *
 * @param scrolled  True if the view is scrolled, false otherwise.
 * @param isDark    True if the theme is dark, false if is light.
 */
fun Activity.changeStatusBarColor(scrolled: Boolean, isDark: Boolean) {
    window?.statusBarColor = when {
        scrolled && isDark -> ContextCompat.getColor(this, R.color.action_mode_background)
        isDark -> ContextCompat.getColor(this, R.color.dark_grey)
        else -> ContextCompat.getColor(this, android.R.color.white)
    }
}

/**
 * Start an [Intent] to select files to be uploaded
 */
fun Activity.uploadFilesManually() {
    this.startActivityForResult(
        Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .setType("*/*"), null
        ), Constants.REQUEST_CODE_GET_FILES
    )
}

/**
 * Start an [Intent] to select a folder to be uploaded
 */
fun Activity.uploadFolderManually() {
    this.startActivityForResult(
        Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null
        ), Constants.REQUEST_CODE_GET_FOLDER
    )
}