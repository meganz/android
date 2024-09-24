package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.mock
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
            isMedia = true,
            isUpdate = true,
            timestamp = 0L,
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

        val result = underTest.getRecentActions(false)

        assertThat(result.size).isEqualTo(expected.size)
    }
}
