package mega.privacy.android.feature.contact.list.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Contact list content
 *
 * @param state
 * @param searchActive
 * @param contentPadding
 * @param onContactClick
 * @param onContactMore
 * @param onRequestsClick
 * @param onGroupsClick
 * @param onContactInfoClick
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactListContent(
    state: ContactListUiState.Data,
    searchActive: Boolean,
    contentPadding: PaddingValues,
    onContactClick: (Long) -> Unit,
    onContactMore: (ContactItemUiState) -> Unit,
    onRequestsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onContactInfoClick: (email: String) -> Unit,
) {
    val isEmpty = state.contacts.isEmpty()

    if (isEmpty && !searchActive) {
        EmptyContactsView(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .testTag(CONTACT_LIST_EMPTY_TAG),
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CONTACT_LIST_LAZY_COLUMN_TAG),
        contentPadding = contentPadding,
    ) {
        if (!searchActive) {
            item(key = "requests_action") {
                ContactActionItem(
                    iconPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.UserPlus),
                    label = stringResource(sharedR.string.contacts_section_requests),
                    badgeCount = state.incomingRequestCount,
                    testTag = CONTACT_LIST_REQUESTS_ACTION_TAG,
                    onClick = onRequestsClick,
                )
            }
            item(key = "groups_action") {
                ContactActionItem(
                    iconPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Users),
                    label = stringResource(sharedR.string.contacts_section_groups),
                    badgeCount = 0,
                    testTag = CONTACT_LIST_GROUPS_ACTION_TAG,
                    onClick = onGroupsClick,
                )
            }

            if (state.recentlyAddedContacts.isNotEmpty()) {
                item(key = "recently_added_header") {
                    SectionHeader(
                        text = stringResource(sharedR.string.contacts_section_recently_added),
                        modifier = Modifier.testTag(CONTACT_LIST_RECENT_HEADER_TAG),
                    )
                }
                item(key = "recently_added_row") {
                    RecentlyAddedRow(
                        contacts = state.recentlyAddedContacts,
                        onContactClick = onContactClick,
                        onContactInfoClick = onContactInfoClick,
                    )
                }
            }
        }

        state.contacts.forEach { (initial, contacts) ->
            stickyHeader(key = "header_$initial") {
                SectionHeader(
                    text = initial,
                    modifier = Modifier.testTag(contactGroupHeaderTag(initial)),
                )
            }
            items(
                items = contacts,
                key = { contact -> contact.handle },
            ) { contact ->
                ContactItemView(
                    contactItemUiState = contact,
                    onClick = { onContactClick(contact.handle) },
                    onMoreClicked = { onContactMore(contact) },
                    onAvatarClick = { onContactInfoClick(contact.email) }
                )
            }
        }
    }
}

/**
 * Contact action item
 *
 * @param iconPainter
 * @param label
 * @param badgeCount
 * @param testTag
 * @param onClick
 */
@Composable
private fun ContactActionItem(
    iconPainter: Painter,
    label: String,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaIcon(painter = iconPainter, contentDescription = null, tint = IconColor.Primary)
        MegaText(
            text = label,
            modifier = Modifier.weight(1f),
            textColor = TextColor.Primary,
        )
        if (badgeCount > 0) {
            MegaText(
                text = badgeCount.toString(),
                modifier = Modifier.testTag("$testTag:badge"),
                textColor = TextColor.Accent,
            )
        }
    }
}

/**
 * Section header
 *
 * @param text
 * @param modifier
 */
@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    BoxSurface(
        surfaceColor = SurfaceColor.PageBackground,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        MegaText(
            text = text,
            textColor = TextColor.Secondary,
        )
    }
}

/**
 * Recently added row
 *
 * @param contacts
 * @param onContactClick
 * @param onContactInfoClick
 */
@Composable
private fun RecentlyAddedRow(
    contacts: ImmutableList<ContactItemUiState>,
    onContactClick: (Long) -> Unit,
    onContactInfoClick: (email: String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CONTACT_LIST_RECENT_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(items = contacts, key = { "${it.handle}_recent" }) { contact ->
            Box(modifier = Modifier.width(240.dp)) {
                ContactItemView(
                    contactItemUiState = contact,
                    onClick = { onContactClick(contact.handle) },
                    onAvatarClick = { onContactInfoClick(contact.email) }
                )
            }
        }
    }
}

/**
 * Empty contacts view
 *
 * @param modifier
 */
@Composable
private fun EmptyContactsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MegaText(
            text = stringResource(sharedR.string.contacts_empty_title),
            textColor = TextColor.Secondary,
        )
    }
}

internal const val CONTACT_LIST_EMPTY_TAG = "contact_list_screen:empty"
internal const val CONTACT_LIST_LAZY_COLUMN_TAG = "contact_list_screen:lazy_column"
internal const val CONTACT_LIST_REQUESTS_ACTION_TAG = "contact_list_screen:requests_action"
internal const val CONTACT_LIST_GROUPS_ACTION_TAG = "contact_list_screen:groups_action"
internal const val CONTACT_LIST_RECENT_ROW_TAG = "contact_list_screen:recent_row"
internal const val CONTACT_LIST_RECENT_HEADER_TAG = "contact_list_screen:recent_header"