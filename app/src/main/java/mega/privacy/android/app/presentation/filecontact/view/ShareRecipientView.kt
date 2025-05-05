package mega.privacy.android.app.presentation.filecontact.view


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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.model.ContactAvatar
import mega.privacy.android.app.presentation.contact.view.ContactStatusView
import mega.privacy.android.app.presentation.contact.view.DefaultAvatarView
import mega.privacy.android.app.presentation.contact.view.UriAvatarView
import mega.privacy.android.app.presentation.container.LocalIsDarkTheme
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.icon.pack.R as IconR

/**
 * View to show a shareRecipient with their avatar and share permission
 * @param shareRecipient that will be shown
 * @param onClick is invoked when the view is clicked
 * @param modifier
 */
@Composable
internal fun ShareRecipientView(
    shareRecipient: ShareRecipient,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatarVerified(
                shareRecipient.getAvatar(),
                selected = selected,
                modifier = Modifier.testTag(SHARE_RECIPIENT_CONTACT_AVATAR_IMAGE),
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MegaText(
                        text = shareRecipient.nameOrEmail(),
                        textColor = TextColor.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(SHARE_RECIPIENT_CONTACT_NAME_TEXT),
                    )

                    (shareRecipient as? ShareRecipient.Contact)?.status?.let {
                        ContactStatusView(iconPainter = painterResource(it.iconRes(!LocalIsDarkTheme.current)))
                    }
                }

                val secondLineText = shareRecipient.permission.description()?.let {
                    stringResource(id = it)
                } ?: ""
                MegaText(
                    text = secondLineText,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(SHARE_RECIPIENT_STATUS_TEXT),
                )
            }
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

private fun ShareRecipient.nameOrEmail(): String {
    return when (this) {
        is ShareRecipient.Contact -> contactData.alias ?: email
        is ShareRecipient.NonContact -> email
    }
}

@Composable
private fun ShareRecipient.getDefaultAvatarColor(): Color {
    return when (this) {
        is ShareRecipient.Contact -> Color(defaultAvatarColor)
        is ShareRecipient.NonContact -> colorResource(R.color.red_600_red_300)
    }
}

@Composable
private fun ShareRecipient.getAvatar(): ContactAvatar {
    return when (this) {
        is ShareRecipient.Contact -> {
            contactData.avatarUri?.let {
                ContactAvatar.UriAvatar(
                    uri = it, areCredentialsVerified = isVerified
                )
            } ?: ContactAvatar.InitialsAvatar(
                firstLetter = getAvatarFirstLetter(),
                defaultAvatarColor = getDefaultAvatarColor(),
                areCredentialsVerified = isVerified
            )
        }

        is ShareRecipient.NonContact -> {
            ContactAvatar.InitialsAvatar(
                firstLetter = getAvatarFirstLetter(),
                defaultAvatarColor = getDefaultAvatarColor(),
                areCredentialsVerified = false
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ShareRecipientViewPreview() {
    CompositionLocalProvider(
        LocalIsDarkTheme provides isSystemInDarkTheme()
    ) {
        AndroidThemeForPreviews {
            val recipient = ShareRecipient.NonContact(
                email = "nonContact@email.com",
                permission = AccessPermission.READWRITE,
                isPending = false,
            )
            var selected by remember { mutableStateOf(false) }

            ShareRecipientView(
                shareRecipient = recipient,
                modifier = Modifier.clickable { selected = !selected },
                selected = selected,
            )
        }
    }
}

internal const val SHARE_RECIPIENT_CONTACT_NAME_TEXT = "share_recipient_view:contact_name_text"
internal const val SHARE_RECIPIENT_STATUS_TEXT = "share_recipient_view:status_text"
internal const val SHARE_RECIPIENT_CONTACT_AVATAR_IMAGE =
    "share_recipient_view:contact_avatar_image"