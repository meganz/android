package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.ChatStatusIcon
import mega.privacy.android.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.conditional

/**
 * Contact attachment message view
 *
 * @param isMe Whether the message is sent by me
 * @param userName User name
 * @param email Email
 * @param avatar Avatar
 * @param status chat status
 * @param modifier Modifier
 * @param isVerified Whether the contact is verified
 */
@Composable
fun ContactAttachmentMessageView(
    isMe: Boolean,
    userName: String,
    email: String,
    status: UiChatStatus?,
    avatar: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    isVerified: Boolean = false,
) {
    ChatBubble(isMe = isMe, modifier = modifier) {
        CompositionLocalProvider(
            LocalContentColor provides if (isMe) MegaTheme.colors.text.inverse else MegaTheme.colors.text.primary,
        ) {
            ContactMessageContentView(
                avatar = avatar,
                status = status,
                userName = userName,
                email = email,
                modifier = modifier,
                isVerified = isVerified
            )
        }
    }
}

/**
 * Contact message content view
 *
 * @param avatar Avatar composable
 * @param status chat status
 * @param userName User name
 * @param email
 * @param modifier Modifier
 * @param isVerified Whether the contact is verified
 */
@Composable
fun ContactMessageContentView(
    avatar: @Composable BoxScope.() -> Unit,
    status: UiChatStatus?,
    userName: String,
    email: String,
    modifier: Modifier = Modifier,
    isVerified: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .conditional(isVerified) {
                        padding(top = 5.dp, end = 5.dp)
                    }
                    .size(40.dp)
            ) {
                Box(
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MegaTheme.colors.background.pageBackground,
                        shape = CircleShape
                    )
                ) {
                    avatar()
                }
                if (!isVerified) {
                    status?.let {
                        ChatStatusIcon(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .testTag(TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_STATUS_ICON),
                            status = status
                        )
                    }
                }
            }
            if (isVerified) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag(TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_VERIFIED)
                        .size(22.dp)
                        .border(1.dp, MegaTheme.colors.background.pageBackground, CircleShape)
                        .background(
                            color = MegaTheme.colors.indicator.blue,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center),
                        painter = painterResource(id = R.drawable.check),
                        tint = MegaTheme.colors.icon.inverse,
                        contentDescription = "Checked"
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                modifier = Modifier.testTag(TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_USER_NAME),
                text = userName,
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                modifier = Modifier.testTag(TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_EMAIL),
                text = email,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ContactAttachmentMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ContactAttachmentMessageView(
            isMe = isMe,
            userName = "User Name",
            email = "lh@mega.co.nz",
            avatar = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MegaTheme.colors.background.inverse,
                            shape = CircleShape
                        ),
                )
            },
            status = UiChatStatus.Online,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VerifiedContactAttachmentMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ContactAttachmentMessageView(
            isMe = isMe,
            userName = "User Name",
            email = "lh@mega.co.nz",
            avatar = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MegaTheme.colors.background.inverse,
                            shape = CircleShape
                        ),
                )
            },
            status = UiChatStatus.Online,
            isVerified = true
        )
    }
}

internal const val TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_STATUS_ICON =
    "contact_message_content_view:status_icon"
internal const val TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_USER_NAME =
    "contact_message_content_view:user_name"
internal const val TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_EMAIL =
    "contact_message_content_view:email"
internal const val TEST_TAG_CONTACT_MESSAGE_CONTENT_VIEW_VERIFIED =
    "contact_message_content_view:verified"