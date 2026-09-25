package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.mobile.analytics.event.TextEditorPlainTextModeMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorRichTextModeMenuItemEvent
import org.junit.jupiter.api.Test

/**
 * The two edit mode menu events are indistinguishable once reported, so a swapped pairing would
 * invert the data silently. This pins it.
 */
class TextEditorScreenAnalyticsTest {

    @Test
    fun `test that picking rich text reports the rich text menu event`() {
        assertThat(editModeMenuEvent(isRichText = true))
            .isEqualTo(TextEditorRichTextModeMenuItemEvent)
    }

    @Test
    fun `test that picking plain text reports the plain text menu event`() {
        assertThat(editModeMenuEvent(isRichText = false))
            .isEqualTo(TextEditorPlainTextModeMenuItemEvent)
    }
}
