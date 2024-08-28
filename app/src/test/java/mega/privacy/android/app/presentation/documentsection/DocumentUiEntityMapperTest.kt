package mega.privacy.android.app.presentation.documentsection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentUiEntityMapperTest {
    private lateinit var underTest: DocumentUiEntityMapper

    private val expectedId = NodeId(123456L)
    private val expectedName = "Audio file name"
    private val expectedSize: Long = 100
    private val expectedThumbnail = "Audio file thumbnail"
    private val expectedIsFavourite = true
    private val expectedIsExported = ExportedData("link", 100)
    private val expectedIsTakeDown = true
    private val expectedNumVersions = 2
    private val expectedModificationTime: Long = 999
    private val expectedLabel = 1
    private val expectedAvailableOffline = true
    private val expectedType = mock<PdfFileTypeInfo> {
        on { extension }.thenReturn("pdf")
    }
    private val expectedIcon = R.drawable.ic_pdf_medium_solid

    private val fileTypeIconMapper = mock<FileTypeIconMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DocumentUiEntityMapper(fileTypeIconMapper)
    }

    @AfterEach
    fun resetMocks() {
        reset(fileTypeIconMapper)
    }

    @Test
    fun `test that DocumentUIEntity can be mapped correctly`() = runTest {
        whenever(fileTypeIconMapper(expectedType.extension)).thenReturn(expectedIcon)
        val documentUIEntity = underTest(initTypedFileNode())
        assertMappedDocumentUIEntity(documentUIEntity)
    }

    private fun initTypedFileNode() = mock<TypedVideoNode> {
        on { id }.thenReturn(expectedId)
        on { name }.thenReturn(expectedName)
        on { size }.thenReturn(expectedSize)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
        on { thumbnailPath }.thenReturn(expectedThumbnail)
        on { exportedData }.thenReturn(expectedIsExported)
        on { label }.thenReturn(expectedLabel)
        on { isTakenDown }.thenReturn(expectedIsTakeDown)
        on { versionCount }.thenReturn(expectedNumVersions)
        on { modificationTime }.thenReturn(expectedModificationTime)
        on { hasVersion }.thenReturn(true)
        on { type }.thenReturn(expectedType)
    }

    private fun assertMappedDocumentUIEntity(documentUIEntity: DocumentUiEntity) {
        documentUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${DocumentUiEntity::class.simpleName}",
                { assertThat(it.id).isEqualTo(expectedId) },
                { assertThat(it.name).isEqualTo(expectedName) },
                { assertThat(it.size).isEqualTo(expectedSize) },
                { assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnail) },
                { assertThat(it.nodeAvailableOffline).isEqualTo(expectedAvailableOffline) },
                { assertThat(it.isFavourite).isEqualTo(expectedIsFavourite) },
                { assertThat(it.isExported).isTrue() },
                { assertThat(it.isTakenDown).isEqualTo(expectedIsTakeDown) },
                { assertThat(it.hasVersions).isTrue() },
                { assertThat(it.modificationTime).isEqualTo(expectedModificationTime) },
                { assertThat(it.label).isEqualTo(expectedLabel) },
                { assertThat(it.fileTypeInfo).isEqualTo(expectedType) },
                { assertThat(it.icon).isEqualTo(expectedIcon) },
            )
        }
    }
}