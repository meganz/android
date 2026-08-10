package mega.privacy.android.shared.contact.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.contact.component.ContactStatusDot
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R
import java.text.SimpleDateFormat
import java.util.Calendar


/**
 * Contact item view
 *
 * @param contactItemUiState
 * @param modifier
 * @param onClick
 * @param onLongClick
 * @param onAvatarClick When non-null, taps on the avatar fire this instead of bubbling up to [onClick].
 * @param onMoreClicked When non-null, a trailing kebab icon is rendered and fires this on tap.
 * @param onRemoveClicked When non-null, a trailing x-circle icon is rendered and fires this on tap.
 * @param selected
 * @param inSelectionMode
 */
@Composable
fun ContactItemView(
    contactItemUiState: ContactItemUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    onMoreClicked: (() -> Unit)? = null,
    onRemoveClicked: (() -> Unit)? = null,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
) {
    val statusString = stringResource(id = statusText(contactItemUiState.status))
    val lastSeenText =
        if (contactItemUiState.status == ContactItemStatus.Online) {
            statusString
        } else {
            getLastSeenString(contactItemUiState.lastSeen) ?: statusString
        }

    ContactItemView(
        displayName = contactItemUiState.displayName,
        statusText = lastSeenText,
        status = contactItemUiState.status,
        avatar = contactItemUiState.avatar,
        isVerified = contactItemUiState.isVerified,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        onAvatarClick = onAvatarClick,
        onMoreClicked = onMoreClicked,
        onRemoveClicked = onRemoveClicked,
        selected = selected,
        inSelectionMode = inSelectionMode,
    )
}

/**
 * Renders a single contact row with an avatar, display name, optional
 * subtitle, an inline status indicator, and an optional verified badge.
 *
 * Uses the design-system primitives `FlexibleLineListItem` and
 * `MediumProfilePicture`.
 *
 * @param displayName Name to render as the row title.
 * @param statusText Pre-resolved subtitle (status label or "Last seen …"); null hides the subtitle.
 * @param status Drives the inline status indicator next to the title.
 * @param avatar Avatar source: image file or coloured initials.
 * @param isVerified Whether to overlay the "verified contact" badge on the avatar.
 * @param modifier Modifier applied to the row container.
 * @param onClick Click handler; pass `null` to render a non-interactive row.
 * @param onLongClick Optional long-click handler on the row.
 * @param onAvatarClick When non-null, taps on the avatar fire this instead of bubbling up to [onClick].
 * @param onMoreClicked When non-null, a trailing kebab icon is rendered and fires this on tap.
 * @param onRemoveClicked When non-null, a trailing x-circle icon is rendered and fires this on tap.
 * @param selected When true, replaces the avatar with a brand-coloured check tile (multi-select pickers).
 * @param inSelectionMode When true, renders a trailing checkbox in place of the kebab.
 */
@Composable
fun ContactItemView(
    displayName: String,
    statusText: String?,
    status: ContactItemStatus,
    avatar: AvatarData,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    onMoreClicked: (() -> Unit)? = null,
    onRemoveClicked: (() -> Unit)? = null,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
) {
    FlexibleLineListItem(
        modifier = modifier.testTag(CONTACT_ITEM_VIEW_ROW),
        title = displayName,
        subtitle = statusText,
        titleMaxLines = 1,
        subtitleMaxLines = 1,
        enableClick = onClick != null || onLongClick != null,
        onClickListener = onClick ?: {},
        onLongClickListener = onLongClick ?: {},
        leadingElement = {
            ContactAvatar(
                avatar = avatar,
                displayName = displayName,
                isVerified = isVerified,
                onAvatarClick = onAvatarClick,
            )
        },
        titleTrailingElement = if (status == ContactItemStatus.Unknown) {
            null
        } else {
            { ContactStatusDot(status) }
        },
        trailingElement = when {
            inSelectionMode -> {
                {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckStateChanged = {},
                            tapTargetArea = false,
                            clickable = false,
                            modifier = Modifier.testTag(CONTACT_ITEM_VIEW_CHECKBOX),
                        )
                    }
                }
            }

            onMoreClicked != null -> {
                {
                    IconButton(
                        onClick = onMoreClicked,
                        modifier = Modifier.testTag(CONTACT_ITEM_VIEW_MORE),
                    ) {
                        MegaIcon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                            contentDescription = stringResource(R.string.more_options),
                            tint = IconColor.Secondary,
                        )
                    }
                }
            }

            onRemoveClicked != null -> {
                {
                    IconButton(
                        onClick = onRemoveClicked,
                        modifier = Modifier.testTag(CONTACT_ITEM_VIEW_REMOVE),
                    ) {
                        MegaIcon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.XCircle),
                            contentDescription = stringResource(R.string.general_remove),
                            tint = IconColor.Secondary,
                        )
                    }
                }
            }

            else -> null
        },
    )
}


