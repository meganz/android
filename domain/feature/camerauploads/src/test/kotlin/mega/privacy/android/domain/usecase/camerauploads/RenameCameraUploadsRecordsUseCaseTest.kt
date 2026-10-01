package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenameCameraUploadsRecordsUseCaseTest {

    private lateinit var underTest: RenameCameraUploadsRecordsUseCase

    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private fun getRecordList(folderType: CameraUploadFolderType): List<CameraUploadsRecord> =
        listOf(
            CameraUploadsRecord(
                mediaId = 1234L,
                fileName = "picture.jpg",
                filePath = "filepath",
                timestamp = 1696294469,
                folderType = folderType,
                type = CameraUploadsRecordType.TYPE_PHOTO,
                uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
                originalFingerprint = "originalFingerprint",
                generatedFingerprint = null,
                tempFilePath = "tempFilePath",
                existsInTargetNode = null,
            ),
            CameraUploadsRecord(
                mediaId = 12345L,
                fileName = "picture.jpg",
                filePath = "filepath",
                timestamp = 1696294470,
                folderType = folderType,
                type = CameraUploadsRecordType.TYPE_PHOTO,
                uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
                originalFingerprint = "originalFingerprint",
                generatedFingerprint = null,
                tempFilePath = "tempFilePath",
                existsInTargetNode = null,
            )
        )

    private val getRecordAlreadyExistInTargetNode: List<CameraUploadsRecord> =
        listOf(
            CameraUploadsRecord(
                mediaId = 1234L,
                fileName = "picture.jpg",
                filePath = "filepath",
                timestamp = 1696294469,
                folderType = CameraUploadFolderType.Primary,
                type = CameraUploadsRecordType.TYPE_PHOTO,
                uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
                originalFingerprint = "originalFingerprint",
                generatedFingerprint = null,
                tempFilePath = "tempFilePath",
                existsInTargetNode = true,
            ),
        )

    private val primaryUploadNodeId = mock<NodeId>()
    private val secondaryUploadNodeId = mock<NodeId>()

    @BeforeAll
    fun setUp() {
        underTest = RenameCameraUploadsRecordsUseCase(
            getChildNodeUseCase,
            areUploadFileNamesKeptUseCase,
            ioDispatcher,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getChildNodeUseCase,
            areUploadFileNamesKeptUseCase,
        )
    }

    @Test
    fun `test that the file name is not generated if the file already exists in the target node`() =
        runTest {
            val recordList = getRecordAlreadyExistInTargetNode
            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)

            val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

            val actual = renamedList[0].generatedFileName
            val expected = null
            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose to keep name and the file name is not already used, the file name is kept`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, recordList[0].fileName))
            .thenReturn(null)

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[0].generatedFileName
        val expected = recordList[0].fileName
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose to keep name and the file name is already used for the current list, the file name is kept and an index suffix is added`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, recordList[0].fileName))
            .thenReturn(null)
        whenever(getChildNodeUseCase(uploadNodeId, recordList[1].fileName))
            .thenReturn(null)

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[1].generatedFileName
        val name = recordList[1].fileName.substringBeforeLast(".", "")
        val extension = recordList[1].fileName.substringAfterLast(".", "")
        val expected = "${name}_1.$extension"
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose to keep name and the file name is already used in the target folder, the file name is kept and an index suffix is added`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, recordList[0].fileName))
            .thenReturn(mock<FileNode>())

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[0].generatedFileName
        val name = recordList[0].fileName.substringBeforeLast(".", "")
        val extension = recordList[0].fileName.substringAfterLast(".", "")
        val expected = "${name}_1.$extension"
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose not to keep name and the file name is not already used, the file name is formatted`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        val timePattern = "yyyy-MM-dd HH.mm.ss"
        val sdf = SimpleDateFormat(timePattern, Locale.getDefault())
        val newFileName = sdf.format(Date(recordList[0].timestamp))
        val extension = recordList[0].fileName.substringAfterLast(".", "")
        val formattedName = "$newFileName.$extension"

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, formattedName))
            .thenReturn(null)

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[0].generatedFileName
        assertThat(actual).isEqualTo(formattedName)
    }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose not to keep name and the file name is already used for the current list, the file name is formatted and an index suffix is added`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        val timePattern = "yyyy-MM-dd HH.mm.ss"
        val sdf = SimpleDateFormat(timePattern, Locale.getDefault())
        val newFileName = sdf.format(Date(recordList[1].timestamp))
        val extension = recordList[1].fileName.substringAfterLast(".", "")
        val formattedName = "$newFileName.$extension"

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, recordList[1].fileName))
            .thenReturn(null)

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[1].generatedFileName
        val name = formattedName.substringBeforeLast(".", "")
        val extension2 = formattedName.substringAfterLast(".", "")
        val expected = "${name}_1.$extension2"

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when folder type is {0}")
    @MethodSource("provideFolderTypeParameters")
    fun `test that if the user choose not to keep name and the file name is already used in the target folder, the file name is formatted and an index suffix is added`(
        folderType: CameraUploadFolderType,
    ) = runTest {
        val recordList = getRecordList(folderType)

        val timePattern = "yyyy-MM-dd HH.mm.ss"
        val sdf = SimpleDateFormat(timePattern, Locale.getDefault())
        val newFileName = sdf.format(Date(recordList[0].timestamp))
        val extension = recordList[0].fileName.substringAfterLast(".", "")
        val formattedName = "$newFileName.$extension"

        whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)

        val uploadNodeId = when (folderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }
        whenever(getChildNodeUseCase(uploadNodeId, formattedName))
            .thenReturn(mock<FileNode>())

        val renamedList = underTest(recordList, primaryUploadNodeId, secondaryUploadNodeId)

        val actual = renamedList[0].generatedFileName
        val name = formattedName.substringBeforeLast(".", "")
        val extension2 = formattedName.substringAfterLast(".", "")
        val expected = "${name}_1.$extension2"

        assertThat(actual).isEqualTo(expected)
    }

    private fun provideFolderTypeParameters(): Stream<Arguments> =
        Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
}
