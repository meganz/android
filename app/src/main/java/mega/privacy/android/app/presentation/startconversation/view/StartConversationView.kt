package mega.privacy.android.app.presentation.startconversation.view

import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012

/**
 * Composable function that displays the Start Conversation screen.
 */
@Composable
fun StartConversationView(
    state: StartConversationState,
    onContactClicked: (ContactItem) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onInviteContactsClicked: () -> Unit,
    onButtonClicked: (StartConversationAction) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            if (state.contactItemList.isEmpty()) {
                SimpleTopAppBar(
                    titleId = R.string.group_chat_start_conversation_label,
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
                    title = stringResource(R.string.group_chat_start_conversation_label),
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
                if (buttonsVisible) {
                    if (fromChat) {
                        item {
                            ActionButton(
                                action = buttons[0],
                                onButtonClicked = onButtonClicked,
                                withDivider = false
                            )
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
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)
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
                Icon(
                    painter = painterResource(id = action.icon),
                    contentDescription = "${action.name} icon",
                    tint = MaterialTheme.colors.primary
                )
            }

            ActionText(actionText = action.title)
        }
        if (withDivider) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun InviteContactsButton(onInviteContactsClicked: () -> Unit) {
    Row(modifier = Modifier
        .clickable { onInviteContactsClicked() }
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
            painter = painterResource(id = R.drawable.ic_invite_contacts),
            contentDescription = stringResource(id = R.string.invite_contacts) + "icon",
            tint = MaterialTheme.colors.secondary
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
    val isDark = isSystemInDarkTheme()
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
                painter = painterResource(id = if (isDark) IconR.drawable.ic_empty_user_dark else IconR.drawable.ic_empty_user),
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
            textId = sharedR.string.invite_contacts_action_label,
            onClick = onInviteContactsClicked,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewActionButton() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(action = StartConversationAction.NewGroup, onButtonClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewInviteContactsButton() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        InviteContactsButton(onInviteContactsClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewHeaderItem() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        HeaderItem(text = "A")
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStartConversationView() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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


@CombinedThemePreviews
@Composable
private fun PreviewEmptyContactsView() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        EmptyContactsView(onInviteContactsClicked = {})
    }
}
