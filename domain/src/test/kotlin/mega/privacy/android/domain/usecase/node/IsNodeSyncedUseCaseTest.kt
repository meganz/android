package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsNodeSyncedUseCaseTest {

    private lateinit var underTest: IsNodeSyncedUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsNodeSyncedUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository
        )
    }

    @Test
    fun `test that proper value is returned depending on repository value received`() = runTest {
        whenever(nodeRepository.isNodeSynced(NodeId(1234L))).thenReturn(true)
        whenever(nodeRepository.isNodeSynced(NodeId(4321L))).thenReturn(false)
        assertThat(underTest(NodeId(1234L))).isTrue()
        assertThat(underTest(NodeId(4321L))).isFalse()
    }
}