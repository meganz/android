package mega.privacy.android.app.uploadFolder

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentEntityDataMapperTest {
    private val context: Context = mock()

    private lateinit var documentEntityDataMapper: DocumentEntityDataMapper

    @BeforeAll
    fun setUp() {
        documentEntityDataMapper = DocumentEntityDataMapper()
    }

    @BeforeEach
    fun reset() {
        Mockito.reset(context)
    }

    @Test
    fun `test that FolderContent Data returns correctly`() {
        Mockito.mockStatic(Uri::class.java).use { _ ->
            val androidUri = mock<Uri>()
            whenever(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A"))
                .thenReturn(androidUri)
            val parent = mock<FolderContent.Data>()
            val entity = mock<DocumentEntity> {
                on { uri } doReturn UriPath("content://com.android.externalstorage.documents/tree/primary%3A")
                on { name } doReturn "name"
                on { isFolder } doReturn true
                on { lastModified } doReturn 0L
                on { size } doReturn 0L
                on { numFiles } doReturn 0
                on { numFolders } doReturn 0
            }

            val result = documentEntityDataMapper(parent, entity)

            assertThat(result).isInstanceOf(FolderContent.Data::class.java)
        }
    }
}