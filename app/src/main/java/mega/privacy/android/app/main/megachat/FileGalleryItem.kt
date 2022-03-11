package mega.privacy.android.app.main.megachat

data class FileGalleryItem(
    var id: Long,
    var isImage: Boolean = false,
    var duration: Long,
    var fileUri: String? = null,
    var dateAdded: String? = null
)