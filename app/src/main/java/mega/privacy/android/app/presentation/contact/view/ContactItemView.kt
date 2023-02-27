package mega.privacy.android.app.presentation.contact


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
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
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.core.ui.controls.MarqueeText
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ContactItemView(contactItem: ContactItem, onClick: () -> Unit) {
    Column {
        Row(modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box {
                ContactAvatar(
                    contactItem = contactItem,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape),
                )
                if (contactItem.areCredentialsVerified) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        painter = painterResource(id = R.drawable.ic_verified),
                        contentDescription = "Verified user"
                    )
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val contactName = with(contactItem) {
                        contactData.alias ?: contactData.fullName ?: email
                    }

                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.subtitle1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (contactItem.status != UserStatus.Invalid) {
                        ContactStatus(status = contactItem.status)
                    }
                }

                if (contactItem.lastSeen != null || contactItem.status != UserStatus.Invalid) {
                    val statusText = stringResource(id = contactItem.status.text)
                    val secondLineText =
                        if (contactItem.status == UserStatus.Online) {
                            statusText
                        } else {
                            getLastSeenString(contactItem.lastSeen) ?: statusText
                        }

                    MarqueeText(
                        text = secondLineText,
                        color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                        style = MaterialTheme.typography.subtitle2
                    )
                }
            }
        }

        Divider(
            modifier = Modifier.padding(start = 72.dp),
            color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
            thickness = 1.dp
        )
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
    modifier: Modifier = Modifier,
    status: UserStatus,
) {
    val statusIcon = status.iconRes(MaterialTheme.colors.isLight)

    Image(
        modifier = modifier.padding(start = 5.dp, top = 2.dp),
        painter = painterResource(id = statusIcon),
        contentDescription = "Contact status"
    )
}

@Composable
fun ContactAvatar(
    modifier: Modifier = Modifier,
    contactItem: ContactItem,
) {
    val avatarUri = contactItem.contactData.avatarUri

    if (avatarUri != null) {
        UriAvatar(modifier = modifier, uri = avatarUri)
    } else {
        DefaultContactAvatar(
            modifier = modifier,
            color = Color(contactItem.defaultAvatarColor.toColorInt()),
            content = contactItem.getAvatarFirstLetter()
        )
    }
}

@Composable
fun UriAvatar(modifier: Modifier, uri: String) {
    Image(
        modifier = modifier,
        painter = rememberAsyncImagePainter(model = uri),
        contentDescription = "User avatar"
    )
}

@Composable
fun DefaultContactAvatar(modifier: Modifier = Modifier, color: Color, content: String) {
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
    ContactItemView(
        contactItem = ContactItem(
            handle = -1,
            email = "email@mega.nz",
            contactData = ContactData("Full name", "Alias", null),
            defaultAvatarColor = "",
            visibility = UserVisibility.Visible,
            timestamp = 2345262L,
            areCredentialsVerified = true,
            status = UserStatus.Online,
            lastSeen = null
        )
    ) { }
}