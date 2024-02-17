package mega.privacy.android.app.presentation.node

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.DefaultGetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper = mock()
    private val getContactItemFromInShareFolder: DefaultGetContactItemFromInShareFolder = mock()
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase = mock()
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()

    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }

    private fun initViewModel() {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = NodeBottomSheetActionMapper(),
            bottomSheetOptions = setOf(),
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbish = isNodeInRubbish,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            nodeAccessPermissionIconMapper = nodeAccessPermissionIconMapper,
            getContactItemFromInShareFolder = getContactItemFromInShareFolder,
            getOutShareByNodeIdUseCase = getOutShareByNodeIdUseCase,
            getContactFromEmailUseCase = getContactFromEmailUseCase,
        )
    }

    @Test
    fun `test that get bottom sheet option invokes getNodeByIdUseCase`() = runTest {
        initViewModel()
        viewModel.getBottomSheetOptions(sampleNode.id.longValue)
        verify(getNodeByIdUseCase).invoke(sampleNode.id)
        verify(isNodeInRubbish).invoke(sampleNode.id.longValue)
        verify(isNodeInBackupsUseCase).invoke(sampleNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleNode.id)
    }
}