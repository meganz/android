package mega.privacy.android.domain.entity.chat

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.RegexPatternType

/**
 * Link detail
 *
 * @param link Link.
 * @param type Link type.
 */
@Serializable
data class LinkDetail(
    val link: String,
    val type: RegexPatternType,
)