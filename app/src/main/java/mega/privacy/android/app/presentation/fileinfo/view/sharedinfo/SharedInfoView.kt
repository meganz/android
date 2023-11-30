package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState.Companion.MAX_NUMBER_OF_CONTACTS_IN_LIST
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHARES_HEADER
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHOW_MORE
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Expandable list of shared contacts
 */
@Composable
internal fun SharedInfoView(
    contacts: List<ContactPermission>,
    selectedContacts: List<String>,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    onContactClick: (ContactPermission) -> Unit,
    onContactLongClick: (ContactPermission) -> Unit,
    onContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreContactsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Header(contacts, expanded, onHeaderClick)
        Column(modifier = modifier.animateContentSize()) {
            if (expanded) {
                ContactsList(
                    contacts,
                    selectedContacts,
                    onContactClick,
                    onContactLongClick,
                    onContactMoreOptionsClick,
                    onShowMoreContactsClick
                )
            }
        }
    }
}

@Composable
private fun Header(
    contacts: List<ContactPermission>,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onHeaderClick)
            .height(56.dp)
            .padding(start = 72.dp)
            .fillMaxWidth()
            .testTag(TEST_TAG_SHARES_HEADER),
    ) {
        Text(
            text = stringResource(id = R.string.file_properties_shared_folder_select_contact),
            style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.textColorPrimary)
        )
        TextMegaButton(
            text =
            if (expanded) {
                stringResource(id = R.string.general_close)
            } else {
                pluralStringResource(
                    id = R.plurals.general_selection_num_contacts,
                    count = contacts.size,
                    contacts.size
                )
            },
            modifier = Modifier
                .padding(end = 16.dp),
            onClick = onHeaderClick
        )
    }
}

@Composable
private fun ColumnScope.ContactsList(
    contacts: List<ContactPermission>,
    selectedContacts: List<String>,
    onContactClick: (ContactPermission) -> Unit,
    onContactLongClick: (ContactPermission) -> Unit,
    onMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreContactsClick: () -> Unit,
) {
    //maximum 5, so no LazyColumn needed
    contacts.take(MAX_CONTACTS_TO_SHOW).forEachIndexed { i, contactItem ->
        SharedInfoContactItemView(
            contactItem = contactItem,
            selected = selectedContacts.contains(contactItem.contactItem.email),
            onClick = { onContactClick(contactItem) },
            onMoreOptionsClick = { onMoreOptionsClick(contactItem) },
            onLongClick = { onContactLongClick(contactItem) },
        )
        if (i < contacts.size - 1) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
    (contacts.size - MAX_CONTACTS_TO_SHOW).takeIf { it > 0 }?.let { extra ->
        Text(
            text = "$extra ${stringResource(id = R.string.label_more)}",
            modifier = Modifier
                .padding(2.dp)
                .clickable(onClick = onShowMoreContactsClick)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .align(Alignment.End)
                .testTag(TEST_TAG_SHOW_MORE),
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary),
        )
    }
}

internal const val MAX_CONTACTS_TO_SHOW = MAX_NUMBER_OF_CONTACTS_IN_LIST

/**
 * Preview for [SharedInfoView]
 */
@CombinedThemePreviews
@Composable
private fun SharedInfoPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        var expanded by remember { mutableStateOf(true) }
        SharedInfoView(
            contacts = List(7) {
                ContactPermission(
                    contactItemForPreviews,
                    AccessPermission.values()[it.mod(AccessPermission.values().size)]
                )
            },
            selectedContacts = emptyList(),
            expanded = expanded,
            onHeaderClick = { expanded = !expanded },
            onContactClick = {},
            onContactLongClick = {},
            onContactMoreOptionsClick = {},
            onShowMoreContactsClick = {},
        )
    }
}