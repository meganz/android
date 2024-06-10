package mega.privacy.android.data.mapper.file

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentFileMapperTest {
    private val mapper = DocumentFileMapper()

    @Test
    fun `test that document file mapper returns`() {
        val uri = mock<Uri> {
            on { toString() }.thenReturn("content://com.android.externalstorage.documents/tree/primary%3A")
        }
        val documentFile = mock<DocumentFile> {
            on { name }.thenReturn("name")
            on { length() }.thenReturn(1)
            on { lastModified() }.thenReturn(2)
            on { isDirectory }.thenReturn(true)
            on { this.uri }.thenReturn(uri)
        }
        val expect = DocumentEntity(
            name = "name",
            size = 1,
            lastModified = 2,
            uri = UriPath("content://com.android.externalstorage.documents/tree/primary%3A"),
            isFolder = true,
            numFiles = 3,
            numFolders = 4
        )
        val result = mapper(documentFile, 3, 4)
        assertThat(result).isEqualTo(expect)
    }
}