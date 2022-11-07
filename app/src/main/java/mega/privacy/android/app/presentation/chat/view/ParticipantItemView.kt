package mega.privacy.android.app.presentation.chat.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.presentation.controls.MarqueeText
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_038
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_038
import mega.privacy.android.presentation.theme.white_alpha_054
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ParticipantItemView(participant: ContactItem, onClick: () -> Unit) {
    Column {
        Row(modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier
                .weight(1f)) {
                Box {
                    ContactAvatar(contactItem = participant)
                }

                Column(modifier = Modifier
                    .align(Alignment.CenterVertically)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val contactName = with(participant) {
                            contactData.alias ?: contactData.fullName ?: email
                        }

                        Text(text = contactName,
                            style = MaterialTheme.typography.subtitle1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)

                        if (participant.status != UserStatus.Invalid) {
                            ContactStatus(status = participant.status)
                        }
                    }

                    if (participant.lastSeen != null || participant.status != UserStatus.Invalid) {
                        val statusText = stringResource(id = participant.status.text)
                        val secondLineText =
                            if (participant.status == UserStatus.Online) {
                                statusText
                            } else {
                                getLastSeenString(participant.lastSeen) ?: statusText
                            }

                        MarqueeText(text = secondLineText,
                            color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                            style = MaterialTheme.typography.subtitle2)
                    }
                }
            }

            Box(modifier = Modifier
                .wrapContentSize(Alignment.CenterEnd)) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_permissions_full_access),
                        contentDescription = "Permissions icon",
                        tint = if (MaterialTheme.colors.isLight) grey_alpha_038 else white_alpha_038)

                    Icon(modifier = Modifier.padding(start = 30.dp),
                        painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                        contentDescription = "Three dots icon",
                        tint = if (MaterialTheme.colors.isLight) grey_alpha_038 else white_alpha_038)
                }
            }
        }

        Divider(modifier = Modifier.padding(start = 72.dp),
            color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
            thickness = 1.dp)
    }
}

@Composable
fun getLastSeenString(lastGreen: Int?): String? {
    if (lastGreen == null) return null

    val lastGreenCalendar = Calendar.getInstance().apply { add(Calendar.MINUTE, -lastGreen) }
    val timeToConsiderAsLongTimeAgo = 65535

    Timber.d("Ts last green: %s", lastGreenCalendar.timeInMillis)

    return when {
        lastGreen >= timeToConsiderAsLongTimeAgo -> {
            stringResource(id = R.string.last_seen_long_time_ago)
        }
        compareLastSeenWithToday(lastGreenCalendar) == 0 -> {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_today, time)
        }
        else -> {
            var dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = lastGreenCalendar.timeZone
            }
            val time = dateFormat.format(lastGreenCalendar.time)
            dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val day = dateFormat.format(lastGreenCalendar.time)
            stringResource(R.string.last_seen_general, day, time)
        }
    }.replace("[A]", "").replace("[/A]", "")
}

private fun compareLastSeenWithToday(lastGreen: Calendar): Int {
    val today = Calendar.getInstance()

    return when {
        lastGreen.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
            lastGreen.get(Calendar.YEAR) - today.get(Calendar.YEAR)
        }
        lastGreen.get(Calendar.MONTH) != today.get(Calendar.MONTH) -> {
            lastGreen.get(Calendar.MONTH) - today.get(Calendar.MONTH)
        }
        else -> {
            lastGreen.get(Calendar.DAY_OF_MONTH) - today.get(Calendar.DAY_OF_MONTH)
        }
    }
}

@Composable
fun ContactStatus(
    modifier: Modifier = Modifier.padding(start = 5.dp, top = 2.dp),
    status: UserStatus,
) {
    val isLightTheme = MaterialTheme.colors.isLight
    val statusIcon = when (status) {
        UserStatus.Online ->
            if (isLightTheme) R.drawable.ic_online_light
            else R.drawable.ic_online_dark_standard
        UserStatus.Away ->
            if (isLightTheme) R.drawable.ic_away_light
            else R.drawable.ic_away_dark_standard
        UserStatus.Busy ->
            if (isLightTheme) R.drawable.ic_busy_light
            else R.drawable.ic_busy_dark_standard
        else ->
            if (isLightTheme) R.drawable.ic_offline_light
            else R.drawable.ic_offline_dark_standard
    }

    Image(modifier = modifier,
        painter = painterResource(id = statusIcon),
        contentDescription = "Contact status")
}

@Composable
fun ContactAvatar(
    modifier: Modifier = Modifier
        .padding(16.dp)
        .size(40.dp)
        .clip(CircleShape),
    contactItem: ContactItem,
) {
    val avatarUri = contactItem.contactData.avatarUri

    if (avatarUri != null) {
        UriAvatar(modifier = modifier, uri = avatarUri)
    } else {
        DefaultContactAvatar(modifier = modifier,
            color = Color(contactItem.defaultAvatarColor.toColorInt()),
            content = contactItem.getAvatarFirstLetter())
    }
}

@Composable
fun UriAvatar(modifier: Modifier, uri: String) {
    Image(modifier = modifier,
        painter = rememberAsyncImagePainter(model = uri),
        contentDescription = "User avatar")
}

@Composable
fun DefaultContactAvatar(modifier: Modifier = Modifier.size(40.dp), color: Color, content: String) {
    Box(contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = color, shape = CircleShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = placeable.height
                var heightCircle = currentHeight
                if (placeable.width > heightCircle)
                    heightCircle = placeable.width

                layout(heightCircle, heightCircle) {
                    placeable.placeRelative(0, (heightCircle - currentHeight) / 2)
                }
            }) {
        Text(
            text = content,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.h6
        )
    }
}

@Preview
@Composable
fun PreviewContactItem() {
    ParticipantItemView(participant = ContactItem(
        handle = -1,
        email = "email@mega.nz",
        contactData = ContactData("Full name", "Alias", null),
        defaultAvatarColor = "",
        visibility = UserVisibility.Visible,
        timestamp = 2345262L,
        areCredentialsVerified = true,
        status = UserStatus.Online,
        lastSeen = null)) { }
}