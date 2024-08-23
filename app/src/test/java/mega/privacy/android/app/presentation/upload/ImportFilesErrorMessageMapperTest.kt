package mega.privacy.android.app.presentation.upload

import android.content.Context
import android.content.res.Resources
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

class ImportFilesErrorMessageMapperTest {
    private val context = mock(Context::class.java)
    private val resources = mock(Resources::class.java)
    private val underTest = ImportFilesErrorMessageMapper(context)

    @BeforeEach
    fun reset() {
        reset(context)
    }

    @Test
    fun `test that mapper return s general incorrect names when both wrong and empty names are greater than 0`() {
        whenever(context.getString(R.string.general_incorrect_names)).thenReturn("Please correct your filenames before proceeding")
        val result = underTest(true, 1)
        Truth.assertThat(result).isEqualTo("Please correct your filenames before proceeding")
    }

    @Test
    fun `test that mapper returns empty names when empty names are greater than 0`() {
        whenever(context.resources).thenReturn(resources)
        whenever(
            resources.getQuantityString(R.plurals.empty_names, 1)
        ).thenReturn("File name cannot be empty.")
        val result = underTest(false, 1)
        Truth.assertThat(result).isEqualTo("File name cannot be empty.")
    }

    @Test
    fun `test that mapper returns invalid characters when wrong names are greater than 0`() {
        whenever(
            context.getString(
                R.string.invalid_characters_defined,
                StringsConstants.INVALID_CHARACTERS
            )
        ).thenReturn("The following characters are not allowed: / \\ < > : \" | ? *")
        val result = underTest(true, 0)
        Truth.assertThat(result)
            .isEqualTo("The following characters are not allowed: / \\ < > : \" | ? *")
    }

    @Test
    fun `test that mapper returns empty string when both wrong and empty names are 0`() {
        val result = underTest(false, 0)
        Truth.assertThat(result).isEmpty()
    }

}