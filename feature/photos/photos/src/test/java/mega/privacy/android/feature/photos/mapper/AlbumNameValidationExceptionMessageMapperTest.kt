package mega.privacy.android.feature.photos.mapper

import android.content.Context
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumNameValidationExceptionMessageMapperTest {

    private lateinit var underTest: AlbumNameValidationExceptionMessageMapper
    private val context: Context = mock()

    @BeforeAll
    fun setUp() {
        underTest = AlbumNameValidationExceptionMessageMapper(context)
    }

    @BeforeEach
    fun init() {
        reset(context)
    }

    @Test
    fun `test that Empty exception maps to general_invalid_string resource`() {
        val expected = "expected"
        val exception = AlbumNameValidationException.Empty
        whenever(context.getString(sharedR.string.general_invalid_string)).thenReturn(expected)
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that Exists exception maps to album_invalid_name_error_message resource`() {
        val expected = "expected"
        val exception = AlbumNameValidationException.Exists
        whenever(context.getString(sharedR.string.album_name_exists_error_message)).thenReturn(
            expected
        )
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that InvalidCharacters exception maps to general_invalid_characters_defined resource`() {
        val expected = "expected"
        val invalidChars = """\ / * : < > ? " |"""
        val exception = AlbumNameValidationException.InvalidCharacters(invalidChars)
        whenever(
            context.getString(
                sharedR.string.general_invalid_characters_defined,
                invalidChars
            )
        ).thenReturn(expected)
        assertThat(underTest(exception)).isEqualTo(expected)
    }

    @Test
    fun `test that Proscribed exception maps to album_invalid_name_error_message resource`() {
        val expected = "expected"
        val exception = AlbumNameValidationException.Proscribed
        whenever(context.getString(sharedR.string.album_invalid_name_error_message)).thenReturn(
            expected
        )
        assertThat(underTest(exception)).isEqualTo(expected)
    }
}

