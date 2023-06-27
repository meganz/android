package mega.privacy.android.domain.entity.chat

/**
 * Data class for storing meta data in a [ChatMessage].
 *
 * @property type [ContainsMetaType]
 * @property textMessage Generic message to be shown when app does not support the type of the contained meta.
 * @property richPreview [RichPreview]
 * @property geolocation [ChatGeolocation]
 * @property giphy [Giphy]
 */
data class ContainsMeta(
    val type: ContainsMetaType,
    val textMessage: String,
    val richPreview: RichPreview?,
    val geolocation: ChatGeolocation?,
    val giphy: Giphy?,
)
