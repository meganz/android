package mega.privacy.android.domain.entity.chat

interface ChatGifInfo {
    val mp4Src: String?
    val webpSrc: String?
    val title: String?
    val mp4Size: Int
    val webpSize: Int
    val width: Int
    val height: Int
}