package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.folderpreference.GetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.photos.MonitorCloudDriveNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveImageNodeFetcherTest {
    private lateinit var underTest: CloudDriveImageNodeFetcher

    private val monitorCloudDriveNodesUseCase = mock<MonitorCloudDriveNodesUseCase>()
    private val getFolderSortOrderUseCase = mock<GetFolderSortOrderUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CloudDriveImageNodeFetcher(
            monitorCloudDriveNodesUseCase = monitorCloudDriveNodesUseCase,
            getFolderSortOrderUseCase = getFolderSortOrderUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorCloudDriveNodesUseCase, getFolderSortOrderUseCase)
    }

    @Test
    fun `test that monitorImageNodes uses the sort order of the parent folder`() = runTest {
        val parentId = NodeId(42L)
        val nodes = listOf(mock<ImageNode>())
        val bundle = mock<Bundle> {
            on { getLong(CloudDriveImageNodeFetcher.PARENT_ID) } doReturn parentId.longValue
        }
        whenever(getFolderSortOrderUseCase(parentId)).thenReturn(SortOrder.ORDER_SIZE_DESC)
        whenever(monitorCloudDriveNodesUseCase(parentId, SortOrder.ORDER_SIZE_DESC))
            .thenReturn(flowOf(nodes))

        underTest.monitorImageNodes(bundle).test {
            assertThat(awaitItem()).isEqualTo(nodes)
            awaitComplete()
        }
    }
}
