package mega.privacy.android.app.domain.usecase.offline

import android.app.Activity
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSetNodeAvailableOfflineTest {

    private lateinit var underTest: SetNodeAvailableOffline

    private val megaNodeRepository = mock<MegaNodeRepository>()
    private val getNodeUseCase = mock<GetNodeUseCase>()
    private val activity = mock<Activity>()

    private val id = NodeId(1L)
    private val node = mock<MegaNode> {
        on { handle }.thenReturn(id.longValue)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = DefaultSetNodeAvailableOffline(
            megaNodeRepository = megaNodeRepository,
            getNodeUseCase = getNodeUseCase,
        )
        whenever(
            getNodeUseCase.setNodeAvailableOffline(
                node = any(),
                setOffline = any(),
                isFromIncomingShares = any(),
                isFromInbox = any(),
                activity = any(),
            )
        ).thenReturn(
            Completable.complete()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test when node is not found getNodeUseCase has no interaction`() = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(null)
        underTest(id, true, WeakReference(activity))
        verifyNoInteractions(getNodeUseCase)
    }

    @Test
    fun `test when node is in inbox getNodeUseCase is called with isFromIncomingShares set to true`() =
        testIsFromIncomingShares(true)

    @Test
    fun `test when node is not in inbox getNodeUseCase is called with isFromIncomingShares set to false`() =
        testIsFromIncomingShares(false)

    @Test
    fun `test when node is from incoming getNodeUseCase is called with isFromIncomingShares set to true`() =
        testIsFromInbox(true)

    @Test
    fun `test when node is not from incoming getNodeUseCase is called with isFromIncomingShares set to false`() =
        testIsFromInbox(false)

    private fun testIsFromIncomingShares(isFromIncomingShares: Boolean) = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(node)
        whenever(megaNodeRepository.isNodeInInbox(node)).thenReturn(false)
        whenever(megaNodeRepository.getUserFromInShare(node, true)).thenReturn(
            if (isFromIncomingShares) mock() else null
        )
        underTest(id, true, WeakReference(activity))
        verify(getNodeUseCase, times(1)).setNodeAvailableOffline(
            node = node,
            setOffline = true,
            isFromIncomingShares = isFromIncomingShares,
            isFromInbox = false,
            activity = activity
        )
    }

    private fun testIsFromInbox(isFromInbox: Boolean) = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(node)
        whenever(megaNodeRepository.isNodeInInbox(node)).thenReturn(isFromInbox)
        whenever(megaNodeRepository.getUserFromInShare(node, true)).thenReturn(null)
        underTest(id, true, WeakReference(activity))
        verify(getNodeUseCase, times(1)).setNodeAvailableOffline(
            node = node,
            setOffline = true,
            isFromIncomingShares = false,
            isFromInbox = isFromInbox,
            activity = activity
        )
    }
}