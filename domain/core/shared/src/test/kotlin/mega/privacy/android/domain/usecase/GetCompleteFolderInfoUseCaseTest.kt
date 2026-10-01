package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CompleteFolderInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCompleteFolderInfoUseCaseTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private lateinit var underTest: GetCompleteFolderInfoUseCase

    @BeforeEach
    fun setUp() {
        underTest = GetCompleteFolderInfoUseCase(getNodeByIdUseCase, getFolderTreeInfo)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getNodeByIdUseCase, getFolderTreeInfo)
    }

    @Test
    fun `test that invoke returns null when node is null`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(null)

        val result = underTest(nodeId)

        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke returns CompleteFolderInfo with default values when getFolderTreeInfo fails`() =
        runTest {
            val nodeId = NodeId(123L)
            val creationTime = 1000L
            val folderNode = mock<TypedFolderNode>()
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
            whenever(folderNode.creationTime).thenReturn(creationTime)
            whenever(getFolderTreeInfo(any())).thenThrow(RuntimeException("Folder tree info failed"))

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(
                CompleteFolderInfo(
                    numOfFiles = 0,
                    numOfFolders = 0,
                    totalSizeInBytes = 0L,
                    creationTime = creationTime,
                )
            )
        }

    @Test
    fun `test that invoke returns CompleteFolderInfo with folder tree info when both operations succeed`() =
        runTest {
            val nodeId = NodeId(123L)
            val creationTime = 1000L
            val folderNode = mock<TypedFolderNode>()
            val folderTreeInfo = FolderTreeInfo(
                numberOfFiles = 5,
                numberOfFolders = 3,
                totalCurrentSizeInBytes = 1024L,
                numberOfVersions = 2,
                sizeOfPreviousVersionsInBytes = 512L
            )

            whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
            whenever(folderNode.creationTime).thenReturn(creationTime)
            whenever(getFolderTreeInfo(any())).thenReturn(folderTreeInfo)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(
                CompleteFolderInfo(
                    numOfFiles = 5,
                    numOfFolders = 3,
                    totalSizeInBytes = 1024L,
                    creationTime = creationTime,
                )
            )
        }

    @Test
    fun `test that invoke returns CompleteFolderInfo with partial folder tree info when some values are zero`() =
        runTest {
            val nodeId = NodeId(123L)
            val creationTime = 2000L
            val folderNode = mock<TypedFolderNode>()
            val folderTreeInfo = FolderTreeInfo(
                numberOfFiles = 10,
                numberOfFolders = 0,
                totalCurrentSizeInBytes = 0L,
                numberOfVersions = 1,
                sizeOfPreviousVersionsInBytes = 0L
            )

            whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
            whenever(folderNode.creationTime).thenReturn(creationTime)
            whenever(getFolderTreeInfo(any())).thenReturn(folderTreeInfo)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(
                CompleteFolderInfo(
                    numOfFiles = 10,
                    numOfFolders = 0,
                    totalSizeInBytes = 0L,
                    creationTime = creationTime,
                )
            )
        }

    @Test
    fun `test that invoke returns null when getNodeByIdUseCase throws exception`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getNodeByIdUseCase(nodeId)).thenThrow(RuntimeException("Node not found"))

        val result = underTest(nodeId)

        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke handles large values correctly`() = runTest {
        val nodeId = NodeId(999L)
        val creationTime = Long.MAX_VALUE
        val folderNode = mock<TypedFolderNode>()
        val folderTreeInfo = FolderTreeInfo(
            numberOfFiles = Int.MAX_VALUE,
            numberOfFolders = Int.MAX_VALUE,
            totalCurrentSizeInBytes = Long.MAX_VALUE,
            numberOfVersions = 0,
            sizeOfPreviousVersionsInBytes = 0L
        )

        whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
        whenever(folderNode.creationTime).thenReturn(creationTime)
        whenever(getFolderTreeInfo(any())).thenReturn(folderTreeInfo)

        val result = underTest(nodeId)

        assertThat(result).isEqualTo(
            CompleteFolderInfo(
                numOfFiles = Int.MAX_VALUE,
                numOfFolders = Int.MAX_VALUE,
                totalSizeInBytes = Long.MAX_VALUE,
                creationTime = Long.MAX_VALUE,
            )
        )
    }

    @Test
    fun `test that invoke handles negative values correctly`() = runTest {
        val nodeId = NodeId(456L)
        val creationTime = -1L
        val folderNode = mock<TypedFolderNode>()
        val folderTreeInfo = FolderTreeInfo(
            numberOfFiles = -5,
            numberOfFolders = -3,
            totalCurrentSizeInBytes = -100L,
            numberOfVersions = 0,
            sizeOfPreviousVersionsInBytes = 0L
        )

        whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
        whenever(folderNode.creationTime).thenReturn(creationTime)
        whenever(getFolderTreeInfo(any())).thenReturn(folderTreeInfo)

        val result = underTest(nodeId)

        assertThat(result).isEqualTo(
            CompleteFolderInfo(
                numOfFiles = -5,
                numOfFolders = -3,
                totalSizeInBytes = -100L,
                creationTime = -1L,
            )
        )
    }

    @Test
    fun `test that invoke returns CompleteFolderInfo with empty folder when folder tree info is null`() =
        runTest {
            val nodeId = NodeId(789L)
            val creationTime = 5000L
            val folderNode = mock<TypedFolderNode>()
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(folderNode)
            whenever(folderNode.creationTime).thenReturn(creationTime)
            whenever(getFolderTreeInfo(any())).thenReturn(null)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(
                CompleteFolderInfo(
                    numOfFiles = 0,
                    numOfFolders = 0,
                    totalSizeInBytes = 0L,
                    creationTime = creationTime,
                )
            )
        }
}
