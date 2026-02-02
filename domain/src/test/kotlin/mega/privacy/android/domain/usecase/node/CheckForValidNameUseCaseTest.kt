package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.RegexRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.util.regex.Pattern
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckForValidNameUseCaseTest {

    private lateinit var underTest: CheckForValidNameUseCase

    private val nodeExistsInParentUseCase: NodeExistsInParentUseCase = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()
    private val regexRepository: RegexRepository = mock()

    private val invalidNamePattern = Pattern.compile(INVALID_NAME_REGEX)

    private val fileNode = mock<FileNode> {
        on { type } doReturn PdfFileTypeInfo
    }
    private val nodeId = NodeId(1234L)
    private val folderNode = mock<FolderNode> {
        on { id } doReturn nodeId
    }

    @BeforeAll
    fun setup() {
        underTest = CheckForValidNameUseCase(
            nodeExistsInParentUseCase,
            nodeExistsInCurrentLocationUseCase,
            regexRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeExistsInParentUseCase,
            nodeExistsInCurrentLocationUseCase,
            regexRepository,
        )

        wheneverBlocking { regexRepository.invalidNamePattern } doReturn invalidNamePattern
    }

    @ParameterizedTest(name = "Check for valid name {0}")
    @MethodSource("provideParams")
    fun `test provided name returns appropriate type`(
        providedName: String,
        node: UnTypedNode,
        expected: InvalidNameType,
    ) = runTest {
        whenever(nodeExistsInCurrentLocationUseCase(node.id, providedName)).thenReturn(false)
        whenever(nodeExistsInParentUseCase(node, providedName)).thenReturn(false)

        assertThat(expected).isEqualTo(underTest(providedName, node))
    }

    @Test
    fun `test that if same name found in node list for file returns NAME_ALREADY_EXISTS error type`() =
        runTest {
            val providedName = "SameName.txt"

            whenever(nodeExistsInParentUseCase(fileNode, providedName)).thenReturn(true)

            assertThat(
                underTest(
                    providedName,
                    fileNode
                )
            ).isEqualTo(InvalidNameType.NAME_ALREADY_EXISTS)

            verifyNoInteractions(nodeExistsInCurrentLocationUseCase)
        }

    @Test
    fun `test that if same name found in node list for folder returns NAME_ALREADY_EXISTS error type`() =
        runTest {
            val providedName = "Folder name"

            whenever(nodeExistsInCurrentLocationUseCase(nodeId, providedName)).thenReturn(true)

            assertThat(underTest(providedName, folderNode))
                .isEqualTo(InvalidNameType.NAME_ALREADY_EXISTS)

            verifyNoInteractions(nodeExistsInParentUseCase)
        }

    private fun provideParams() =
        Stream.of(
            Arguments.of(" ", fileNode, InvalidNameType.BLANK_NAME),
            Arguments.of("", folderNode, InvalidNameType.BLANK_NAME),
            Arguments.of(".", fileNode, InvalidNameType.DOT_NAME),
            Arguments.of(".", folderNode, InvalidNameType.DOT_NAME),
            Arguments.of("..", fileNode, InvalidNameType.DOUBLE_DOT_NAME),
            Arguments.of("..", folderNode, InvalidNameType.DOUBLE_DOT_NAME),
            Arguments.of("SomeInvalidName/*", fileNode, InvalidNameType.INVALID_NAME),
            Arguments.of("SomeInvalidName/*", folderNode, InvalidNameType.INVALID_NAME),
            Arguments.of("no extension", fileNode, InvalidNameType.NO_EXTENSION),
            Arguments.of("changeInExtension.jpeg", fileNode, InvalidNameType.DIFFERENT_EXTENSION),
            Arguments.of("Proper rename.pdf", fileNode, InvalidNameType.VALID),
            Arguments.of("Folder", folderNode, InvalidNameType.VALID),
        )

    companion object {
        private const val INVALID_NAME_REGEX = "[*|\\?:\"<>\\\\\\\\/]"
    }
}