package mega.privacy.android.app.interfaces

import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.NOT_SPACE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.RESUME_TRANSFERS_TYPE
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

interface SnackbarShower {
    fun showSnackbar(type: Int = SNACKBAR_TYPE, content: String?, chatId: Long = -1L)

    companion object {
        val IDLE = object : SnackbarShower {
            override fun showSnackbar(type: Int, content: String?, chatId: Long) {}
        }
    }
}

fun SnackbarShower.showSnackbar(content: String) {
    showSnackbar(SNACKBAR_TYPE, content, MEGACHAT_INVALID_HANDLE)
}

fun SnackbarShower.showTransfersSnackBar(content: String) {
    showSnackbar(RESUME_TRANSFERS_TYPE, content, MEGACHAT_INVALID_HANDLE)
}

fun SnackbarShower.showSnackbarWithChat(content: String?, chatId: Long) {
    showSnackbar(MESSAGE_SNACKBAR_TYPE, content, chatId)
}

fun SnackbarShower.showNotEnoughSpaceSnackbar() {
    showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null, MEGACHAT_INVALID_HANDLE)
}
