package mega.privacy.android.app.presentation.contact.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.Image
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium

/**
 * View to show a note to self item
 * @param onNoteToSelfClicked is invoked when the view is clicked
 * @param modifier
 */

@Composable
internal fun NoteToSelfView(
    onNoteToSelfClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .then(
                    if (onNoteToSelfClicked != null) {
                        Modifier.clickable(onClick = onNoteToSelfClicked)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = modifier,
            ) {
                val avatarModifier = Modifier
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                    .size(40.dp)

                Image(
                    painter = painterResource(id = mega.privacy.android.app.R.drawable.note_avatar),
                    contentDescription = stringResource(id = sharedR.string.chat_note_to_self_chat_title) + "icon",
                    modifier = avatarModifier
                        .testTag(NOTE_TO_SELF_ITEM_AVATAR_IMAGE),
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MegaText(
                        modifier = Modifier.testTag(NOTE_TO_SELF_ITEM_TITLE_TEXT),
                        text = stringResource(id = sharedR.string.chat_note_to_self_chat_title),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle1medium,
                    )
                }
            }
        }

    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewNoteToSelfView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NoteToSelfView(
            onNoteToSelfClicked = {}
        )
    }
}

internal const val NOTE_TO_SELF_ITEM_TITLE_TEXT = "note_to_self_item:title_text"
internal const val NOTE_TO_SELF_ITEM_AVATAR_IMAGE = "note_to_self_item:avatar_image"
