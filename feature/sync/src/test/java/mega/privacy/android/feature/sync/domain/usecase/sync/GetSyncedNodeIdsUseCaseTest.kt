package mega.privacy.android.feature.sync.domain.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncedNodeIdsUseCaseTest {

    private lateinit var underTest: GetSyncedNodeIdsUseCase

    private val syncRepository = mock<SyncRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetSyncedNodeIdsUseCase(syncRepository = syncRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository)
    }

    @Test
    fun `test that empty list is returned when no synced folders exist`() = runTest {
        whenever(syncRepository.getSyncedNodeIds()).thenReturn(emptyList())

        val result = underTest.invoke()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that single node id is returned when one synced folder exists`() = runTest {
        val nodeId = NodeId(123L)
        whenever(syncRepository.getSyncedNodeIds()).thenReturn(listOf(nodeId))

        val result = underTest.invoke()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(nodeId)
    }

    @Test
    fun `test that multiple node ids are returned when multiple synced folders exist`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L), NodeId(3L))
        whenever(syncRepository.getSyncedNodeIds()).thenReturn(nodeIds)

        val result = underTest.invoke()

        assertThat(result).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L)).inOrder()
    }
}
