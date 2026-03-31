package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.recentactions.NodeInfoForRecentActionsMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultRecentActionsRepositoryTest {
    private lateinit var underTest: RecentActionsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val recentActionsMapper = mock<RecentActionsMapper>()
    private val recentActionBucketMapper = mock<RecentActionBucketMapper>()
    private val nodeInfoForRecentActionsMapper = mock<NodeInfoForRecentActionsMapper>()

    @Before
    fun setUp() {
        underTest = DefaultRecentActionsRepository(
            megaApiGateway = megaApiGateway,
            recentActionsMapper = recentActionsMapper,
            recentActionBucketMapper = recentActionBucketMapper,
            nodeInfoForRecentActionsMapper = nodeInfoForRecentActionsMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that get recent actions returns the result of api recentActions`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(4) }
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val list = (1..4).map {
            val mock = mock<MegaRecentActionBucket> {
                on { nodes } doReturn mock<MegaNodeList>()
            }
            whenever(megaApiGateway.copyBucket(mock)).thenReturn(mock)
            mock
        }
        val recentActionBucket = RecentActionBucketUnTyped(
            id = "M_true-U_true-D_1970-01-01-UE_1-PNH_1",
            isMedia = true,
            isUpdate = true,
            timestamp = 0L,
            dateTimestamp = 0L,
            parentNodeId = NodeId(1L),
            userEmail = "1",
            nodes = listOf(mock<FileNode>())
        )
        val expected = (1..4).map { recentActionBucket }

        whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any(), any())).thenAnswer {
            (it.arguments[3] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }
        whenever(megaApiGateway.copyBucketList(any())).thenReturn(mock())
        whenever(recentActionsMapper(any())).thenReturn(list)
        whenever(megaApiGateway.getNodesFromMegaNodeList(any())).thenReturn(listOf(mock<MegaNode>()))
        whenever(recentActionBucketMapper(any(), any())).thenReturn(recentActionBucket)

        val result = underTest.getRecentActions(false, 500)

        assertThat(result.size).isEqualTo(expected.size)
    }

    @Test
    fun `test that maxBucketCount limits the number of buckets returned`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(10) }
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val list = (1..10).map {
            val mock = mock<MegaRecentActionBucket> {
                on { nodes } doReturn mock<MegaNodeList>()
            }
            whenever(megaApiGateway.copyBucket(mock)).thenReturn(mock)
            mock
        }
        val recentActionBucket = RecentActionBucketUnTyped(
            id = "M_true-U_true-D_1970-01-01-UE_1-PNH_1",
            isMedia = true,
            isUpdate = true,
            timestamp = 0L,
            dateTimestamp = 0L,
            parentNodeId = NodeId(1L),
            userEmail = "1",
            nodes = listOf(mock<FileNode>())
        )

        whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any(), any())).thenAnswer {
            (it.arguments[3] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }
        whenever(megaApiGateway.copyBucketList(any())).thenReturn(mock())
        whenever(recentActionsMapper(any())).thenReturn(list)
        whenever(megaApiGateway.getNodesFromMegaNodeList(any())).thenReturn(listOf(mock<MegaNode>()))
        whenever(recentActionBucketMapper(any(), any())).thenReturn(recentActionBucket)

        val result = underTest.getRecentActions(false, maxBucketCount = 5)

        // Should only return 5 buckets even though 10 were fetched
        assertThat(result.size).isEqualTo(5)
    }

    @Test
    fun `test that maxBucketCount returns all buckets when count is larger than available`() =
        runTest {
            val megaApiJava = mock<MegaApiJava>()
            val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(3) }
            val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
            val list = (1..3).map {
                val mock = mock<MegaRecentActionBucket> {
                    on { nodes } doReturn mock<MegaNodeList>()
                }
                whenever(megaApiGateway.copyBucket(mock)).thenReturn(mock)
                mock
            }
            val recentActionBucket = RecentActionBucketUnTyped(
                id = "M_true-U_true-D_1970-01-01-UE_1-PNH_1",
                isMedia = true,
                isUpdate = true,
                timestamp = 0L,
                dateTimestamp = 0L,
                parentNodeId = NodeId(1L),
                userEmail = "1",
                nodes = listOf(mock<FileNode>())
            )

            whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any(), any())).thenAnswer {
                (it.arguments[3] as MegaRequestListenerInterface).onRequestFinish(
                    megaApiJava,
                    request,
                    error
                )
            }
            whenever(megaApiGateway.copyBucketList(any())).thenReturn(mock())
            whenever(recentActionsMapper(any())).thenReturn(list)
            whenever(megaApiGateway.getNodesFromMegaNodeList(any())).thenReturn(listOf(mock<MegaNode>()))
            whenever(recentActionBucketMapper(any(), any())).thenReturn(recentActionBucket)

            val result = underTest.getRecentActions(false, maxBucketCount = 10)

            // Should return all 3 buckets even though maxBucketCount is 10
            assertThat(result.size).isEqualTo(3)
        }

    @Test
    fun `test that maxBucketCount is applied before fetching nodes`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(10) }
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val nodeList1 = mock<MegaNodeList>()
        val nodeList2 = mock<MegaNodeList>()
        val nodeList3 = mock<MegaNodeList>()
        val nodeList4 = mock<MegaNodeList>()
        val nodeList5 = mock<MegaNodeList>()
        val nodeList6 = mock<MegaNodeList>()

        val list = (1..10).map { index ->
            val nodeList = when (index) {
                1 -> nodeList1
                2 -> nodeList2
                3 -> nodeList3
                4 -> nodeList4
                5 -> nodeList5
                6 -> nodeList6
                else -> mock<MegaNodeList>()
            }
            val mock = mock<MegaRecentActionBucket> {
                on { nodes } doReturn nodeList
            }
            whenever(megaApiGateway.copyBucket(mock)).thenReturn(mock)
            mock
        }
        val recentActionBucket = RecentActionBucketUnTyped(
            id = "M_true-U_true-D_1970-01-01-UE_1-PNH_1",
            isMedia = true,
            isUpdate = true,
            timestamp = 0L,
            dateTimestamp = 0L,
            parentNodeId = NodeId(1L),
            userEmail = "1",
            nodes = listOf(mock<FileNode>())
        )

        whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any(), any())).thenAnswer {
            (it.arguments[3] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }
        whenever(megaApiGateway.copyBucketList(any())).thenReturn(mock())
        whenever(recentActionsMapper(any())).thenReturn(list)
        whenever(megaApiGateway.getNodesFromMegaNodeList(any())).thenReturn(listOf(mock<MegaNode>()))
        whenever(recentActionBucketMapper(any(), any())).thenReturn(recentActionBucket)

        underTest.getRecentActions(false, maxBucketCount = 5)

        // Verify that getNodesFromMegaNodeList is only called for the first 5 buckets
        verify(megaApiGateway).getNodesFromMegaNodeList(nodeList1)
        verify(megaApiGateway).getNodesFromMegaNodeList(nodeList2)
        verify(megaApiGateway).getNodesFromMegaNodeList(nodeList3)
        verify(megaApiGateway).getNodesFromMegaNodeList(nodeList4)
        verify(megaApiGateway).getNodesFromMegaNodeList(nodeList5)
        // Verify it's NOT called for bucket 6 (beyond maxBucketCount)
        verify(megaApiGateway, never()).getNodesFromMegaNodeList(nodeList6)
    }

    @Test
    fun `test that getRecentActionBucketById returns matching bucket`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val targetId = "some-bucket-id"
        val nodeList = mock<MegaNodeList>()

        val megaBucket = mock<MegaRecentActionBucket> {
            on { isMedia }.thenReturn(true)
            on { isUpdate }.thenReturn(true)
            on { timestamp }.thenReturn(2000L)
            on { userEmail }.thenReturn("user2@example.com")
            on { parentHandle }.thenReturn(200L)
            on { nodes } doReturn nodeList
        }
        val copiedBucket = mock<MegaRecentActionBucket> {
            on { isMedia }.thenReturn(true)
            on { isUpdate }.thenReturn(true)
            on { timestamp }.thenReturn(2000L)
            on { userEmail }.thenReturn("user2@example.com")
            on { parentHandle }.thenReturn(200L)
            on { nodes } doReturn nodeList
        }

        val bucketList = mock<MegaRecentActionBucketList> {
            on { size() }.thenReturn(1)
            on { get(0) }.thenReturn(megaBucket)
        }
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        whenever(megaApiGateway.getRecentBucketById(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }
        whenever(megaApiGateway.copyBucketList(any())).thenReturn(bucketList)
        whenever(megaApiGateway.copyBucket(megaBucket)).thenReturn(copiedBucket)
        whenever(megaApiGateway.getNodesFromMegaNodeList(nodeList)).thenReturn(listOf(mock<MegaNode>()))

        val expectedBucket = RecentActionBucketUnTyped(
            id = targetId,
            isMedia = true,
            isUpdate = true,
            timestamp = 2000L,
            dateTimestamp = 0L,
            parentNodeId = NodeId(200L),
            userEmail = "user2@example.com",
            nodes = listOf(mock<FileNode>())
        )
        whenever(recentActionBucketMapper(any(), any())).thenReturn(expectedBucket)

        val result = underTest.getRecentActionBucketById(targetId, false)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(targetId)
        assertThat(result?.isMedia).isTrue()
        assertThat(result?.isUpdate).isTrue()
        assertThat(result?.timestamp).isEqualTo(2000L)
        assertThat(result?.userEmail).isEqualTo("user2@example.com")
    }

    @Test
    fun `test that getRecentActionBucketById returns null when bucket not found`() =
        runTest {
            val megaApiJava = mock<MegaApiJava>()
            val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(0) }
            val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

            whenever(megaApiGateway.getRecentBucketById(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    megaApiJava,
                    request,
                    error
                )
            }
            whenever(megaApiGateway.copyBucketList(any())).thenReturn(bucketList)

            val result = underTest.getRecentActionBucketById("non-existent-id", false)

            assertThat(result).isNull()
        }

    @Test
    fun `test that monitorRecentActivityCleared emits after clearRecentActions succeeds`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val timestamp = 1234567890L
        val request = mock<MegaRequest> { on { number }.thenReturn(timestamp) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        whenever(megaApiGateway.clearRecentActions(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }

        val emissions = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            underTest.monitorRecentActivityCleared().collect { emissions.add(it) }
        }

        underTest.clearRecentActions(timestamp)

        assertThat(emissions).hasSize(1)
    }

    @Test
    fun `test that getRecentActionBucketById calls gateway with correct id`() =
        runTest {
            val megaApiJava = mock<MegaApiJava>()
            val targetId = "test-bucket-id"
            val nodeList = mock<MegaNodeList>()

            val megaBucket = mock<MegaRecentActionBucket> {
                on { nodes } doReturn nodeList
            }
            val copiedBucket = mock<MegaRecentActionBucket> {
                on { nodes } doReturn nodeList
            }

            val bucketList = mock<MegaRecentActionBucketList> {
                on { size() }.thenReturn(1)
                on { get(0) }.thenReturn(megaBucket)
            }
            val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

            whenever(megaApiGateway.getRecentBucketById(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    megaApiJava,
                    request,
                    error
                )
            }
            whenever(megaApiGateway.copyBucketList(any())).thenReturn(bucketList)
            whenever(megaApiGateway.copyBucket(megaBucket)).thenReturn(copiedBucket)
            whenever(megaApiGateway.getNodesFromMegaNodeList(nodeList)).thenReturn(listOf(mock<MegaNode>()))
            whenever(recentActionBucketMapper(any(), any())).thenReturn(mock())

            underTest.getRecentActionBucketById(targetId, false)

            verify(megaApiGateway).getRecentBucketById(eq(targetId), any(), any())
        }
}
