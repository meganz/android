package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.GetAttachableNodeIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodesToAttachUseCaseTest {
    private val getAttachableNodeIdUseCase: GetAttachableNodeIdUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private lateinit var underTest: GetNodesToAttachUseCase

    @BeforeAll
    fun setup() {
        underTest = GetNodesToAttachUseCase(
            getAttachableNodeIdUseCase = getAttachableNodeIdUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getAttachableNodeIdUseCase,
            monitorStorageStateEventUseCase
        )
    }

    @Test
    fun `test that exception throw correctly when storage state is paywall`() = runTest {
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 1L,
                    eventString = "",
                    number = 0L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.PayWall
                )
            )
        )
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        assertThrows<StorageStatePayWallException> {
            underTest(listOf(nodeId))
        }
    }

    @Test
    fun `test that returns list of node ids when storage state is not paywall`() = runTest {
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 1L,
                    eventString = "",
                    number = 0L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.Unknown
                )
            )
        )

        val node = mock<TypedFileNode>()
        val nodeId = NodeId(1L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        whenever(getAttachableNodeIdUseCase(node)).thenReturn(nodeId)
        Truth.assertThat(underTest(listOf(nodeId))).isEqualTo(listOf(nodeId))
    }
}