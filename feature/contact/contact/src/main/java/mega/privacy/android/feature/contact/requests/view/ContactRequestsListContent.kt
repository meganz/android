package mega.privacy.android.feature.contact.requests.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.shared.contact.components.ContactItemView

/**
 * Content for a single contact requests tab: a list of request rows, or the tab's empty state.
 *
 * @param tab The tab whose content is being rendered; drives the empty-state variant.
 * @param requests The requests to render for this tab.
 * @param isSearching Whether a search query is filtering [requests]; the tab's empty state
 * describes having no requests at all, so it is suppressed while a filter is active.
 * @param contentPadding Padding applied to the list content.
 * @param onItemClick Invoked when a request row is tapped.
 * @param modifier
 */
@Composable
internal fun ContactRequestsListContent(
    tab: ContactRequestTab,
    requests: ImmutableList<ContactRequestUiItem>,
    isSearching: Boolean,
    contentPadding: PaddingValues,
    onItemClick: (ContactRequestUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (requests.isEmpty()) {
        if (!isSearching) {
            ContactRequestsEmptyView(
                tab = tab,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .testTag(CONTACT_REQUESTS_EMPTY_TAG),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_REQUESTS_LIST_TAG),
        contentPadding = contentPadding,
    ) {
        items(
            items = requests,
            key = { request -> request.handle },
        ) { request ->
            ContactItemView(
                displayName = request.contact.displayName,
                statusText = request.createdTime,
                status = ContactItemStatus.Unknown,
                avatar = request.contact.avatar,
                isVerified = request.contact.isVerified,
                onClick = { onItemClick(request) },
            )
        }
    }
}
