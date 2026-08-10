package mega.privacy.android.domain.usecase.node.hiddennode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SensitiveNodeShareWarning
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetShareFolderSensitiveWarningUseCaseTest {
    private lateinit var underTest: GetShareFolderSensitiveWarningUseCase
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val getShareFolderSensitiveWarningTypeUseCase =
        mock<GetShareFolderSensitiveWarningTypeUseCase>()

    @Before
    fun setUp() {
        underTest = GetShareFolderSensitiveWarningUseCase(
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            getShareFolderSensitiveWarningTypeUseCase = getShareFolderSensitiveWarningTypeUseCase,
        )
    }

    @Test
    fun `test that invoke passes the enabled flag and node ids to the type use case`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(getShareFolderSensitiveWarningTypeUseCase(nodeIds, true))
            .thenReturn(SensitiveNodeShareWarning.Folders)

        val result = underTest(nodeIds)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folders)
        verify(getShareFolderSensitiveWarningTypeUseCase)(nodeIds, true)
    }

    @Test
    fun `test that invoke passes false to the type use case when hidden nodes are not enabled`() =
        runTest {
            val nodeIds = listOf(NodeId(1L))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(getShareFolderSensitiveWarningTypeUseCase(nodeIds, false))
                .thenReturn(SensitiveNodeShareWarning.None)

            val result = underTest(nodeIds)

            assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
            verify(getShareFolderSensitiveWarningTypeUseCase)(nodeIds, false)
        }

    @Test
    fun `test that invoke uses the first emission of the enabled flow`() = runTest {
        val nodeIds = listOf(NodeId(3L))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true, false))
        whenever(getShareFolderSensitiveWarningTypeUseCase(nodeIds, true))
            .thenReturn(SensitiveNodeShareWarning.Folder)

        val result = underTest(nodeIds)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folder)
        verify(getShareFolderSensitiveWarningTypeUseCase)(nodeIds, true)
    }
}
