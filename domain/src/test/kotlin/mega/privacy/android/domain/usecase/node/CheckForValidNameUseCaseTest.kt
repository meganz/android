package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.RegexRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.regex.Pattern
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckForValidNameUseCaseTest {

    private val nodeExistsInParentUseCase: NodeExistsInParentUseCase = mock()
    private val regexRepository: RegexRepository = mock()

    private val invalidNamePattern = Pattern.compile(INVALID_NAME_REGEX)
    private val underTest = CheckForValidNameUseCase(nodeExistsInParentUseCase, regexRepository)

    @ParameterizedTest(name = "Check for valid name {0}")
    @MethodSource("provideParams")
    fun `test provided name returns appropriate type`(
        providedName: String,
        node: UnTypedNode,
        expected: ValidNameType,
    ) = runTest {
        whenever(nodeExistsInParentUseCase(node, providedName)).thenReturn(false)
        whenever(regexRepository.invalidNamePattern).thenReturn(invalidNamePattern)

        val actual = underTest(providedName, node)
        Truth.assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `test that if same name found in node list returns NAME_ALREADY_EXISTS error type`() =
        runTest {
            val providedName = "SameName.txt"
            val node = mock<FileNode>()
            whenever(nodeExistsInParentUseCase(node, providedName)).thenReturn(true)
            val actual = underTest(providedName, node)
            Truth.assertThat(actual).isEqualTo(ValidNameType.NAME_ALREADY_EXISTS)
        }

    private fun provideParams() =
        Stream.of(
            Arguments.of(" ", mock<FileNode>(), ValidNameType.BLANK_NAME),
            Arguments.of("SomeInvalidName/*", mock<FileNode>(), ValidNameType.INVALID_NAME),
            Arguments.of("Folder", mock<FolderNode>(), ValidNameType.NO_ERROR),
            Arguments.of("no extension", mock<FileNode>(), ValidNameType.NO_EXTENSION),
            Arguments.of(
                "changeInExtension.jpeg",
                mock<FileNode> {
                    whenever(it.type).thenReturn(PdfFileTypeInfo)
                },
                ValidNameType.DIFFERENT_EXTENSION
            ),
            Arguments.of("Proper rename.pdf", mock<FileNode> {
                whenever(it.type).thenReturn(PdfFileTypeInfo)
            }, ValidNameType.NO_ERROR)
        )

    companion object {
        private const val INVALID_NAME_REGEX = "[*|\\?:\"<>\\\\\\\\/]"
    }
}