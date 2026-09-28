package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNodeShareDataUseCaseTest {
    private lateinit var underTest: GetNodeShareDataUseCase
    private val mockGetUnverifiedIncomingShares = mock<GetUnverifiedIncomingShares>()
    private val mockGetUnverifiedOutgoingShares = mock<GetUnverifiedOutgoingShares>()
    private val mockIsOutShareUseCase = mock<IsOutShareUseCase>()

    @BeforeAll
    fun init() {
        underTest = GetNodeShareDataUseCase(
            mockGetUnverifiedIncomingShares,
            mockGetUnverifiedOutgoingShares,
            mockIsOutShareUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            mockGetUnverifiedIncomingShares,
            mockGetUnverifiedOutgoingShares,
            mockIsOutShareUseCase
        )
    }

    @Test
    fun `test that returns outgoing share data for out share node`() = runTest {
        val nodeId = NodeId(123L)
        val mockShareData = mock<ShareData> {
            on { nodeHandle } doReturn nodeId.longValue
            on { user } doReturn "test@example.com"
        }
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn false
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(true)
        whenever(mockGetUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(
            listOf(
                mockShareData
            )
        )

        val result = underTest(mockNode)

        assertThat(result).isEqualTo(mockShareData)
    }

    @Test
    fun `test that returns incoming share data for incoming share node`() = runTest {
        val nodeId = NodeId(456L)
        val mockShareData = mock<ShareData> {
            on { nodeHandle } doReturn nodeId.longValue
            on { user } doReturn "incoming@example.com"
        }
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn true
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(false)
        whenever(mockGetUnverifiedIncomingShares(SortOrder.ORDER_NONE)).thenReturn(
            listOf(
                mockShareData
            )
        )

        val result = underTest(mockNode)

        assertThat(result).isEqualTo(mockShareData)
    }

    @Test
    fun `test that returns null for node that is neither out share nor incoming share`() = runTest {
        val nodeId = NodeId(789L)
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn false
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(false)

        val result = underTest(mockNode)

        assertThat(result).isNull()
    }

    @Test
    fun `test that returns null when no matching outgoing share data found`() = runTest {
        val nodeId = NodeId(123L)
        val mockShareData = mock<ShareData> {
            on { nodeHandle } doReturn 999L // Different node ID
            on { user } doReturn "other@example.com"
        }
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn false
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(true)
        whenever(mockGetUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(
            listOf(
                mockShareData
            )
        )

        val result = underTest(mockNode)

        assertThat(result).isNull()
    }

    @Test
    fun `test that returns null when no matching incoming share data found`() = runTest {
        val nodeId = NodeId(456L)
        val mockShareData = mock<ShareData> {
            on { nodeHandle } doReturn 999L // Different node ID
            on { user } doReturn "other@example.com"
        }
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn true
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(false)
        whenever(mockGetUnverifiedIncomingShares(SortOrder.ORDER_NONE)).thenReturn(
            listOf(
                mockShareData
            )
        )

        val result = underTest(mockNode)

        assertThat(result).isNull()
    }

    @Test
    fun `test that returns null when getUnverifiedOutgoingShares throws exception`() = runTest {
        val nodeId = NodeId(123L)
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn false
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(true)
        whenever(mockGetUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenThrow(RuntimeException("Network error"))

        val result = underTest(mockNode)

        assertThat(result).isNull()
    }

    @Test
    fun `test that returns null when getUnverifiedIncomingShares throws exception`() = runTest {
        val nodeId = NodeId(456L)
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn true
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(false)
        whenever(mockGetUnverifiedIncomingShares(SortOrder.ORDER_NONE)).thenThrow(RuntimeException("Network error"))

        val result = underTest(mockNode)

        assertThat(result).isNull()
    }

    @Test
    fun `test that returns first matching share data when multiple shares exist`() = runTest {
        val nodeId = NodeId(123L)
        val mockShareData1 = mock<ShareData> {
            on { nodeHandle } doReturn nodeId.longValue
            on { user } doReturn "first@example.com"
        }
        val mockShareData2 = mock<ShareData> {
            on { nodeHandle } doReturn nodeId.longValue
            on { user } doReturn "second@example.com"
        }
        val mockNode = mock<FolderNode> {
            on { id } doReturn nodeId
            on { isIncomingShare } doReturn false
        }

        whenever(mockIsOutShareUseCase(mockNode)).thenReturn(true)
        whenever(mockGetUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(
            listOf(
                mockShareData1,
                mockShareData2
            )
        )

        val result = underTest(mockNode)

        assertThat(result).isEqualTo(mockShareData1)
    }
}
