package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.RegexPatternType

/**
 * Link detail
 *
 * @param link Link.
 * @param type Link type.
 */
data class LinkDetail(
    val link: String,
    val type: RegexPatternType,
)