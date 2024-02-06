package mega.privacy.android.app.presentation.node.view.toolbar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.mapper.NodeToolbarActionMapper
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Download
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolbarViewModelTest {

    private val nodeToolbarActionMapper = NodeToolbarActionMapper()
    private val cloudDriveToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>> =
        setOf(Download(DownloadMenuAction()))
    private val incomingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>> =
        setOf(Download(DownloadMenuAction()))
    private val outgoingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>> =
        setOf(Download(DownloadMenuAction()))
    private val linksToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>> =
        setOf(Download(DownloadMenuAction()))
    private val rubbishBinToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>> =
        setOf(Download(DownloadMenuAction()))
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val getRubbishBinNodeUseCase: GetRubbishNodeUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()

    private val generalNode = mock<TypedNode> {
        whenever(it.id).thenReturn(NodeId(GENERAL_NODE_ID))
    }
    private val backUpNode = mock<TypedNode> {
        whenever(it.id).thenReturn(NodeId(BACKUP_NODE_HANDLE))
    }
    private val rubbishNode = mock<TypedNode> {
        whenever(it.id).thenReturn(NodeId(RUBBISH_NODE_HANDLE))
    }
    private val accessNode = mock<TypedNode> {
        whenever(it.id).thenReturn(NodeId(HAS_ACCESS_HANDLE))
    }

    private val selectedNodes = setOf(generalNode, backUpNode, rubbishNode, accessNode)

    private lateinit var underTest: ToolbarViewModel

    @BeforeAll
    fun setDispatchers() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = ToolbarViewModel(
            cloudDriveToolbarOptions = cloudDriveToolbarOptions,
            incomingSharesToolbarOptions = incomingSharesToolbarOptions,
            outgoingSharesToolbarOptions = outgoingSharesToolbarOptions,
            linksToolbarOptions = linksToolbarOptions,
            rubbishBinToolbarOptions = rubbishBinToolbarOptions,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
            getNodeAccessPermission = getNodeAccessPermission,
            getRubbishNodeUseCase = getRubbishBinNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            nodeToolbarActionMapper = nodeToolbarActionMapper
        )
    }

    @ParameterizedTest(name = "screen type {0}")
    @MethodSource("provideParams")
    fun `test that updateToolbarState executes all required interactions`(
        nodeSourceType: NodeSourceType,
    ) = runTest {

        whenever(isNodeInBackupsUseCase(BACKUP_NODE_HANDLE)).thenReturn(true)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(
            checkNodeCanBeMovedToTargetNode(
                NodeId(RUBBISH_NODE_HANDLE),
                NodeId(ANY_NODE_HANDLE)
            )
        ).thenReturn(
            true
        )
        whenever(getNodeAccessPermission(NodeId(HAS_ACCESS_HANDLE))).thenReturn(AccessPermission.FULL)
        whenever(getNodeAccessPermission(NodeId(GENERAL_NODE_ID))).thenReturn(AccessPermission.READ)
        whenever(getNodeAccessPermission(NodeId(BACKUP_NODE_HANDLE))).thenReturn(AccessPermission.READ)
        whenever(getNodeAccessPermission(NodeId(RUBBISH_NODE_HANDLE))).thenReturn(AccessPermission.READ)

        underTest.updateToolbarState(
            selectedNodes = selectedNodes,
            resultCount = RESULT_COUNT,
            nodeSourceType = nodeSourceType
        )
        if (nodeSourceType == NodeSourceType.RUBBISH_BIN) {
            verifyNoInteractions(
                checkNodeCanBeMovedToTargetNode,
                isNodeInBackupsUseCase,
                getNodeAccessPermission,
            )
        } else if (nodeSourceType == NodeSourceType.INCOMING_SHARES) {
            verify(isNodeInBackupsUseCase, Times(selectedNodes.size)).invoke(any())
        } else {
            verify(isNodeInBackupsUseCase, Times(selectedNodes.size)).invoke(any())
            verifyNoInteractions(getNodeAccessPermission)
        }
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            NodeSourceType.CLOUD_DRIVE
        ),
        Arguments.of(
            NodeSourceType.LINKS
        ),
        Arguments.of(
            NodeSourceType.INCOMING_SHARES
        ),
        Arguments.of(
            NodeSourceType.OUTGOING_SHARES
        ),
        Arguments.of(
            NodeSourceType.RUBBISH_BIN
        )
    )

    @AfterEach
    fun resetMocks() {
        reset(
            checkNodeCanBeMovedToTargetNode,
            getNodeAccessPermission,
            getRubbishBinNodeUseCase,
            isNodeInBackupsUseCase,
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }

    private suspend fun stubCommon() {
        whenever(getRubbishBinNodeUseCase()).thenReturn(null)
    }

    companion object {
        const val HAS_ACCESS_HANDLE = 12L
        const val GENERAL_NODE_ID = 1234L
        const val RUBBISH_NODE_HANDLE = 34L
        const val BACKUP_NODE_HANDLE = 23L
        const val ANY_NODE_HANDLE = 235L
        const val RESULT_COUNT = 30
    }
}