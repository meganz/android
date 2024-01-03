package mega.privacy.android.core.ui.controls.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.conditional

/**
 * Attachment icon test tag.
 */
const val TEST_TAG_ATTACHMENT_ICON = "chat_input_text_toolbar:attachment_icon"

/**
 * Expand icon test tag.
 */
const val TEST_TAG_EXPAND_ICON = "chat_input_text_toolbar:expand_icon"

/**
 * Send icon test tag.
 */
const val TEST_TAG_SEND_ICON = "chat_input_text_toolbar:send_icon"

/**
 * Chat input text toolbar
 *
 * @param modifier modifier
 * @param onAttachmentClick click listener for attachment icon
 * @param onSendClick click listener for send icon
 */
@Composable
fun ChatInputTextToolbar(
    text: String,
    placeholder: String,
    onAttachmentClick: () -> Unit,
    onSendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable(text) {
        mutableStateOf(text)
    }
    var isInputExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    var showExpandButton by remember {
        mutableStateOf(false)
    }
    var isRoundedShape by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(input) {
        if (input.isEmpty()) {
            isInputExpanded = false
        }
    }
    BackHandler(enabled = isInputExpanded) {
        isInputExpanded = false
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MegaTheme.colors.background.pageBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (showExpandButton || isInputExpanded) {
            Icon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp)
                    .testTag(TEST_TAG_EXPAND_ICON)
                    .clickable {
                        isInputExpanded = !isInputExpanded
                    },
                painter = painterResource(id = if (isInputExpanded) R.drawable.ic_collapse_text_input else R.drawable.ic_expand_text_input),
                contentDescription = "Attachment icon",
                tint = MegaTheme.colors.icon.secondary,
            )
        }
        Row {
            Icon(
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(end = 8.dp, top = 6.dp, bottom = 6.dp)
                    .testTag(TEST_TAG_ATTACHMENT_ICON)
                    .clickable(onClick = onAttachmentClick),
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = "Attachment icon",
                tint = MegaTheme.colors.icon.secondary,
            )

            ChatTextField(
                text = input,
                placeholder = placeholder,
                onTextChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .conditional(isInputExpanded) {
                        fillMaxHeight()
                    },
                isExpanded = isInputExpanded,
                onTextLayout = {
                    showExpandButton = it.lineCount >= 4
                    isRoundedShape = it.lineCount == 1
                },
                shape = if (isRoundedShape) CircleShape else RoundedCornerShape(12.dp),
            )

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(vertical = 6.dp),
                visible = input.isNotEmpty()
            ) {
                Icon(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag(TEST_TAG_SEND_ICON)
                        .clickable(onClick = {
                            onSendClick(input)
                            isInputExpanded = false
                            input = ""
                        }),
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Send icon",
                    tint = MegaTheme.colors.icon.accent
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarPlaceholderPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            text = "",
            placeholder = "Very long long long long long long long long long long long long long long long long long long long long long long long long long long long ",
            onAttachmentClick = {},
            onSendClick = {})
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarLongTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            text = "abc ".repeat(30),
            placeholder = "",
            onAttachmentClick = {},
            onSendClick = {})
    }
}