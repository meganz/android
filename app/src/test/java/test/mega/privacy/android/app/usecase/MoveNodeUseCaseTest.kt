package test.mega.privacy.android.app.usecase

import android.content.Context
import com.google.common.truth.Truth
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.CopyNodeListByHandleUseCase
import mega.privacy.android.app.usecase.CopyNodeListUseCase
import mega.privacy.android.app.usecase.LegacyMoveNodeUseCase
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveNodeUseCaseTest {

    private lateinit var underTest: LegacyMoveNodeUseCase

    private lateinit var megaApiGateway: MegaApiGateway
    private lateinit var megaApiFolderGateway: MegaApiFolderGateway
    private lateinit var getChatMessageUseCase: GetChatMessageUseCase
    private lateinit var copyNodeListUseCase: CopyNodeListUseCase
    private lateinit var copyNodeListByHandleUseCase: CopyNodeListByHandleUseCase
    private lateinit var getNodeByHandle: GetNodeByHandle
    private lateinit var moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle
    private lateinit var accountRepository: AccountRepository

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @BeforeEach
    fun resetMocks() {
        megaApiGateway = mock()
        megaApiFolderGateway = mock()
        getChatMessageUseCase = mock()
        copyNodeListUseCase = mock()
        copyNodeListByHandleUseCase = mock()
        getNodeByHandle = mock()
        moveNodeToRubbishByHandle = mock()
        accountRepository = mock()

        underTest = LegacyMoveNodeUseCase(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            moveNodeToRubbishByHandle = moveNodeToRubbishByHandle,
            accountRepository = accountRepository
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
        RxAndroidPlugins.reset()
    }

    @Test
    internal fun `test that move node use case returns success when valid input is given`() =
        runTest {
            val copyNode = mock<MegaNode>()
            val parentNode = mock<MegaNode>()
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("Success")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val mockNameCollision = mock<NameCollision.Movement> {
                on { nodeHandle }.thenReturn(123456)
                on { parentHandle }.thenReturn(1234567)
            }
            val nameCollisionResult = mock<NameCollisionResult> {
                on { nameCollision }.thenReturn(mockNameCollision)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(copyNode)
            whenever(megaApiGateway.getMegaNodeByHandle(1234567)).thenReturn(parentNode)
            val response = underTest.moveAsync(nameCollisionResult, false)
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response.errorCount).isEqualTo(0)
        }

    @Test
    internal fun `test that move node use case throws foreign node exception when api returns over quota and parent node is foreign node`() =
        runTest {
            val copyNode = mock<MegaNode>()
            val parentNode = mock<MegaNode>()
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EOVERQUOTA)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val mockNameCollision = mock<NameCollision.Movement> {
                on { nodeHandle }.thenReturn(123456)
                on { parentHandle }.thenReturn(1234567)
            }
            val nameCollisionResult = mock<NameCollisionResult> {
                on { nameCollision }.thenReturn(mockNameCollision)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(copyNode)
            whenever(megaApiGateway.getMegaNodeByHandle(1234567)).thenReturn(parentNode)
            whenever(megaApiGateway.isForeignNode(parentNode.handle)).thenReturn(true)
            assertFailsWith<ForeignNodeException> {
                underTest.moveAsync(nameCollisionResult, false)
            }
        }

    @Test
    internal fun `test that move node use case throws over quota exception when api returns over quota and parent node is not foreign node`() =
        runTest {
            val copyNode = mock<MegaNode>()
            val parentNode = mock<MegaNode>()
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EOVERQUOTA)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val mockNameCollision = mock<NameCollision.Movement> {
                on { nodeHandle }.thenReturn(123456)
                on { parentHandle }.thenReturn(1234567)
            }
            val nameCollisionResult = mock<NameCollisionResult> {
                on { nameCollision }.thenReturn(mockNameCollision)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(copyNode)
            whenever(megaApiGateway.getMegaNodeByHandle(1234567)).thenReturn(parentNode)
            whenever(megaApiGateway.isForeignNode(parentNode.handle)).thenReturn(false)
            assertFailsWith<QuotaExceededMegaException> {
                underTest.moveAsync(nameCollisionResult, false)
            }
        }

    @Test
    internal fun `test that move node use case throws not enough quota exception when api returns not enough quota error code`() =
        runTest {
            val copyNode = mock<MegaNode>()
            val parentNode = mock<MegaNode>()
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EGOINGOVERQUOTA)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val mockNameCollision = mock<NameCollision.Movement> {
                on { nodeHandle }.thenReturn(123456)
                on { parentHandle }.thenReturn(1234567)
            }
            val nameCollisionResult = mock<NameCollisionResult> {
                on { nameCollision }.thenReturn(mockNameCollision)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(copyNode)
            whenever(megaApiGateway.getMegaNodeByHandle(1234567)).thenReturn(parentNode)
            assertFailsWith<NotEnoughQuotaMegaException> {
                underTest.moveAsync(nameCollisionResult, false)
            }
        }

    @Test
    internal fun `test that move node use case fails whenever an unhandled exception comes`() =
        runTest {
            val copyNode = mock<MegaNode>()
            val parentNode = mock<MegaNode>()
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EINTERNAL)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val mockNameCollision = mock<NameCollision.Movement> {
                on { nodeHandle }.thenReturn(123456)
                on { parentHandle }.thenReturn(1234567)
            }
            val nameCollisionResult = mock<NameCollisionResult> {
                on { nameCollision }.thenReturn(mockNameCollision)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(copyNode)
            whenever(megaApiGateway.getMegaNodeByHandle(1234567)).thenReturn(parentNode)
            val response = underTest.moveAsync(nameCollisionResult, false)
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response.errorCount).isEqualTo(1)
        }


    @Test
    internal fun `test that error count is zero when all nodes are moved to rubbish bin`() =
        runTest {
            val deleteHandles = listOf<Long>(123456, 234567, 345678, 456789)
            val context = mock<Context>()
            val rubbishBinNode = mock<MegaNode> {
                on { handle }.thenReturn(123456)
            }
            val node1 = mock<MegaNode> { on { handle }.thenReturn(123456) }
            val node2 = mock<MegaNode> { on { handle }.thenReturn(234567) }
            val node3 = mock<MegaNode> { on { handle }.thenReturn(345678) }
            val node4 = mock<MegaNode> { on { handle }.thenReturn(456789) }
            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(rubbishBinNode)
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(node1)
            whenever(megaApiGateway.getMegaNodeByHandle(234567)).thenReturn(node2)
            whenever(megaApiGateway.getMegaNodeByHandle(345678)).thenReturn(node3)
            whenever(megaApiGateway.getMegaNodeByHandle(456789)).thenReturn(node4)
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("Success")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val response = underTest.moveToRubbishBin(deleteHandles, context).blockingGetOrNull()
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response?.errorCount).isEqualTo(0)
        }

    @Test
    internal fun `test that error count is one when one node is null`() =
        runTest {
            val deleteHandles = listOf<Long>(123456, 234567, 345678, 456789)
            val context = mock<Context>()
            val rubbishBinNode = mock<MegaNode> {
                on { handle }.thenReturn(123456)
            }
            val node1 = mock<MegaNode> { on { handle }.thenReturn(123456) }
            val node2 = mock<MegaNode> { on { handle }.thenReturn(234567) }
            val node3 = mock<MegaNode> { on { handle }.thenReturn(345678) }
            val node4 = null
            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(rubbishBinNode)
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(node1)
            whenever(megaApiGateway.getMegaNodeByHandle(234567)).thenReturn(node2)
            whenever(megaApiGateway.getMegaNodeByHandle(345678)).thenReturn(node3)
            whenever(megaApiGateway.getMegaNodeByHandle(456789)).thenReturn(node4)
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("Success")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val response = underTest.moveToRubbishBin(deleteHandles, context).blockingGetOrNull()
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response?.errorCount).isEqualTo(1)
        }

    @Test
    internal fun `test that error count is one when some exceptions are thrown while moving to rubbish bin`() =
        runTest {
            val deleteHandles = listOf<Long>(123456)
            val context = mock<Context>()
            val rubbishBinNode = mock<MegaNode> {
                on { handle }.thenReturn(123456)
            }
            val node1 = mock<MegaNode> { on { handle }.thenReturn(123456) }

            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(rubbishBinNode)
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(node1)
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EINTERNAL)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val response = underTest.moveToRubbishBin(deleteHandles, context).blockingGetOrNull()
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response?.errorCount).isEqualTo(1)
        }

    @Test
    internal fun `test that error count is zero when all nodes are restored rubbish bin`() =
        runTest {
            val node1 = mock<MegaNode> { on { restoreHandle }.thenReturn(123456) }
            val node2 = mock<MegaNode> { on { restoreHandle }.thenReturn(234567) }
            val node3 = mock<MegaNode> { on { restoreHandle }.thenReturn(345678) }
            val node4 = mock<MegaNode> { on { restoreHandle }.thenReturn(456789) }
            val parentNode1 = mock<MegaNode> { on { handle }.thenReturn(123456) }
            val parentNode2 = mock<MegaNode> { on { handle }.thenReturn(234567) }
            val parentNode3 = mock<MegaNode> { on { handle }.thenReturn(345678) }
            val parentNode4 = mock<MegaNode> { on { handle }.thenReturn(456789) }
            val deleteNodes = listOf(node1, node2, node3, node4)
            val context = mock<Context>()
            whenever(megaApiGateway.getMegaNodeByHandle(node1.restoreHandle)).thenReturn(parentNode1)
            whenever(megaApiGateway.getMegaNodeByHandle(node2.restoreHandle)).thenReturn(parentNode2)
            whenever(megaApiGateway.getMegaNodeByHandle(node3.restoreHandle)).thenReturn(parentNode3)
            whenever(megaApiGateway.getMegaNodeByHandle(node4.restoreHandle)).thenReturn(parentNode4)
            whenever(megaApiGateway.isInRubbish(any())).thenReturn(false)
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
                on { errorString }.thenReturn("Success")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val response = underTest.restore(deleteNodes, context).blockingGetOrNull()
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response?.errorCount).isEqualTo(0)
        }

    @Test
    internal fun `test that error count is one when some exceptions are thrown while restoring rubbish bin`() =
        runTest {
            val node1 = mock<MegaNode> { on { handle }.thenReturn(123456) }
            val deleteNode = listOf(node1)
            val context = mock<Context>()

            whenever(megaApiGateway.isInRubbish(any())).thenReturn(false)
            whenever(megaApiGateway.getMegaNodeByHandle(123456)).thenReturn(node1)
            val success = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EINTERNAL)
                on { errorString }.thenReturn("OverQuota")
            }
            whenever(
                megaApiGateway.moveNode(
                    nodeToMove = any(),
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            val response = underTest.restore(deleteNode, context).blockingGetOrNull()
            Truth.assertThat(response).isNotNull()
            Truth.assertThat(response?.errorCount).isEqualTo(1)
        }
}