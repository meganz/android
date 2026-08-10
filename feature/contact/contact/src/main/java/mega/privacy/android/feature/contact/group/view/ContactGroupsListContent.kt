package mega.privacy.android.feature.contact.group.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.MultiAvatarView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Contact groups list content. Renders the list of group rows, or an empty state when there
 * are no groups.
 *
 * @param state
 * @param contentPadding
 * @param onGroupClick
 * @param modifier
 */
@Composable
internal fun ContactGroupsListContent(
    state: ContactGroupUiState.Data,
    contentPadding: PaddingValues,
    onGroupClick: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.groups.isEmpty()) {
        EmptyStateView(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .testTag(CONTACT_GROUPS_EMPTY_TAG),
            imagePainter = painterResource(iconPackR.drawable.ic_message_chat_glass),
            title = stringResource(sharedR.string.contacts_groups_empty_title),
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_GROUPS_LAZY_COLUMN_TAG),
        contentPadding = contentPadding,
    ) {
        items(
            items = state.groups,
            key = { group -> group.chatId },
        ) { group ->
            ContactGroupItemRow(
                group = group,
                onClick = { onGroupClick(group.chatId) },
            )
        }
    }
}

/**
 * A single contact group row: overlapping avatars, the group name, and a private indicator.
 *
 * @param group
 * @param onClick
 */
@Composable
private fun ContactGroupItemRow(
    group: ContactGroupItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag(contactGroupRowTag(group.chatId))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MultiAvatarView(
            avatars = group.avatarData,
            modifier = Modifier.size(40.dp),
        )
        MegaText(
            text = group.name,
            modifier = Modifier.weight(1f),
            textColor = TextColor.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (group.isPrivate) {
            MegaIcon(
                modifier = Modifier
                    .size(20.dp)
                    .testTag(contactGroupPrivateIconTag(group.chatId)),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Lock),
                contentDescription = null,
                tint = IconColor.Accent,
            )
        }
    }
}

internal const val CONTACT_GROUPS_EMPTY_TAG = "contact_groups_screen:empty"
internal const val CONTACT_GROUPS_LAZY_COLUMN_TAG = "contact_groups_screen:lazy_column"

internal fun contactGroupRowTag(chatId: Long): String = "contact_groups_screen:row_$chatId"

internal fun contactGroupPrivateIconTag(chatId: Long): String =
    "contact_groups_screen:private_$chatId"
