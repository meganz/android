package mega.privacy.android.app.psa

data class Psa(
    val id: Int,
    val title: String,
    val text: String,
    val imageUrl: String?,
    val positiveText: String?,
    val positiveLink: String?,
    val url: String?
)
