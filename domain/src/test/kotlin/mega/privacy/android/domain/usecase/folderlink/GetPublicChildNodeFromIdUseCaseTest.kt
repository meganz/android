package mega.privacy.android.domain.usecase.folderlink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPublicChildNodeFromIdUseCaseTest {

    private lateinit var underTest: GetPublicChildNodeFromIdUseCase
    private val folderLinkRepository = mock<FolderLinkRepository>()
    private val mapNodeToPublicLinkUseCase = mock<MapNodeToPublicLinkUseCase>()


    @BeforeAll
    fun initialize() {
        underTest = GetPublicChildNodeFromIdUseCase(
            folderLinkRepository,
            mapNodeToPublicLinkUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        folderLinkRepository,
        mapNodeToPublicLinkUseCase,
    )

    @Test
    fun `test that mapped node from folder link repository is returned`() = runTest {
        val id = NodeId(1L)
        val unTypedNode = mock<FileNode>()
        val expected = mock<PublicLinkFile>()
        whenever(folderLinkRepository.getChildNode(id)).thenReturn(unTypedNode)
        whenever(mapNodeToPublicLinkUseCase(unTypedNode, null)).thenReturn(expected)
        val actual = underTest(id)
        assertThat(actual).isEqualTo(expected)
    }
}