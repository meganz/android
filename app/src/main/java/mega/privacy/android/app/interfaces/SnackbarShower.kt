package mega.privacy.android.app.interfaces

interface SnackbarShower {
    fun showSnackbar(content: String)

    fun showSnackbarWithChat(content: String?, chatId: Long)
}
