package test.mega.privacy.android.app.presentation.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.mapper.file.FolderInfoStringMapper
import mega.privacy.android.app.presentation.zipbrowser.mapper.ZipInfoUiEntityMapper
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipInfoUiEntityMapperTest {
    private lateinit var underTest: ZipInfoUiEntityMapper

    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val folderInfoStringMapper = mock<FolderInfoStringMapper>()

    private val testName = "zip entry"
    private val testPath = "zip path"

    @BeforeAll
    fun setUp() {
        underTest = ZipInfoUiEntityMapper(fileSizeStringMapper, folderInfoStringMapper)
    }

    @Test
    fun `test that ZipInfoUiEntity can be mapped correctly when ZipEntryType is FOLDER`() =
        runTest {
            val testZipFile = mock<ZipTreeNode> {
                on { zipEntryType }.thenReturn(ZipEntryType.File)
            }
            val testChildren = listOf(testZipFile, testZipFile)
            val testInfo = "2 files"
            val testEntryType = ZipEntryType.Folder
            val zipTreeNode = mock<ZipTreeNode> {
                on { name }.thenReturn(testName)
                on { path }.thenReturn(testPath)
                on { zipEntryType }.thenReturn(testEntryType)
                on { children }.thenReturn(testChildren)
            }
            whenever(folderInfoStringMapper(0, 2)).thenReturn(testInfo)

            val zipInfoUiEntity = underTest(zipTreeNode)
            assertMappedZipInfoEntityMapper(
                zipInfoUiEntity = zipInfoUiEntity,
                expectedInfo = testInfo,
                expectedZipEntryType = testEntryType
            )
        }

    @Test
    fun `test that ZipInfoUiEntity can be mapped correctly when ZipEntryType is FILE`() =
        runTest {
            val testInfo = "100 B"
            val testEntryType = ZipEntryType.File
            val zipTreeNode = mock<ZipTreeNode> {
                on { name }.thenReturn(testName)
                on { path }.thenReturn(testPath)
                on { zipEntryType }.thenReturn(testEntryType)
                on { size }.thenReturn(100L)
            }
            whenever(fileSizeStringMapper(100)).thenReturn(testInfo)

            val zipInfoUiEntity = underTest(zipTreeNode)
            assertMappedZipInfoEntityMapper(
                zipInfoUiEntity = zipInfoUiEntity,
                expectedInfo = testInfo,
                expectedZipEntryType = testEntryType
            )
        }

    private fun assertMappedZipInfoEntityMapper(
        zipInfoUiEntity: ZipInfoUiEntity,
        expectedInfo: String,
        expectedZipEntryType: ZipEntryType,
    ) {
        zipInfoUiEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${ZipInfoUiEntity::class.simpleName}",
                { assertThat(it.name).isEqualTo(testName) },
                { assertThat(it.path).isEqualTo(testPath) },
                { assertThat(it.info).isEqualTo(expectedInfo) },
                { assertThat(it.zipEntryType).isEqualTo(expectedZipEntryType) }
            )
        }
    }

}