package mega.privacy.android.app.presentation.startconversation.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.search.view.EmptySearchView
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.core.ui.controls.SearchAppBar
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.domain.entity.contacts.ContactItem

@Composable
fun StartConversationView(
    state: StartConversationState,
    onButtonClicked: (StartConversationAction) -> Unit = {},
    onContactClicked: (ContactItem) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onInviteContactsClicked: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            if (state.contactItemList.isEmpty()) {
                SimpleTopAppBar(titleId = R.string.group_chat_start_conversation_label,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed)
            } else {
                SearchAppBar(
                    searchWidgetState = state.searchWidgetState,
                    typedSearch = state.typedSearch,
                    onSearchTextChange = { typedSearch -> onSearchTextChange(typedSearch) },
                    onCloseClicked = onCloseSearchClicked,
                    onBackPressed = onBackPressed,
                    onSearchClicked = onSearchClicked,
                    elevation = !firstItemVisible,
                    titleId = R.string.group_chat_start_conversation_label,
                    hintId = R.string.hint_action_search
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(state = listState,
            modifier = Modifier.padding(paddingValues)) {
            state.apply {
                if (buttonsVisible) {
                    if (fromChat) {
                        item {
                            ActionButton(action = buttons[0],
                                onButtonClicked = onButtonClicked,
                                withDivider = false)
                        }
                    } else {
                        items(buttons) { button ->
                            ActionButton(action = button, onButtonClicked = onButtonClicked)
                        }
                    }
                }

                val contactsList = filteredContactList ?: contactItemList
                var header = ""

                when {
                    contactsList.isNotEmpty() -> {
                        item(key = "Contacts header") { ContactsHeader() }

                        if (buttonsVisible) {
                            item(key = "Invite contacts") {
                                InviteContactsButton(onInviteContactsClicked)
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
                scaffoldState.snackbarHostState.showSnackbar(message = error,
                    duration = SnackbarDuration.Long)
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)
}

@Composable
private fun ContactsHeader() {
    Text(modifier = Modifier.padding(start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = 8.dp),
        text = stringResource(id = R.string.section_contacts),
        style = MaterialTheme.typography.body2)
}

@Composable
private fun ActionButton(
    action: StartConversationAction,
    onButtonClicked: (StartConversationAction) -> Unit = {},
    withDivider: Boolean = true,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onButtonClicked(action) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colors.secondary)
                    .wrapContentSize(Alignment.Center)

            ) {
                Icon(painter = painterResource(id = action.icon),
                    contentDescription = "${action.name} icon",
                    tint = MaterialTheme.colors.primary)
            }

            ActionText(actionText = action.title)
        }
        if (withDivider) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
                thickness = 1.dp)
        }
    }
}

@Composable
private fun InviteContactsButton(onInviteContactsClicked: () -> Unit) {
    Row(modifier = Modifier
        .clickable { onInviteContactsClicked() }
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            painter = painterResource(id = R.drawable.ic_invite_contacts),
            contentDescription = stringResource(id = R.string.invite_contacts) + "icon",
            tint = MaterialTheme.colors.secondary)

        ActionText(actionText = R.string.invite_contacts)
    }
}

@Composable
private fun ActionText(actionText: Int) {
    Text(modifier = Modifier.padding(end = 8.dp),
        style = MaterialTheme.typography.subtitle2,
        fontWeight = FontWeight.Medium,
        text = stringResource(id = actionText),
        color = MaterialTheme.colors.secondary)
}

@Composable
private fun HeaderItem(text: String) {
    Text(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        text = text,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.subtitle2)
}

@Composable
private fun EmptyContactsView(onInviteContactsClicked: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Spacer(modifier = Modifier.height(50.dp))
        }

        Image(painter = painterResource(id = R.drawable.ic_empty_contact_list),
            contentDescription = "Empty contacts image",
            alpha = if (MaterialTheme.colors.isLight) 1f else 0.16f)

        Spacer(modifier = Modifier.height(10.dp))

        Text(modifier = Modifier.padding(horizontal = 10.dp),
            text = "No contacts",
            style = MaterialTheme.typography.subtitle1,
            color = if (MaterialTheme.colors.isLight) Color.Black else Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        Text(modifier = Modifier.padding(horizontal = 10.dp),
            text = stringResource(id = R.string.invite_contacts_to_start_chat),
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onInviteContactsClicked,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.secondary),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.secondary)
        ) {
            Text(text = stringResource(id = R.string.invite_contacts),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.secondary)
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(action = StartConversationAction.NewGroup, onButtonClicked = {})
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkInviteContactsButton")
@Composable
fun PreviewInviteContactsButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        InviteContactsButton(onInviteContactsClicked = {})
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewHeaderItem")
@Composable
fun PreviewHeaderItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        HeaderItem(text = "A")
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewStartConversationView")
@Composable
fun PreviewStartConversationView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        StartConversationView(
            state = StartConversationState(),
            onButtonClicked = {},
            onContactClicked = {},
            onSearchTextChange = {},
            onCloseSearchClicked = {},
            onBackPressed = {},
            onSearchClicked = {},
            onScrollChange = {},
            onInviteContactsClicked = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewEmptyContactsView")
@Composable
fun PreviewEmptyContactsView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        EmptyContactsView(onInviteContactsClicked = {})
    }
}