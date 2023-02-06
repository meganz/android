package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.QuerySignupLinkException

internal val QuerySignupLinkException.messageId: Int?
    get() = when (this) {
        is QuerySignupLinkException.LinkNoLongerAvailable -> R.string.reg_link_expired
        is QuerySignupLinkException.Unknown -> this.megaException.getErrorStringId()
    }