private fun statusText(status: ContactItemStatus): Int = when (status) {
    ContactItemStatus.Away -> R.string.away_status
    ContactItemStatus.Online -> R.string.online_status
    ContactItemStatus.Busy -> R.string.busy_status
    else -> R.string.offline_status
}

/**
 * Standard last-seen formatter for contact rows.
 *
 * Converts the SDK's "minutes since last seen" integer into a localised
 * human-readable string (e.g. "Last seen today at 14:32", "Last seen long
 * time ago"). Used by contact list rows and chat headers to render a user's
 * presence subtitle when they are not currently online.
 *
 * @param lastGreen minutes elapsed since the user was last seen, or `null`
 *                  if unknown.
 * @return the formatted "Last seen …" string, or `null` if [lastGreen] is `null`.
 */
@Composable
fun getLastSeenString(lastGreen: Int?): String? {
    if (lastGreen == null) return null

    val lastGreenCalendar = Calendar.getInstance().apply { add(Calendar.MINUTE, -lastGreen) }
    val timeToConsiderAsLongTimeAgo = 65535

    return when {
        lastGreen >= timeToConsiderAsLongTimeAgo -> {
            stringResource(id = R.string.last_seen_long_time_ago)
        }

        compareLastSeenWithToday(lastGreenCalendar) == 0 -> {
            val dateFormat = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_today, time)
        }

        else -> {
            var dateFormat = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)

            dateFormat = SimpleDateFormat(
                DateFormat.getBestDateTimePattern(LocalLocale.current.platformLocale, "dd MMM"),
                LocalLocale.current.platformLocale
            )
            val day = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_general, day, time)
        }
    }.replace("[A]", "").replace("[/A]", "")
}

private fun compareLastSeenWithToday(lastGreen: Calendar): Int {
    val today = Calendar.getInstance()

    return when {
        lastGreen.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
            lastGreen.get(Calendar.YEAR) - today.get(Calendar.YEAR)
        }

        lastGreen.get(Calendar.MONTH) != today.get(Calendar.MONTH) -> {
            lastGreen.get(Calendar.MONTH) - today.get(Calendar.MONTH)
        }

        else -> {
            lastGreen.get(Calendar.DAY_OF_MONTH) - today.get(Calendar.DAY_OF_MONTH)
        }
    }
}


internal const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
internal const val CONTACT_ITEM_VIEW_VERIFIED_BADGE = "contact_item_view:verified_badge"
internal const val CONTACT_ITEM_VIEW_MORE = "contact_item_view:more"
internal const val CONTACT_ITEM_VIEW_REMOVE = "contact_item_view:remove"
internal const val CONTACT_ITEM_VIEW_CHECKBOX = "contact_item_view:checkbox"

private class ContactItemPreviewProvider :
    CollectionPreviewParameterProvider<ContactItemUiState>(
        listOf(
            ContactItemUiState(
                handle = 1L,
                displayName = "Alice Anderson",
                status = ContactItemStatus.Online,
                lastSeen = null,
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                isVerified = true,
            ),
            ContactItemUiState(
                handle = 2L,
                displayName = "Bob Brown",
                lastSeen = 45,
                status = ContactItemStatus.Away,
                avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                isVerified = false,
            ),
            ContactItemUiState(
                handle = 3L,
                displayName = "Charlie",
                lastSeen = null,
                status = ContactItemStatus.Unknown,
                avatar = AvatarData.Initials(initials = "C", avatarColor = Color(0xFF6A1B9A)),
                isVerified = false,
            ),
        ),
    )

@CombinedThemePreviews
@Composable
private fun ContactItemViewPreview(
    @PreviewParameter(ContactItemPreviewProvider::class) data: ContactItemUiState,
) {
    AndroidThemeForPreviews {
        ContactItemView(
            contactItemUiState = data,
            onClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactItemViewSelectionModeUnselectedPreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            displayName = "Diana",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = false,
            onClick = {},
            inSelectionMode = true,
            selected = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactItemViewSelectionModeSelectedPreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            displayName = "Diana",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = false,
            onClick = {},
            inSelectionMode = true,
            selected = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactItemViewMoreMenuPreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            displayName = "Diana",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = false,
            onClick = {},
            onMoreClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactItemViewRemovePreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            displayName = "Diana",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = false,
            onRemoveClicked = {},
        )
    }
}
