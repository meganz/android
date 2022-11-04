package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.presentation.theme.AndroidTheme

@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoState,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            /*SearchAppBar(
                searchWidgetState = state.searchWidgetState,
                typedSearch = state.typedSearch,
                onSearchTextChange = { typedSearch -> onSearchTextChange(typedSearch) },
                onCloseClicked = onCloseSearchClicked,
                onBackPressed = onBackPressed,
                onSearchClicked = onSearchClicked,
                elevation = !firstItemVisible,
                titleId = R.string.group_chat_start_conversation_label,
                hintId = R.string.hint_action_search
            )*/
        }
    ) { paddingValues ->
        LazyColumn(state = listState,
            modifier = Modifier.padding(paddingValues)) {

            state.apply {
                items(buttons) { button ->
                    //ActionButton(action = button, onButtonClicked = onButtonClicked)
                }

                val contactsList = contactItemList

                when {
                    contactsList.isNotEmpty() -> {
                        item(key = "Participants") { ParticipantsHeader() }

                        /* val defaultAvatarContent = contactsList[0].getAvatarFirstLetter()

                         header = defaultAvatarContent

                         item(key = contactsList[0].handle.hashCode()) {
                             HeaderItem(text = defaultAvatarContent)
                         }*/
                    }
                }

                /*contactsList.forEach { contact ->
                    val defaultAvatarContent = contact.getAvatarFirstLetter()

                    if (header != defaultAvatarContent) {
                        header = defaultAvatarContent

                        item(key = contact.handle.hashCode()) {
                            HeaderItem(text = defaultAvatarContent)
                        }
                    }

                    item(key = contact.handle) { ContactItemView(contact) { onContactClicked(contact) } }
                }*/
            }
        }
    }

}

@Composable
private fun ParticipantsHeader() {
    Text(modifier = Modifier.padding(start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = 8.dp),
        text = stringResource(id = R.string.participants_chat_label),
        style = MaterialTheme.typography.body2)
}

@Composable
private fun ActionButton(
    action: ScheduledMeetingInfoAction,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    withDivider: Boolean = true,
) {
    /* Column(modifier = Modifier
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
     }*/
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
private fun EmptyContactsView() {
    /*Column(modifier = Modifier
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
    }*/
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(action = ScheduledMeetingInfoAction.MeetingLink, onButtonClicked = {})
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewScheduledMeetingInfoView")
@Composable
fun PreviewScheduledMeetingInfoView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ScheduledMeetingInfoView(
            state = ScheduledMeetingInfoState(),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onBackPressed = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewEmptyContactsView")
@Composable
fun PreviewEmptyContactsView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        EmptyContactsView()
    }
}