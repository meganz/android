package mega.privacy.android.app.presentation.upload

import android.content.Context
import mega.privacy.android.app.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ImportFileErrorMessageMapperTest {

    @Test
    fun `test that mapper returns empty name error message when file name is blank`() {
        val context = mock(Context::class.java)
        `when`(context.getString(R.string.empty_name)).thenReturn("empty name")
        val importFileErrorMessageMapper = ImportFileErrorMessageMapper(context)
        val fileName = ""
        val result = importFileErrorMessageMapper(fileName)
        assertEquals("empty name", result)
    }

    @Test
    fun `test that mapper returns invalid characters error message when file name contains invalid characters`() {
        val context = mock(Context::class.java)
        `when`(context.getString(R.string.invalid_characters)).thenReturn("invalid characters")
        val importFileErrorMessageMapper = ImportFileErrorMessageMapper(context)
        val fileName = "file*"
        val result = importFileErrorMessageMapper(fileName)
        assertEquals("invalid characters", result)
    }

    @Test
    fun `test that mapper returns null when file name is valid`() {
        val context = mock(Context::class.java)
        val importFileErrorMessageMapper = ImportFileErrorMessageMapper(context)
        val fileName = "file"
        val result = importFileErrorMessageMapper(fileName)
        assertNull(result)
    }
}