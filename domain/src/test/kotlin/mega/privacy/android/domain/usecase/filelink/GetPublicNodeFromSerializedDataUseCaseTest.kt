package mega.privacy.android.domain.usecase.filelink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPublicNodeFromSerializedDataUseCaseTest {
    private lateinit var underTest: GetPublicNodeFromSerializedDataUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val mapNodeToPublicLinkUseCase = mock<MapNodeToPublicLinkUseCase>()

    @BeforeAll
    fun initialize() {
        underTest = GetPublicNodeFromSerializedDataUseCase(
            nodeRepository,
            mapNodeToPublicLinkUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        nodeRepository,
        mapNodeToPublicLinkUseCase,
    )

    @Test
    fun `test that correct mapped node from repository is returned`() = runTest {
        val serializedData = "serialized data"
        val fileNode = mock<FileNode>()
        val expected = mock<PublicLinkFile>()
        whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(fileNode)
        whenever(mapNodeToPublicLinkUseCase(fileNode, null)).thenReturn(expected)
        val actual = underTest(serializedData)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that null is returned when repository returns null`() = runTest {
        val serializedData = "serialized data"
        whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(null)
        val actual = underTest(serializedData)
        assertThat(actual).isNull()
    }
}