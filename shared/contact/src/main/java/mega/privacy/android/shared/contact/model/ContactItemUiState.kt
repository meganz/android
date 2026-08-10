package mega.privacy.android.shared.contact.model

import androidx.compose.runtime.Stable
import mega.android.core.ui.components.contact.state.ContactItemStatus

/**
 * UI state for [ContactItemView]. Pre-resolved presentational data for a
 * single contact row — no domain types, no Android resource lookups.
 *
 * @property handle Stable identifier for this row; usable as a `LazyColumn` key
 * and to look the contact back up in the source list when handling clicks.
 * @property displayName Resolved name to render as the row title.
 * @property status Drives the inline status indicator next to the title.
 * @property lastSeen Last time the contact was active.
 * @property avatar Avatar source: image file or coloured initials.
 * @property isVerified Whether to overlay the "verified contact" badge on the avatar.
 * @property email Contact's email address, used when callers need to identify the
 * contact by email (e.g. for navigation to Contact info or remove-by-email). Empty
 * string when the email is unavailable.
 */
@Stable
data class ContactItemUiState(
    val handle: Long,
    val displayName: String,
    val status: ContactItemStatus,
    val lastSeen: Int?,
    val avatar: AvatarData,
    val isVerified: Boolean,
    val email: String = "",
)

