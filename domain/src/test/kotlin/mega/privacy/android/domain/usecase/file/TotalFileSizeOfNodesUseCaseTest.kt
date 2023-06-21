package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TotalFileSizeOfNodesUseCaseTest {

    private val getFolderTreeInfo = mock<GetFolderTreeInfo>()
    private val folderTreeInfo = mock<FolderTreeInfo>()

    private lateinit var underTest: TotalFileSizeOfNodesUseCase

    @BeforeAll
    fun setup() {
        underTest = TotalFileSizeOfNodesUseCase(getFolderTreeInfo)
    }

    @BeforeEach
    fun resetMocks() = reset(getFolderTreeInfo, folderTreeInfo)

    @ParameterizedTest
    @MethodSource("provideParameters")
    fun `test that file size is calculated correctly when different sets of nodes are provided`(
        nodes: List<Node>,
        expectedSize: Long,
    ) = runTest {
        stubFolderTreeInfoResponse()
        Truth.assertThat(underTest(nodes)).isEqualTo(expectedSize)
    }

    private fun provideParameters() = listOf(
        Arguments.of(emptyList<Node>(), 0L),
        Arguments.of(listOf(mockFile()), FILE_SIZE),
        Arguments.of(listOf(mockFolder()), FOLDER_SIZE),
        Arguments.of(listOf(mockFolder(), mockFile()), FOLDER_SIZE + FILE_SIZE),
        Arguments.of(
            listOf(mockFile(), mockFile(), mockFolder()),
            FILE_SIZE + FILE_SIZE + FOLDER_SIZE
        ),
    )

    private suspend fun stubFolderTreeInfoResponse() {
        whenever(folderTreeInfo.totalCurrentSizeInBytes).thenReturn(FOLDER_SIZE)
        whenever(getFolderTreeInfo(any())).thenReturn(folderTreeInfo)
    }

    private fun mockFile() = mock<DefaultTypedFileNode> {
        on { size }.thenReturn(FILE_SIZE)
    }

    private fun mockFolder() = mock<DefaultTypedFolderNode>()

    companion object {
        private const val FILE_SIZE = 1024L
        private const val FOLDER_SIZE = 1536L
    }
}