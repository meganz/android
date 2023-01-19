package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults

internal val QRCodeQueryResults.dialogTitle: Int
    get() = when (this) {
        QRCodeQueryResults.CONTACT_QUERY_EEXIST -> R.string.invite_not_sent
        QRCodeQueryResults.CONTACT_QUERY_DEFAULT -> R.string.invite_not_sent
        else -> -1
    }

internal val QRCodeQueryResults.dialogContent: Int
    get() = when (this) {
        QRCodeQueryResults.CONTACT_QUERY_EEXIST -> R.string.invite_not_sent_text_already_contact
        QRCodeQueryResults.CONTACT_QUERY_DEFAULT -> R.string.invite_not_sent_text
        else -> -1
    }