package mega.privacy.android.app.lollipop.megachat

data class FileGalleryItem(
    var isImage: Boolean = false,
    var duration: Int,
    var fileUri: String? = null,
)