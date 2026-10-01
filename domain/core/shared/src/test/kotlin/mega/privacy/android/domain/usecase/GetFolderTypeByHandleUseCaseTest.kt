package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFolderTypeByHandleUseCaseTest {
    private lateinit var underTest: GetFolderTypeByHandleUseCase
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val addNodeType = mock<AddNodeType>()

    @BeforeAll
    fun setUp() {
        underTest = GetFolderTypeByHandleUseCase(
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            addNodeType = addNodeType,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(getNodeByHandleUseCase, addNodeType)
    }

    @ParameterizedTest(name = " when folder type is {0}")
    @MethodSource("provideFolderType")
    fun `test that the result returns correctly`(
        folderType: FolderType,
    ) = runTest {
        val testHandle = 1234L
        val testUnTypedNode = mock<FolderNode>()
        val testTypedFolderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(folderType)
        }

        whenever(getNodeByHandleUseCase(testHandle)).thenReturn(testUnTypedNode)
        whenever(addNodeType(testUnTypedNode)).thenReturn(testTypedFolderNode)

        val actual = underTest(testHandle)
        assertThat(actual).isEqualTo(folderType)
    }

    private fun provideFolderType() = listOf(
        Arguments.of(FolderType.Default),
        Arguments.of(FolderType.MediaSyncFolder),
        Arguments.of(FolderType.ChatFilesFolder),
        Arguments.of(FolderType.RootBackup),
        Arguments.of(FolderType.ChildBackup),
        Arguments.of(FolderType.Sync),
    )

    @Test
    fun `test that the result returns null when the node is a file`() = runTest {
        val testHandle = 1234L
        val testUnTypedNode = mock<FileNode>()
        val testTypedFileNode = mock<TypedFileNode>()

        whenever(getNodeByHandleUseCase(testHandle)).thenReturn(testUnTypedNode)
        whenever(addNodeType(testUnTypedNode)).thenReturn(testTypedFileNode)

        val actual = underTest(testHandle)
        assertThat(actual).isNull()
    }
}