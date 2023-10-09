package mega.privacy.android.app.main.dialog.link

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.RegexPatternType
import javax.inject.Inject

/**
 * Open link positive text mapper
 *
 */
internal class OpenLinkPositiveTextMapper @Inject constructor() {
    @StringRes
    operator fun invoke(linkType: RegexPatternType?) = when (linkType) {
        RegexPatternType.CHAT_LINK -> R.string.action_open_chat_link
        RegexPatternType.CONTACT_LINK -> R.string.action_open_contact_link
        else -> R.string.context_open_link
    }
}