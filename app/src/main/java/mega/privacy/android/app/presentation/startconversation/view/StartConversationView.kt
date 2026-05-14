package mega.privacy.android.app.presentation.startconversation.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.chat.view.NoteToSelfView
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.app.presentation.search.view.EmptySearchView
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale

/**
 * Composable function that displays the Start Conversation screen.
 */
@Composable
fun StartConversationView(
    state: StartConversationState,
    noteToSelfChatUIState: NoteToSelfChatUIState,
    onContactClicked: (Long) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    onInviteContactsClicked: () -> Unit,
    onNoteToSelfClicked: () -> Unit,
    onButtonClicked: (StartConversationAction) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val title = stringResource(R.string.fab_label_new_chat)

    MegaScaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            if (state.contactItemList.isEmpty()) {
                MegaTopAppBar(
                    title = title,
                    navigationType = AppBarNavigationType.Back(onBackPressed),
                )
            } else {
                MegaSearchTopAppBar(
                    title = title,
                    navigationType = AppBarNavigationType.Back(onBackPressed),
                    query = state.typedSearch,
                    onQueryChanged = onSearchTextChange,
                    searchPlaceholder = stringResource(R.string.hint_action_search),
                    isSearchingMode = state.searchWidgetState == SearchWidgetState.EXPANDED,
                    onSearchingModeChanged = { isSearching ->
                        if (isSearching) onSearchClicked() else onCloseSearchClicked()
                    },
                )
            }
        },
        snackbarHost = {
            MegaSnackbar(snackBarHostState = snackbarHostState)
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(paddingValues)
        ) {
            state.apply {
                val contactsList = filteredContactList ?: contactItemList

                if (buttonsVisible) {
                    val isInviteButtonVisible = contactItemList.isNotEmpty()
                    if (fromChat) {
                        item {
                            ActionButtons(
                                action = buttons[0],
                                onButtonClicked = onButtonClicked,
                                onInviteContactsClicked = onInviteContactsClicked,
                                isInviteButtonVisible = isInviteButtonVisible,
                                withDivider = false
                            )
                        }
                    } else {
                        items(buttons) { button ->
                            ActionButtons(
                                action = button,
                                onButtonClicked = onButtonClicked,
                                onInviteContactsClicked = onInviteContactsClicked,
                                isInviteButtonVisible = isInviteButtonVisible && button == StartConversationAction.JoinMeeting
                            )
                        }
                    }
                }

                var header = ""

                when {
                    contactsList.isNotEmpty() -> {
                        item(key = "Contacts header") { ContactsHeader() }

                        item(key = "Note to self") {
                            NoteToSelfView(
                                onNoteToSelfClicked,
                                isHint = noteToSelfChatUIState.isNoteToSelfChatEmpty,
                            )
                        }

                        val firstHeader = contactsList[0].headerLetter()

                        header = firstHeader

                        item(key = "header_${contactsList[0].handle}") {
                            HeaderItem(text = firstHeader)
                        }
                    }

                    typedSearch.isNotEmpty() -> {
                        item(key = "Empty search") { EmptySearchView() }
                    }

                    else -> {
                        item(key = "Contacts header") { ContactsHeader() }
                        item(key = "Note to self") {
                            NoteToSelfView(
                                onNoteToSelfClicked,
                                isHint = noteToSelfChatUIState.isNoteToSelfChatEmpty,
                            )
                        }
                        item(key = "Empty contacts") { EmptyContactsView(onInviteContactsClicked) }
                    }
                }

                contactsList.forEach { contact ->
                    val rowHeader = contact.headerLetter()

                    if (header != rowHeader) {
                        header = rowHeader

                        item(key = "header_${contact.handle}") {
                            HeaderItem(text = rowHeader)
                        }
                    }

                    item(key = contact.handle) {
                        ContactItemView(
                            contactItemUiState = contact,
                            onClick = { onContactClicked(contact.handle) },
                        )
                    }
                }
            }
        }

        if (state.error != null) {
            val error = stringResource(id = state.error)
            LaunchedEffect(snackbarHostState) {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
}

@Composable
private fun ContactsHeader() {
    MegaText(
        modifier = Modifier.padding(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 8.dp
        ),
        text = stringResource(id = sharedR.string.general_section_contacts),
        textColor = TextColor.Primary,
        style = AppTheme.typography.bodyMedium,
    )
}

@Composable
private fun ActionButtons(
    action: StartConversationAction,
    onButtonClicked: (StartConversationAction) -> Unit = {},
    onInviteContactsClicked: () -> Unit,
    withDivider: Boolean = true,
    isInviteButtonVisible: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onButtonClicked(action) }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            MegaIcon(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
                painter = rememberVectorPainter(action.icon),
                contentDescription = "${action.name} icon",
                tint = IconColor.Primary
            )

            ActionText(actionText = action.title)
        }

        if (withDivider) {
            MegaDivider(dividerType = DividerType.BigStartPadding)
        }

        if (isInviteButtonVisible) {
            InviteContactsButton(onInviteContactsClicked)

            if (withDivider) {
                MegaDivider(dividerType = DividerType.BigStartPadding)
            }
        }
    }
}

