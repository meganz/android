package mega.privacy.android.domain.entity.chat

interface ChatRichPreviewInfo {
    val title: String
    val description: String
    val image: String?
    val imageFormat: String?
    val icon: String?
    val iconFormat: String?
    val url: String
    val domainName: String
}