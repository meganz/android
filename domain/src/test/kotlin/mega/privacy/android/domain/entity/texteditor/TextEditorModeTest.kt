package mega.privacy.android.domain.entity.texteditor

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TextEditorModeTest {

    @Test
    fun `test that textEditorModeFromValue returns View for VIEW_MODE`() {
        assertThat(textEditorModeFromValue("VIEW_MODE")).isEqualTo(TextEditorMode.View)
    }

    @Test
    fun `test that textEditorModeFromValue returns Edit for EDIT_MODE`() {
        assertThat(textEditorModeFromValue("EDIT_MODE")).isEqualTo(TextEditorMode.Edit)
    }

    @Test
    fun `test that textEditorModeFromValue returns Create for CREATE_MODE`() {
        assertThat(textEditorModeFromValue("CREATE_MODE")).isEqualTo(TextEditorMode.Create)
    }

    @Test
    fun `test that textEditorModeFromValue returns View when value is unknown`() {
        assertThat(textEditorModeFromValue("UNKNOWN")).isEqualTo(TextEditorMode.View)
        assertThat(textEditorModeFromValue("")).isEqualTo(TextEditorMode.View)
    }
}
