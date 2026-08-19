package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Content of one chat list tab: the chat room rows, an empty state view, or a
 * no-search-results view.
 *
 * @param state Content state of the tab.
 * @param isMeetingsTab Whether this tab shows meetings; drives the empty state content.
 * @param onItemClick Callback when a row is clicked, with the chat id.
 * @param modifier [Modifier]
 * @param contentPadding Padding for the list content.
 * @param onItemLongClick Optional callback when a row is long-pressed, with the chat id.
 */
@Composable
internal fun ChatListContent(
    state: ChatListTabState,
    isMeetingsTab: Boolean,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onItemLongClick: ((Long) -> Unit)? = null,
) {
    when (state) {
        ChatListTabState.Empty -> ChatListEmptyView(
            isMeetingsTab = isMeetingsTab,
            modifier = modifier,
        )

        ChatListTabState.NoSearchResults -> ChatListNoResultsView(
            isMeetingsTab = isMeetingsTab,
            modifier = modifier,
        )

        is ChatListTabState.Results -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag(if (isMeetingsTab) MEETING_LIST_TAG else CHAT_LIST_TAG),
            contentPadding = contentPadding,
        ) {
            items(items = state.items, key = ChatRoomUiItem::chatId) { item ->
                ChatRoomItemView(
                    item = item,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
        }
    }
}

@Composable
private fun ChatListEmptyView(
    isMeetingsTab: Boolean,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(if (isMeetingsTab) MEETING_LIST_EMPTY_TAG else CHAT_LIST_EMPTY_TAG),
        contentAlignment = Alignment.Center,
    ) {
        EmptyStateView(
            illustration = if (isMeetingsTab) {
                iconPackR.drawable.ic_video_glass
            } else {
                iconPackR.drawable.ic_message_call_glass
            },
            title = stringResource(
                if (isMeetingsTab) {
                    sharedR.string.meeting_recent_list_empty_title
                } else {
                    sharedR.string.chat_recent_list_empty_title
                }
            ),
            description = stringResource(
                if (isMeetingsTab) {
                    sharedR.string.meeting_recent_list_empty_subtitle
                } else {
                    sharedR.string.chat_recent_list_empty_subtitle
                }
            ),
            descriptionSpanStyles = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    MegaSpanStyle.LinkColorStyle(
                        SpanStyle(),
                        LinkColor.Primary,
                    ),
                    LEARN_MORE_URL,
                ),
            ),
            onDescriptionAnnotationClick = { uriHandler.openUri(LEARN_MORE_URL) },
        )
    }
}

@Composable
private fun ChatListNoResultsView(
    isMeetingsTab: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(if (isMeetingsTab) MEETING_LIST_NO_RESULTS_TAG else CHAT_LIST_NO_RESULTS_TAG),
        contentAlignment = Alignment.Center,
    ) {
        EmptyStateView(
            illustration = iconPackR.drawable.ic_search_02,
            title = stringResource(sharedR.string.chat_list_search_no_results_title),
            description = stringResource(sharedR.string.chat_list_search_no_results_description),
        )
    }
}

private const val LEARN_MORE_URL = "https://mega.io/chatandmeetings"

internal const val CHAT_LIST_TAG = "chat_list_content:chat_list"
internal const val MEETING_LIST_TAG = "chat_list_content:meeting_list"
internal const val CHAT_LIST_EMPTY_TAG = "chat_list_content:chat_empty"
internal const val MEETING_LIST_EMPTY_TAG = "chat_list_content:meeting_empty"
internal const val CHAT_LIST_NO_RESULTS_TAG = "chat_list_content:chat_no_results"
internal const val MEETING_LIST_NO_RESULTS_TAG = "chat_list_content:meeting_no_results"

@CombinedThemePreviews
@Composable
private fun ChatListContentEmptyPreview() {
    AndroidThemeForPreviews {
        ChatListContent(
            state = ChatListTabState.Empty,
            isMeetingsTab = false,
            onItemClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeetingListContentEmptyPreview() {
    AndroidThemeForPreviews {
        ChatListContent(
            state = ChatListTabState.Empty,
            isMeetingsTab = true,
            onItemClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListContentNoResultsPreview() {
    AndroidThemeForPreviews {
        ChatListContent(
            state = ChatListTabState.NoSearchResults,
            isMeetingsTab = false,
            onItemClick = {},
        )
    }
}
