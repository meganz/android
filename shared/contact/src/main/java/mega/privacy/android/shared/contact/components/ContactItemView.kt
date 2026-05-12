package mega.privacy.android.shared.contact.components

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.contact.component.ContactStatusDot
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.profile.MediumProfileIcon
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


/**
 * Contact item view
 *
 * @param contactItemUiState
 * @param modifier
 * @param onClick
 * @param selected
 * @param showDivider
 */
@Composable
fun ContactItemView(
    contactItemUiState: ContactItemUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showDivider: Boolean = true,
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
        selected = selected,
        showDivider = showDivider,
    )
}

/**
 * Renders a single contact row with an avatar, display name, optional
 * subtitle, an inline status indicator, and an optional verified badge.
 *
 * Uses the design-system primitives `FlexibleLineListItem`,
 * `MediumProfilePicture`, and `SubtleDivider`.
 *
 * @param displayName Name to render as the row title.
 * @param statusText Pre-resolved subtitle (status label or "Last seen …"); null hides the subtitle.
 * @param status Drives the inline status indicator next to the title.
 * @param avatar Avatar source: image file or coloured initials.
 * @param isVerified Whether to overlay the "verified contact" badge on the avatar.
 * @param modifier Modifier applied to the row container.
 * @param onClick Click handler; pass `null` to render a non-interactive row.
 * @param selected When true, replaces the avatar with a brand-coloured check tile (multi-select pickers).
 * @param showDivider Toggles the bottom divider.
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
    selected: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        FlexibleLineListItem(
            modifier = Modifier.testTag(CONTACT_ITEM_VIEW_ROW),
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
                    selected = selected
                )
            },
            titleTrailingElement = if (status == ContactItemStatus.Unknown) {
                null
            } else {
                { ContactStatusDot(status) }
            },
        )
        if (showDivider) SubtleDivider()
    }
}


@Composable
private fun ContactAvatar(
    avatar: AvatarData,
    displayName: String,
    isVerified: Boolean,
    selected: Boolean,
) {
    Box(modifier = Modifier.testTag(CONTACT_ITEM_VIEW_AVATAR)) {
        if (selected) {
            MediumProfileIcon(
                icon = IconPack.Medium.Thin.Outline.Check,
                iconTint = IconColor.Inverse,
                contentDescription = null,
                avatarColor = DSTokens.colors.icon.brand,
            )
        } else {
            when (avatar) {
                is AvatarData.Image -> MediumProfilePicture(
                    imageFile = avatar.file,
                    name = null,
                    contentDescription = displayName,
                )

                is AvatarData.Initials -> MediumProfilePicture(
                    imageFile = null,
                    name = avatar.initials,
                    contentDescription = displayName,
                    avatarColor = avatar.avatarColor,
                )
            }
            if (isVerified) {
                Image(
                    painter = painterResource(IconR.drawable.ic_contact_verified),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(14.dp)
                        .testTag(CONTACT_ITEM_VIEW_VERIFIED_BADGE),
                )
            }
        }
    }
}

private fun statusText(status: ContactItemStatus): Int = when (status) {
    ContactItemStatus.Away -> R.string.away_status
    ContactItemStatus.Online -> R.string.online_status
    ContactItemStatus.Busy -> R.string.busy_status
    else -> R.string.offline_status
}

@Composable
internal fun getLastSeenString(lastGreen: Int?): String? {
    if (lastGreen == null) return null

    val lastGreenCalendar = Calendar.getInstance().apply { add(Calendar.MINUTE, -lastGreen) }
    val timeToConsiderAsLongTimeAgo = 65535

    Timber.d("Ts last green: %s", lastGreenCalendar.timeInMillis)

    return when {
        lastGreen >= timeToConsiderAsLongTimeAgo -> {
            stringResource(id = R.string.last_seen_long_time_ago)
        }

        compareLastSeenWithToday(lastGreenCalendar) == 0 -> {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_today, time)
        }

        else -> {
            var dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)

            dateFormat = SimpleDateFormat(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd MMM"),
                Locale.getDefault()
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
internal const val CONTACT_ITEM_VIEW_AVATAR = "contact_item_view:avatar"
internal const val CONTACT_ITEM_VIEW_VERIFIED_BADGE = "contact_item_view:verified_badge"

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
private fun ContactItemViewSelectedPreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            displayName = "Diana",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = false,
            onClick = {},
            selected = true,
        )
    }
}
