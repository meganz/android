package mega.privacy.android.app.presentation.contact.view


import mega.privacy.android.icon.pack.R as IconR
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.model.ContactAvatar
import mega.privacy.android.app.presentation.contact.model.ContactStatus
import mega.privacy.android.app.presentation.contact.model.ContactUiItem
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.MarqueeText
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.android.core.ui.theme.values.TextColor
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
 * @param dividerType divider type to be shown at the bottom of the view, null for no divider
 */
@Deprecated(
    message = "Use the ContactItemView function with ContactUiItem parameter",
    replaceWith = ReplaceWith(
        expression = "ContactItemView(contactUiItem, onClick, modifier, statusOverride, selected, dividerType)",
        imports = ["mega.privacy.android.app.presentation.contact.view.ContactItemView"]
    )
)
@Composable
internal fun ContactItemView(
    contactItem: ContactItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusOverride: String? = null,
    selected: Boolean = false,
    dividerType: DividerType? = DividerType.BigStartPadding,
) {

    val status =
        ContactStatus(statusText = if (contactItem.lastSeen != null || contactItem.status != UserChatStatus.Invalid) {
            val statusText = stringResource(id = contactItem.status.text)
            if (contactItem.status == UserChatStatus.Online) {
                statusText
            } else {
                getLastSeenString(contactItem.lastSeen) ?: statusText
            }
        } else null,
            iconPainter = painterResource(contactItem.status.iconRes(MaterialTheme.colors.isLight))
                .takeUnless { contactItem.status == UserChatStatus.Invalid }
        )

    val uiItem = ContactUiItem(nameOrEmail = with(contactItem) {
        contactData.alias ?: contactData.fullName ?: email
    }, contactStatus = status, avatar = contactItem.contactData.avatarUri?.let {
        ContactAvatar.UriAvatar(
            uri = it, areCredentialsVerified = contactItem.areCredentialsVerified
        )
    } ?: ContactAvatar.InitialsAvatar(
        firstLetter = contactItem.getAvatarFirstLetter(),
        defaultAvatarColor = Color(contactItem.defaultAvatarColor?.toColorInt() ?: -1),
        areCredentialsVerified = contactItem.areCredentialsVerified,
    ))
    ContactItemView(
        contactUiItem = uiItem,
        onClick = onClick,
        modifier = modifier,
        statusOverride = statusOverride,
        selected = selected,
        dividerType = dividerType,
    )
}

/**
 * View to show a contactItem with their avatar and connection status
 * @param contactUiItem that will be shown
 * @param onClick is invoked when the view is clicked
 * @param modifier
 * @param statusOverride to allow change the status text, if null connection status description will be shown
 * @param dividerType divider type to be shown at the bottom of the view, null for no divider
 */
@Composable
internal fun ContactItemView(
    contactUiItem: ContactUiItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusOverride: String? = null,
    selected: Boolean = false,
    dividerType: DividerType? = DividerType.BigStartPadding,
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
            ContactAvatarVerified(
                contactUiItem.avatar,
                selected = selected,
                modifier = Modifier.testTag(CONTACT_ITEM_CONTACT_AVATAR_IMAGE),
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contactUiItem.nameOrEmail,
                        style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(CONTACT_ITEM_CONTACT_NAME_TEXT),
                    )

                    contactUiItem.contactStatus.iconPainter?.let { iconPainter ->
                        ContactStatusView(iconPainter = iconPainter)
                    }
                }

                val secondLineText = statusOverride ?: contactUiItem.contactStatus.statusText
                secondLineText?.let {
                    MarqueeText(
                        text = secondLineText,
                        style = MaterialTheme.typography.subtitle2,
                        color = TextColor.Secondary,
                        modifier = Modifier.testTag(CONTACT_ITEM_STATUS_TEXT),
                    )
                }
            }
        }
        dividerType?.let {
            MegaDivider(it)
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
private fun ContactAvatarView(
    contactAvatar: ContactAvatar,
    modifier: Modifier = Modifier,
) {
    when (contactAvatar) {
        is ContactAvatar.UriAvatar -> {
            UriAvatarView(modifier = modifier, uri = contactAvatar.uri)
        }

        is ContactAvatar.InitialsAvatar -> {
            DefaultAvatarView(
                modifier = modifier,
                color = contactAvatar.defaultAvatarColor,
                content = contactAvatar.firstLetter,
            )
        }
    }
}

@Deprecated(
    message = "Use the ContactAvatarVerified function with ContactAvatar parameter",
    replaceWith = ReplaceWith(
        expression = "ContactAvatarVerified(contactAvatar, modifier, selected)",
        imports = ["mega.privacy.android.app.presentation.contact.view.ContactAvatarVerified"]
    )
)
@Composable
internal fun ContactAvatarVerified(
    contactItem: ContactItem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    ContactAvatarVerified(
        ContactAvatar.InitialsAvatar(
            firstLetter = contactItem.getAvatarFirstLetter(),
            defaultAvatarColor = Color(contactItem.defaultAvatarColor?.toColorInt() ?: -1),
            areCredentialsVerified = contactItem.areCredentialsVerified,
        ),
        modifier = modifier,
        selected = selected,
    )
}

@Composable
internal fun ContactAvatarVerified(
    contactAvatar: ContactAvatar,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier,
    ) {
        val avatarModifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = if (contactAvatar.areCredentialsVerified) 16.dp else 8.dp
            )
            .size(40.dp)
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                scaleIn(animationSpec = tween(220)) togetherWith scaleOut(animationSpec = tween(90))
            },
            label = ""
        ) { targetState ->
            if (targetState) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chat_avatar_select),
                    contentDescription = stringResource(id = R.string.selected_items, 1),
                    modifier = avatarModifier,
                )
            } else {
                ContactAvatarView(
                    contactAvatar = contactAvatar,
                    modifier = avatarModifier.clip(CircleShape),
                )
            }
        }
        if (contactAvatar.areCredentialsVerified) {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                painter = painterResource(id = IconR.drawable.ic_contact_verified),
                contentDescription = "Verified user"
            )
        }
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewContactItem() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {

        val contactUiItem = ContactUiItem(
            nameOrEmail = "Alias",
            avatar = ContactAvatar.InitialsAvatar(
                firstLetter = "A",
                defaultAvatarColor = Color("blue".toColorInt()),
                areCredentialsVerified = false
            ),
            contactStatus = ContactStatus(
                iconPainter = painterResource(UserChatStatus.Online.iconRes(MaterialTheme.colors.isLight)),
                statusText = "Online"
            )
        )

        ContactItemView(
            contactUiItem = contactUiItem,
            onClick = {}
        )
    }
}

internal const val CONTACT_ITEM_CONTACT_NAME_TEXT = "contact_item:contact_name_text"
internal const val CONTACT_ITEM_STATUS_TEXT = "contact_item:status_text"
internal const val CONTACT_ITEM_CONTACT_AVATAR_IMAGE = "contact_item:contact_avatar_image"
