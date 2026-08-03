package mega.privacy.android.feature.contact.requests.mapper

import androidx.compose.ui.graphics.Color
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a domain [ContactRequest] to the presentational [ContactRequestUiItem] rendered by the
 * contact requests screen.
 *
 * Contact requests are pre-verification and carry no online status, so the mapped contact always
 * uses [ContactItemStatus.Unknown] and `isVerified = false`. The email shown is the target email
 * for outgoing requests and the source email for incoming ones. The avatar falls back to coloured
 * initials built from that email, since a request carries no avatar file.
 */
class ContactRequestUiItemMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param request Domain contact request.
     * @return mapped UI row.
     */
    operator fun invoke(request: ContactRequest): ContactRequestUiItem {
        val email = request.email()
        return ContactRequestUiItem(
            handle = request.handle,
            isOutgoing = request.isOutgoing,
            contact = ContactItemUiState(
                handle = request.handle,
                displayName = email,
                status = ContactItemStatus.Unknown,
                lastSeen = null,
                avatar = AvatarData.Initials(
                    initials = email.firstLetter(),
                    avatarColor = Color.Unspecified,
                ),
                isVerified = false,
                email = email,
            ),
            createdTime = request.creationTime.formatCreationDate(),
        )
    }

    private fun ContactRequest.email(): String =
        (if (isOutgoing) targetEmail else sourceEmail) ?: sourceEmail

    private fun String.firstLetter(): String =
        trim().take(1).uppercase(Locale.getDefault()).ifBlank { UNKNOWN_INITIAL }

    private fun Long.formatCreationDate(): String =
        DATE_FORMATTER.format(
            Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault()).toLocalDate()
        )

    private companion object {
        const val UNKNOWN_INITIAL = "U"

        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
