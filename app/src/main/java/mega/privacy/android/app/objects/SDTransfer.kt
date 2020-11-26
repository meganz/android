package mega.privacy.android.app.objects

data class SDTransfer(
    val tag: Int,
    val name: String,
    val size: String,
    val nodeHandle: String,
    val path: String,
    val appData: String
)