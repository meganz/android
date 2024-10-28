package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRecentActionNodesTest {
    private lateinit var underTest: GetRecentActionNodes
    private val nodes = (0L..5L).map { value ->
        mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(value))
        }
    }
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on { invoke() }.thenReturn(flowOf(AccountDetail()))
    }

    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @Before
    fun setUp() {
        runBlocking {
            commonStub()
        }
        underTest = DefaultGetRecentActionNodes(
            ioDispatcher = UnconfinedTestDispatcher(),
            getNodeByHandle = mock(),
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    private suspend fun commonStub() = runTest {
        whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(false)
    }

    @Test
    fun `test that if getThumbnail succeed for each element,the list returned contains as many elements as the list given in parameter`() =
        runTest {
            assertThat(underTest.invoke(nodes).size).isEqualTo(nodes.size)
        }
}
