package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExportChatNodesUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private lateinit var underTest: ExportChatNodesUseCase

    @BeforeAll
    fun setup() {
        underTest = ExportChatNodesUseCase(nodeRepository)
    }

    @Test
    fun `test that export nodes returns correctly`() = runTest {
        val node1 = mock<ChatImageFile>()
        val node2 = mock<ChatImageFile>()
        val link1 = "link1"
        val link2 = "link2"
        whenever(nodeRepository.exportNode(node1)).thenReturn(link1)
        whenever(nodeRepository.exportNode(node2)).thenReturn(link2)
        val expected = mapOf(node1.id to link1, node2.id to link2)
        val actual = underTest.invoke(listOf(node1, node2))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that export nodes returns correctly when one of nodes has empty link`() = runTest {
        val node1 = mock<ChatImageFile>()
        val node2 = mock<ChatImageFile>()
        val link1 = "link1"
        whenever(nodeRepository.exportNode(node1)).thenReturn(link1)
        whenever(nodeRepository.exportNode(node2)).thenReturn("")
        val expected = mapOf(node1.id to link1)
        val actual = underTest.invoke(listOf(node1, node2))
        assertThat(actual).isEqualTo(expected)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }
}