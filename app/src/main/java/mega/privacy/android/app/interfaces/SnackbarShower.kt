package mega.privacy.android.app.interfaces

import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.NOT_SPACE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * Legacy interface to allow showing SnackBars in xml views or views migrated to Compose but not correctly using Scaffold.
 */
interface SnackbarShower {
    /**
     * Show a snackbar with a message and based on type, it could have an action or specific message.
     */
    fun showSnackbar(
        type: Int = SNACKBAR_TYPE,
        content: String?,
        chatId: Long = MEGACHAT_INVALID_HANDLE,
    )

    /**
     * Show a snackbar with a message and an action.
     */
    fun showSnackbar(type: Int, content: String, action: () -> Unit)
}

/**
 * Shows a snackbar with a message.
 */
fun SnackbarShower.showSnackbar(content: String) {
    showSnackbar(SNACKBAR_TYPE, content, MEGACHAT_INVALID_HANDLE)
}

/**
 * Shows a snackbar with a message informing a message or several messages was/were sent to chat and SEE action.
 */
fun SnackbarShower.showSnackbarWithChat(content: String?, chatId: Long) {
    showSnackbar(MESSAGE_SNACKBAR_TYPE, content, chatId)
}

/**
 * Shows a snackbar with a message indicating there is not enough space.
 */
fun SnackbarShower.showNotEnoughSpaceSnackbar() {
    showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null, MEGACHAT_INVALID_HANDLE)
}
