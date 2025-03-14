package mega.privacy.android.app.presentation.startconversation.view

import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as sharedR
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.search.view.EmptySearchView
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.meeting.chat.view.NoteToSelfView
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider

/**
 * Composable function that displays the Start Conversation screen.
 */
@Composable
fun StartConversationView(
    state: StartConversationState,
    noteToSelfChatUIState: NoteToSelfChatUIState,
    onContactClicked: (ContactItem) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    onInviteContactsClicked: () -> Unit,
    onNoteToSelfClicked: () -> Unit,
    onButtonClicked: (StartConversationAction) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            if (state.contactItemList.isEmpty()) {
                SimpleTopAppBar(
                    titleId = R.string.fab_label_new_chat,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed
                )
            } else {
                LegacySearchAppBar(
                    searchWidgetState = state.searchWidgetState,
                    typedSearch = state.typedSearch,
                    onSearchTextChange = { typedSearch -> onSearchTextChange(typedSearch) },
                    onCloseClicked = onCloseSearchClicked,
                    onBackPressed = onBackPressed,
                    onSearchClicked = onSearchClicked,
                    elevation = !firstItemVisible,
                    title = stringResource(R.string.fab_label_new_chat),
                    hintId = R.string.hint_action_search
                )
            }
        }
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

                        if (noteToSelfChatUIState.isNoteToYourselfFeatureFlagEnabled) {
                            item(key = "Note to self") {
                                NoteToSelfView(
                                    onNoteToSelfClicked,
                                    isHint = noteToSelfChatUIState.isNoteToSelfChatEmpty,
                                    isNew = noteToSelfChatUIState.isNewFeature
                                )
                            }
                        }

                        val defaultAvatarContent = contactsList[0].getAvatarFirstLetter()

                        header = defaultAvatarContent

                        item(key = contactsList[0].handle.hashCode()) {
                            HeaderItem(text = defaultAvatarContent)
                        }
                    }

                    typedSearch.isNotEmpty() -> {
                        item(key = "Empty search") { EmptySearchView() }
                    }

                    else -> {
                        item(key = "Contacts header") { ContactsHeader() }
                        if (noteToSelfChatUIState.isNoteToYourselfFeatureFlagEnabled) {
                            item(key = "Note to self") {
                                NoteToSelfView(
                                    onNoteToSelfClicked,
                                    isHint = noteToSelfChatUIState.isNoteToSelfChatEmpty,
                                    isNew = noteToSelfChatUIState.isNewFeature
                                )
                            }
                        }
                        item(key = "Empty contacts") { EmptyContactsView(onInviteContactsClicked) }
                    }
                }

                contactsList.forEach { contact ->
                    val defaultAvatarContent = contact.getAvatarFirstLetter()

                    if (header != defaultAvatarContent) {
                        header = defaultAvatarContent

                        item(key = contact.handle.hashCode()) {
                            HeaderItem(text = defaultAvatarContent)
                        }
                    }

                    item(key = contact.handle) {
                        ContactItemView(contact, onClick = { onContactClicked(contact) })
                    }
                }
            }
        }

        if (state.error != null) {
            val error = stringResource(id = state.error)
            LaunchedEffect(scaffoldState.snackbarHostState) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)
}

@Composable
private fun ContactsHeader() {
    Text(
        modifier = Modifier.padding(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 8.dp
        ),
        text = stringResource(id = R.string.section_contacts),
        style = MaterialTheme.typography.body2
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
        Row(modifier = Modifier
            .clickable { onButtonClicked(action) }
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            MegaIcon(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
                painter = painterResource(id = action.icon),
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
    Row(modifier = Modifier
        .clickable { onInviteContactsClicked() }
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        MegaIcon(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            painter = painterResource(id = R.drawable.ic_invite_contacts),
            contentDescription = stringResource(id = R.string.invite_contacts) + "icon",
            tint = IconColor.Primary
        )

        ActionText(actionText = R.string.invite_contacts)
    }
}

@Composable
private fun ActionText(actionText: Int) {
    Text(
        modifier = Modifier.padding(end = 8.dp),
        style = MaterialTheme.typography.subtitle2,
        fontWeight = FontWeight.Medium,
        text = stringResource(id = actionText),
        color = MaterialTheme.colors.secondary
    )
}

@Composable
private fun HeaderItem(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        text = text,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.subtitle2
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
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.W500),
            textColor = TextColor.Primary
        )

        MegaText(
            modifier = Modifier.padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 16.dp),
            text = stringResource(id = sharedR.string.invite_contacts_to_start_chat_subtitle),
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.W400,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center,
            textColor = TextColor.Secondary,
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