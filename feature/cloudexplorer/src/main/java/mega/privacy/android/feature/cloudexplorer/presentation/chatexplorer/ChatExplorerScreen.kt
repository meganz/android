package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ChatExplorerContent(
    onNewGroupChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(CHAT_EXPLORER_LIST_TAG)
            .padding(top = 8.dp),
    ) {
        NewGroupChatItemView(onClick = onNewGroupChatClick)
        SectionHeaderItemView(textId = sharedR.string.chat_explorer_recent_chats_header)
        EmptyStateView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(CHAT_EXPLORER_EMPTY_TAG),
            imagePainter = painterResource(id = iconPackR.drawable.ic_user_glass),
            title = stringResource(sharedR.string.contacts_empty_title),
        )
    }
}

@Composable
private fun NewGroupChatItemView(onClick: () -> Unit) {
    FlexibleLineListItem(
        modifier = Modifier.testTag(CHAT_EXPLORER_NEW_GROUP_TAG),
        title = stringResource(sharedR.string.general_new_group_chat),
        enableClick = true,
        onClickListener = onClick,
        leadingElement = {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.MessageChatCircle,
                    contentDescription = null,
                    tint = IconColor.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailingElement = {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ChevronRight,
                contentDescription = null,
                tint = IconColor.Secondary,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

@Composable
private fun SectionHeaderItemView(@StringRes textId: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = stringResource(textId),
            style = MaterialTheme.typography.titleSmall,
            textColor = TextColor.Secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatExplorerPreview() {
    AndroidThemeForPreviews {
        ChatExplorerContent(
            onNewGroupChatClick = {},
        )
    }
}

internal const val CHAT_EXPLORER_LIST_TAG = "chat_explorer:list"
internal const val CHAT_EXPLORER_EMPTY_TAG = "chat_explorer:empty"
internal const val CHAT_EXPLORER_NEW_GROUP_TAG = "chat_explorer:new_group"
