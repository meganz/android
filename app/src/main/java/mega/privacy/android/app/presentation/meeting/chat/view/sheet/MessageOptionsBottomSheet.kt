package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.emoji2.emojipicker.EmojiPickerView
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageBottomSheetAction
import mega.privacy.android.shared.original.core.ui.controls.chat.MegaEmojiPickerView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.AddReactionsSheetItem
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Bottom sheet for chat message options.
 */
@Composable
fun MessageOptionsBottomSheet(
    onReactionClicked: (String) -> Unit,
    actions: List<MessageBottomSheetAction>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var preloadedPicker by remember {
        mutableStateOf<EmojiPickerView?>(null)
    }

    val context = LocalContext.current
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    LaunchedEffect(isPortrait) {
        preloadedPicker = preloadEmojiPicker(
            context = context,
            isPortrait = isPortrait,
            onEmojiSelected = onReactionClicked
        )
    }

    var moreEmoji by remember {
        mutableStateOf(false)
    }

    val showEmoji by remember { derivedStateOf { preloadedPicker != null && moreEmoji } }

    BackHandler(moreEmoji) {
        moreEmoji = false
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_MESSAGE_OPTIONS_PANEL)
            .verticalScroll(scrollState)
    ) {
        AnimatedContent(
            targetState = showEmoji,
            transitionSpec = {
                if (targetState) {
                    // Forward animation: slide in from right and out to left
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) using
                            SizeTransform(clip = false)
                } else {
                    // Reverse animation: slide in from left and out to right
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
                            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) using
                            SizeTransform(clip = false)
                }
            }, label = "Animate emoji picker"
        ) { targetState ->
            if (targetState) {
                val picker = preloadedPicker
                if (picker != null) {
                    MegaEmojiPickerView(
                        preloadedPicker = picker,
                        modifier = Modifier
                    )
                }
            } else {
                Column {
                    AddReactionsSheetItem(
                        onReactionClicked = {
                            onReactionClicked(it)
                        },
                        onMoreReactionsClicked = { moreEmoji = true },
                        modifier = Modifier.padding(8.dp),
                    )

                    var group = if (actions.isNotEmpty()) actions.first().group else null
                    actions.forEach {
                        if (group != it.group) {
                            MegaDivider(dividerType = DividerType.BigStartPadding)
                            group = it.group
                        }
                        it.view()
                    }
                }
            }
        }
    }
}

private fun preloadEmojiPicker(
    context: Context,
    isPortrait: Boolean,
    onEmojiSelected: (String) -> Unit,
) = EmojiPickerView(context).apply {
    emojiGridColumns = if (isPortrait) 9 else 18
    setOnEmojiPickedListener { selection -> onEmojiSelected(selection.emoji) }
}

@CombinedThemePreviews
@Composable
private fun MessageOptionsBottomSheetPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MessageOptionsBottomSheet(
            onReactionClicked = {},
            actions = listOf(),
        )
    }
}

internal const val TEST_TAG_MESSAGE_OPTIONS_PANEL = "chat_view:message_options_panel"