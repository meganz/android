package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.testTag
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Avatar view for note to self chat
 */
@Composable
fun NoteToSelfAvatarView(
    isHint: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isHint) {
        MegaIcon(
            painter = painterResource(id = mega.privacy.android.core.R.drawable.file_icon),
            contentDescription = null,
            modifier = modifier.testTag(NOTE_TO_SELF_ITEM_HINT_BUTTON),
            tint = IconColor.Primary,
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.note_avatar),
            contentDescription = stringResource(id = sharedR.string.chat_note_to_self_chat_title) + "icon",
            modifier = modifier.testTag(NOTE_TO_SELF_ITEM_AVATAR_IMAGE),
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewNoteToSelfAvatarView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NoteToSelfAvatarView(
            isHint = false,
        )
    }
}


@CombinedTextAndThemePreviews
@Composable
private fun PreviewNoteToSelfHintView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NoteToSelfAvatarView(
            isHint = true,
        )
    }
}

internal const val NOTE_TO_SELF_ITEM_AVATAR_IMAGE = "note_to_self_item:avatar_image"
internal const val NOTE_TO_SELF_ITEM_HINT_BUTTON = "note_to_self_item:hint_button"