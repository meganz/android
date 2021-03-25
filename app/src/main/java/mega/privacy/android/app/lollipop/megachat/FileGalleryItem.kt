package mega.privacy.android.app.lollipop.megachat

data class FileGalleryItem(
    var isImage: Boolean = false,
    var duration: String? = null,
    var fileUri: String? = null,
    var dateAdded: String? = null
)