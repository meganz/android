package mega.privacy.android.feature.sync.ui.model.exclusion

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddExclusionByExtensionFormStateTest {

    @Test
    fun `test that extensions is empty when nothing has been typed`() {
        assertThat(AddExclusionByExtensionFormState().extensions).isEmpty()
    }

    @Test
    fun `test that extensions reads a single entry`() {
        assertThat(AddExclusionByExtensionFormState(value = "log").extensions)
            .containsExactly("log")
    }

    @Test
    fun `test that extensions splits on commas and keeps typing order`() {
        assertThat(AddExclusionByExtensionFormState(value = "log, mp4, mp3").extensions)
            .containsExactly("log", "mp4", "mp3").inOrder()
    }

    @Test
    fun `test that extensions trims surrounding whitespace from every entry`() {
        assertThat(AddExclusionByExtensionFormState(value = "  mp4 ,\tzip  ").extensions)
            .containsExactly("mp4", "zip").inOrder()
    }

    @Test
    fun `test that extensions drops the empty entry left by a trailing comma`() {
        assertThat(AddExclusionByExtensionFormState(value = "log,").extensions)
            .containsExactly("log")
    }

    @Test
    fun `test that extensions drops empty entries between repeated commas`() {
        assertThat(AddExclusionByExtensionFormState(value = "log,,mp4").extensions)
            .containsExactly("log", "mp4").inOrder()
    }

    @Test
    fun `test that extensions is empty when only separators have been typed`() {
        assertThat(AddExclusionByExtensionFormState(value = " , , ").extensions).isEmpty()
    }

    @Test
    fun `test that extensions leaves a leading dot for the pattern mapper`() {
        assertThat(AddExclusionByExtensionFormState(value = ".mp3, .zip").extensions)
            .containsExactly(".mp3", ".zip").inOrder()
    }

    @Test
    fun `test that extensions does not walk a doubled leading dot past the pattern mapper`() {
        assertThat(AddExclusionByExtensionFormState(value = "..mp3").extensions)
            .containsExactly("..mp3")
    }

    @Test
    fun `test that extensions reports a lone dot rather than dropping it`() {
        assertThat(AddExclusionByExtensionFormState(value = "., log").extensions)
            .containsExactly(".", "log").inOrder()
    }

    @Test
    fun `test that extensions keeps the space in a dot and space entry`() {
        assertThat(AddExclusionByExtensionFormState(value = ". mp3").extensions)
            .containsExactly(". mp3")
    }

    @Test
    fun `test that extensions folds case so one extension cannot become two rules`() {
        assertThat(AddExclusionByExtensionFormState(value = "MP3, Mp4").extensions)
            .containsExactly("mp3", "mp4").inOrder()
    }

    @Test
    fun `test that extensions splits on a fullwidth comma`() {
        assertThat(AddExclusionByExtensionFormState(value = "mp4，zip").extensions)
            .containsExactly("mp4", "zip").inOrder()
    }

    @Test
    fun `test that extensions splits on an ideographic comma`() {
        assertThat(AddExclusionByExtensionFormState(value = "mp4、zip").extensions)
            .containsExactly("mp4", "zip").inOrder()
    }

    @Test
    fun `test that extensions splits on an arabic comma`() {
        assertThat(AddExclusionByExtensionFormState(value = "mp4،zip").extensions)
            .containsExactly("mp4", "zip").inOrder()
    }

    @Test
    fun `test that extensions splits on a mixture of comma characters`() {
        assertThat(AddExclusionByExtensionFormState(value = "log,mp4，zip、mp3").extensions)
            .containsExactly("log", "mp4", "zip", "mp3").inOrder()
    }

    @Test
    fun `test that extensions keeps an inner dot`() {
        assertThat(AddExclusionByExtensionFormState(value = "tar.gz").extensions)
            .containsExactly("tar.gz")
    }

    @Test
    fun `test that extensions keeps a repeated entry so the caller can report it`() {
        assertThat(AddExclusionByExtensionFormState(value = "mp3, mp3").extensions)
            .containsExactly("mp3", "mp3")
    }

    @Test
    fun `test that showsHelper is true when there is no error`() {
        assertThat(AddExclusionByExtensionFormState(value = "log").showsHelper).isTrue()
    }

    @Test
    fun `test that showsHelper is false when an invalid error is shown`() {
        val underTest = AddExclusionByExtensionFormState(
            value = "my doc",
            error = AddExclusionExtensionError.Invalid,
        )

        assertThat(underTest.showsHelper).isFalse()
    }

    @Test
    fun `test that showsHelper is false when a duplicate error is shown`() {
        val underTest = AddExclusionByExtensionFormState(
            value = "mp3",
            error = AddExclusionExtensionError.Duplicate("mp3"),
        )

        assertThat(underTest.showsHelper).isFalse()
    }
}
