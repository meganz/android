package mega.privacy.android.app.presentation.contact.view


import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.core.ui.controls.text.MarqueeText
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * View to show a contactItem with their avatar and connection status
 * @param contactItem that will be shown
 * @param onClick is invoked when the view is clicked
 * @param modifier
 * @param statusOverride to allow change the status text, if null connection status description will be shown
 * @param includeDivider to show or not a divider at the bottom of the view, default is true
 */
@Composable
internal fun ContactItemView(
    contactItem: ContactItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusOverride: String? = null,
    selected: Boolean = false,
    includeDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatarVerified(contactItem, selected = selected)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val contactName = with(contactItem) {
                        contactData.alias ?: contactData.fullName ?: email
                    }

                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (contactItem.status != UserChatStatus.Invalid) {
                        ContactStatus(status = contactItem.status)
                    }
                }

                val secondLineText = statusOverride
                    ?: if (contactItem.lastSeen != null || contactItem.status != UserChatStatus.Invalid) {
                        val statusText = stringResource(id = contactItem.status.text)
                        if (contactItem.status == UserChatStatus.Online) {
                            statusText
                        } else {
                            getLastSeenString(contactItem.lastSeen) ?: statusText
                        }
                    } else null
                secondLineText?.let {
                    MarqueeText(
                        text = secondLineText,
                        style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary)
                    )
                }
            }
        }
        if (includeDivider) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

@Composable
internal fun getLastSeenString(lastGreen: Int?): String? {
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

            dateFormat = SimpleDateFormat(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd MMM"),
                Locale.getDefault()
            )
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
    status: UserChatStatus,
) {
    val statusIcon = status.iconRes(MaterialTheme.colors.isLight)

    Image(
        modifier = modifier.padding(start = 5.dp, top = 2.dp),
        painter = painterResource(id = statusIcon),
        contentDescription = "Contact status"
    )
}

@Composable
private fun ContactAvatar(
    modifier: Modifier = Modifier,
    contactItem: ContactItem,
) {
    val avatarUri = contactItem.contactData.avatarUri

    if (avatarUri != null) {
        UriAvatar(modifier = modifier, uri = avatarUri)
    } else {
        DefaultContactAvatar(
            modifier = modifier,
            color = Color(contactItem.defaultAvatarColor?.toColorInt() ?: -1),
            content = contactItem.getAvatarFirstLetter()
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ContactAvatarVerified(
    contactItem: ContactItem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier,
    ) {
        val avatarModifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = if (contactItem.areCredentialsVerified) 16.dp else 8.dp
            )
            .size(40.dp)
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                scaleIn(animationSpec = tween(220)) with scaleOut(animationSpec = tween(90))
            },
            label = ""
        ) {
            if (selected) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chat_avatar_select),
                    contentDescription = stringResource(id = R.string.selected_items, 1),
                    modifier = avatarModifier,
                )
            } else {
                ContactAvatar(
                    contactItem = contactItem,
                    modifier = avatarModifier.clip(CircleShape),
                )
            }
        }
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
}

@Composable
internal fun UriAvatar(modifier: Modifier, uri: String) {
    Image(
        modifier = modifier,
        painter = rememberAsyncImagePainter(model = uri),
        contentDescription = "User avatar"
    )
}

@Composable
internal fun DefaultContactAvatar(modifier: Modifier = Modifier, color: Color, content: String) {
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

@CombinedTextAndThemePreviews
@Composable
private fun PreviewContactItem() {
    ContactItemView(
        contactItem = contactItemForPreviews,
        onClick = {}
    )
}

internal val contactItemForPreviews get() = contactItemForPreviews(-1)
internal fun contactItemForPreviews(id: Int) = ContactItem(
    handle = id.toLong(),
    email = "email$id@mega.nz",
    contactData = ContactData("Full name $id", "Alias $id", null),
    defaultAvatarColor = "blue",
    visibility = UserVisibility.Visible,
    timestamp = 2345262L,
    areCredentialsVerified = false,
    status = UserChatStatus.Online,
    lastSeen = null
)