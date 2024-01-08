package mega.privacy.android.domain.entity.chat

/**
 * Rich link config
 *
 * @property isShowRichLinkWarning
 * @property isRichLinkEnabled
 * @property counterNotNowRichLinkWarning
 */
data class RichLinkConfig(
    val isShowRichLinkWarning: Boolean = false,
    val isRichLinkEnabled: Boolean = false,
    val counterNotNowRichLinkWarning: Int = -1,
)