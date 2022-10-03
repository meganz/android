package mega.privacy.android.domain.entity.chat

/**
 * View item that represents a Contact Request at UI level.
 *
 * @property id         File id
 * @property isImage    True, if it's image. False, if it's video.
 * @property isTakePicture     True, if it's take picture option. False, otherwise.
 * @property hasCameraPermissions      File title
 * @property title      File title
 * @property fileUri    File URI
 * @property dateAdded  Date added
 * @property duration   Video duration
 * @property isSelected   True, if it's selected. False, if not.
 * @property filePath   File Path
 */
data class FileGalleryItem constructor(
    val id: Long,
    var isImage: Boolean,
    var isTakePicture: Boolean,
    val hasCameraPermissions: Boolean? = false,
    val title: String? = null,
    var fileUri: String? = null,
    var dateAdded: Long? = null,
    var duration: String? = "",
    var isSelected: Boolean = false,
    var filePath: String? = null,
)