package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun BottomCallButton(
    uiState: ChatUiState,
    modifier: Modifier = Modifier,
    enablePasscodeCheck: () -> Unit = {},
    onJoinAnswerCallClick: () -> Unit = {},
) = with(uiState) {
    if (callsInOtherChats.isEmpty()) return@with

    val context = LocalContext.current

    when {
        callInThisChat?.isOnHold == true -> {
            BottomCallButton(
                textId = R.string.call_on_hold,
                iconId = R.drawable.ic_pause_thin,
                modifier = modifier.testTag(TEST_TAG_BOTTOM_CALL_ON_HOLD_BUTTON),
            ) {
                enablePasscodeCheck()
                startMeetingActivity(context, chatId)
            }
        }

        callInThisChat?.status == ChatCallStatus.UserNoPresent && !schedIsPending -> {
            BottomCallButton(
                textId = if (isGroup) R.string.title_join_call else R.string.title_join_one_to_one_call,
                iconId = R.drawable.ic_phone_01_medium_regular,
                modifier = modifier.testTag(
                    if (isGroup) TEST_TAG_BOTTOM_JOIN_CALL_BUTTON
                    else TEST_TAG_BOTTOM_ANSWER_CALL_BUTTON
                ),
            ) {
                onJoinAnswerCallClick()
            }
        }
    }
}

@Composable
private fun BottomCallButton(
    @StringRes textId: Int,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) = OutlinedMegaButton(
    textId = textId,
    onClick = onClick,
    rounded = true,
    modifier = modifier
        .padding(
            start = 24.dp,
            top = 2.dp,
            end = 24.dp,
            bottom = 16.dp
        ),
    iconId = iconId
)

@CombinedThemePreviews
@Composable
private fun CallOnHoldButtonPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        Box {
            BottomCallButton(
                textId = R.string.call_on_hold,
                iconId = R.drawable.ic_pause_thin,
                onClick = {},
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun JoinAnswerCallButtonPreview(
    @PreviewParameter(BooleanProvider::class) isGroup: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        Box {
            BottomCallButton(
                textId = if (isGroup) R.string.title_join_call else R.string.title_join_one_to_one_call,
                iconId = R.drawable.ic_phone_01_medium_regular,
                onClick = {},
            )
        }
    }
}

internal const val TEST_TAG_BOTTOM_CALL_ON_HOLD_BUTTON = "chat_view:bottom_call_on_hold_button"
internal const val TEST_TAG_BOTTOM_ANSWER_CALL_BUTTON = "chat_view:bottom_answer_call_button"
internal const val TEST_TAG_BOTTOM_JOIN_CALL_BUTTON = "chat_view:bottom_join_call_button"