package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByNodeIdUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodePreviewFileUseCaseTest {
    private val fileSystemRepository: FileSystemRepository = mock()
    private val cacheRepository: CacheRepository = mock()
    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase = mock()
    private val getOfflineFileUseCase: GetOfflineFileUseCase = mock()

    private val underTest = GetNodePreviewFileUseCase(
        fileSystemRepository = fileSystemRepository,
        cacheRepository = cacheRepository,
        getOfflineNodeInformationByNodeIdUseCase = getOfflineNodeInformationByNodeIdUseCase,
        getOfflineFileUseCase = getOfflineFileUseCase
    )

    @ParameterizedTest(name = "test path for file is expected {0}")
    @MethodSource("provideParams")
    fun `test that file path is same as expected and actual`(
        node: TypedFileNode,
        expected: File,
    ) = runTest {
        if (node.id.longValue == 1234L)
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(expected)
        else {
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(null)
        }
        whenever(cacheRepository.getPreviewFile(node.name)).thenReturn(expected)
        val actual = underTest(node)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that offline local file is returned when node is available offline`() = runTest {
        val node = mock<DefaultTypedFileNode> {
            whenever(it.isAvailableOffline).thenReturn(true)
            whenever(it.id).thenReturn(NodeId(1234))
        }
        val file = mock<File>()
        val offlineNode = mock<OtherOfflineNodeInformation>()
        whenever(getOfflineNodeInformationByNodeIdUseCase(node.id)).thenReturn(offlineNode)
        whenever(getOfflineFileUseCase(offlineNode)).thenReturn(file)
        val actual = underTest(node)
        assertThat(actual).isEqualTo(file)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(mock<DefaultTypedFileNode> {
            whenever(it.name).thenReturn("some name")
        }, File("cached path")),
        Arguments.of(mock<DefaultTypedFileNode> {
            whenever(it.id).thenReturn(NodeId(1234))
            whenever(it.name).thenReturn("some name")
        }, File("local path")),
    )
}