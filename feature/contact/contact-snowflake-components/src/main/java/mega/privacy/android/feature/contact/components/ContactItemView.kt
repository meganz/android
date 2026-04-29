package mega.privacy.android.feature.contact.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
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

/**
 * Renders a single contact row with an avatar, display name, optional
 * subtitle, an inline status indicator, and an optional verified badge.
 *
 * Uses the design-system primitives `FlexibleLineListItem`,
 * `MediumProfilePicture`, and `SubtleDivider`.
 *
 * @param state Pre-resolved presentational data for the row.
 * @param modifier Modifier applied to the row container.
 * @param onClick Click handler; pass `null` to render a non-interactive row.
 * @param selected When true, replaces the avatar with a brand-coloured check tile (multi-select pickers).
 * @param showDivider Toggles the bottom divider.
 */
@Composable
fun ContactItemView(
    state: ContactItemUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        FlexibleLineListItem(
            modifier = Modifier.testTag(CONTACT_ITEM_VIEW_ROW),
            title = state.displayName,
            subtitle = state.statusText,
            titleMaxLines = 1,
            subtitleMaxLines = 1,
            enableClick = onClick != null,
            onClickListener = onClick ?: {},
            leadingElement = { ContactAvatar(state, selected) },
            titleTrailingElement = if (state.status == ContactItemStatus.Unknown) {
                null
            } else {
                { ContactStatusDot(state.status) }
            },
        )
        if (showDivider) SubtleDivider()
    }
}

@Composable
private fun ContactAvatar(
    state: ContactItemUiState,
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
            when (val avatar = state.avatar) {
                is AvatarData.Image -> MediumProfilePicture(
                    imageFile = avatar.file,
                    name = null,
                    contentDescription = state.displayName,
                )

                is AvatarData.Initials -> MediumProfilePicture(
                    imageFile = null,
                    name = avatar.initials,
                    contentDescription = state.displayName,
                    avatarColor = avatar.avatarColor,
                )
            }
            if (state.isVerified) {
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

@Composable
private fun ContactStatusDot(status: ContactItemStatus) {
    val color = when (status) {
        ContactItemStatus.Online -> DSTokens.colors.indicator.green
        ContactItemStatus.Away -> DSTokens.colors.indicator.yellow
        ContactItemStatus.Busy -> DSTokens.colors.indicator.pink
        ContactItemStatus.Offline -> DSTokens.colors.icon.secondary
        ContactItemStatus.Unknown -> return
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .border(2.dp, DSTokens.colors.background.pageBackground, CircleShape)
            .background(color, CircleShape)
            .testTag(CONTACT_ITEM_VIEW_STATUS_DOT),
    )
}

internal const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
internal const val CONTACT_ITEM_VIEW_AVATAR = "contact_item_view:avatar"
internal const val CONTACT_ITEM_VIEW_VERIFIED_BADGE = "contact_item_view:verified_badge"
internal const val CONTACT_ITEM_VIEW_STATUS_DOT = "contact_item_view:status_dot"

private class ContactItemPreviewProvider : CollectionPreviewParameterProvider<ContactItemUiState>(
    listOf(
        ContactItemUiState(
            displayName = "Alice Anderson",
            statusText = "Online",
            status = ContactItemStatus.Online,
            avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            isVerified = true,
        ),
        ContactItemUiState(
            displayName = "Bob Brown",
            statusText = "Last seen today at 09:42",
            status = ContactItemStatus.Away,
            avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
            isVerified = false,
        ),
        ContactItemUiState(
            displayName = "Charlie",
            statusText = null,
            status = ContactItemStatus.Unknown,
            avatar = AvatarData.Initials(initials = "C", avatarColor = Color(0xFF6A1B9A)),
            isVerified = false,
        ),
    ),
)

@CombinedThemePreviews
@Composable
private fun ContactItemViewPreview(
    @PreviewParameter(ContactItemPreviewProvider::class) state: ContactItemUiState,
) {
    AndroidThemeForPreviews {
        ContactItemView(
            state = state,
            onClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactItemViewSelectedPreview() {
    AndroidThemeForPreviews {
        ContactItemView(
            state = ContactItemUiState(
                displayName = "Diana",
                statusText = "Online",
                status = ContactItemStatus.Online,
                avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
                isVerified = false,
            ),
            onClick = {},
            selected = true,
        )
    }
}