@Composable
private fun InviteContactsButton(onInviteContactsClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onInviteContactsClicked() }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        MegaIcon(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.PlusCircle),
            contentDescription = stringResource(id = R.string.invite_contacts) + "icon",
            tint = IconColor.Primary
        )

        ActionText(actionText = R.string.invite_contacts)
    }
}

@Composable
private fun ActionText(actionText: Int) {
    MegaText(
        modifier = Modifier.padding(end = 8.dp),
        text = stringResource(id = actionText),
        textColor = TextColor.Accent,
        style = AppTheme.typography.titleSmall,
    )
}

@Composable
private fun HeaderItem(text: String) {
    MegaText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        text = text,
        textColor = TextColor.Primary,
        style = AppTheme.typography.titleSmall,
    )
}

@Composable
private fun EmptyContactsView(onInviteContactsClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Spacer(modifier = Modifier.height(50.dp))
        }
        val isPortrait =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            Image(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(120.dp),
                painter = painterResource(id = IconR.drawable.ic_user_glass),
                contentDescription = "Empty contacts image",
            )
        }
        MegaText(
            modifier = Modifier.padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 16.dp),
            text = stringResource(id = sharedR.string.invite_contacts_to_start_chat_title),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium,
        )

        MegaText(
            modifier = Modifier.padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 16.dp),
            text = stringResource(id = sharedR.string.invite_contacts_to_start_chat_subtitle),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        RaisedDefaultMegaButton(
            modifier = Modifier
                .testTag(TEST_TAG_RAISED_DEFAULT_MEGA_BUTTON)
                .padding(bottom = 20.dp)
                .align(Alignment.CenterHorizontally),
            textId = sharedR.string.invite_contacts_action_label,
            onClick = onInviteContactsClicked,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewActionButton() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ActionButtons(
            action = StartConversationAction.NewGroup,
            onButtonClicked = {},
            onInviteContactsClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewInviteContactsButton() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        InviteContactsButton(onInviteContactsClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewHeaderItem() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        HeaderItem(text = "A")
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStartConversationView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        StartConversationView(
            state = StartConversationState(),
            noteToSelfChatUIState = NoteToSelfChatUIState(),
            onButtonClicked = {},
            onContactClicked = {},
            onSearchTextChange = {},
            onCloseSearchClicked = {},
            onBackPressed = {},
            onSearchClicked = {},
            onInviteContactsClicked = {},
            onNoteToSelfClicked = {}
        )
    }
}


@CombinedThemePreviews
@Composable
private fun PreviewEmptyContactsView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        EmptyContactsView(onInviteContactsClicked = {})
    }
}

internal const val TEST_TAG_RAISED_DEFAULT_MEGA_BUTTON = "raised_default_mega_button"

private fun ContactItemUiState.headerLetter(): String =
    displayName.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: ""