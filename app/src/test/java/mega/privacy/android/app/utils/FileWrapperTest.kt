package mega.privacy.android.app.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FileWrapperTest {

    @Test
    fun `test isPath returns false for uri path`() {
        val uriPath = "file:///path/to/file"
        assertThat(FileWrapper.isPath(uriPath)).isFalse()
    }

    @Test
    fun `test isPath returns true for file path`() {
        Mockito.mockStatic(Uri::class.java).use {
            val filePath = "/path/to/file"
            val uri = mock<Uri> {
                on { it.scheme } doReturn null
            }
            val fileUri = mock<Uri> {
                on { it.scheme } doReturn "file"
            }
            whenever(Uri.parse(filePath)) doReturn uri
            whenever(Uri.fromFile(any())) doReturn fileUri
            assertThat(FileWrapper.isPath(filePath)).isTrue()
        }
    }

    @Test
    fun `test isPath returns false for content uri`() {
        Mockito.mockStatic(Uri::class.java).use {
            val contentUri = "content://path/to/file"
            val uri = mock<Uri> {
                on { it.scheme } doReturn "content"
            }
            whenever(Uri.parse(contentUri)) doReturn uri
            assertThat(FileWrapper.isPath(contentUri)).isFalse()
        }
    }
}