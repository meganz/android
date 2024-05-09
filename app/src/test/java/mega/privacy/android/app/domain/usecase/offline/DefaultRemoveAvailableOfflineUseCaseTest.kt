package mega.privacy.android.app.domain.usecase.offline

import android.app.Activity
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.lang.ref.WeakReference

class DefaultRemoveAvailableOfflineUseCaseTest {

    private lateinit var underTest: RemoveAvailableOfflineUseCase

    private val megaNodeRepository = mock<MegaNodeRepository>()
    private val getNodeUseCase = mock<GetNodeUseCase>()
    private val activity = mock<Activity>()

    private val id = NodeId(1L)
    private val node = mock<MegaNode> {
        on { handle }.thenReturn(id.longValue)
    }

    @Before
    fun setup() {
        underTest = DefaultRemoveAvailableOfflineUseCase(
            megaNodeRepository = megaNodeRepository,
            getNodeUseCase = getNodeUseCase,
        )
        whenever(
            getNodeUseCase.removeNodeAvailableOffline(
                node = any(),
                activity = any(),
            )
        ).thenReturn(
            Completable.complete()
        )
    }

    @Test
    fun `test when node is not found getNodeUseCase has no interaction`() = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(null)
        underTest(id, WeakReference(activity))
        verifyNoInteractions(getNodeUseCase)
    }

    @Test
    fun `test when node is in backups getNodeUseCase is called with isFromIncomingShares set to true`() =
        testIsFromIncomingShares(true)

    @Test
    fun `test when node is not in backups getNodeUseCase is called with isFromIncomingShares set to false`() =
        testIsFromIncomingShares(false)

    @Test
    fun `test when node is from incoming getNodeUseCase is called with isFromIncomingShares set to true`() =
        testIsFromBackups(true)

    @Test
    fun `test when node is not from incoming getNodeUseCase is called with isFromIncomingShares set to false`() =
        testIsFromBackups(false)

    private fun testIsFromIncomingShares(isFromIncomingShares: Boolean) = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(node)
        whenever(megaNodeRepository.isNodeInBackups(node)).thenReturn(false)
        whenever(megaNodeRepository.getUserFromInShare(node, true)).thenReturn(
            if (isFromIncomingShares) mock() else null
        )
        underTest(id, WeakReference(activity))
        verify(getNodeUseCase, times(1)).removeNodeAvailableOffline(
            node = node,
            activity = activity
        )
    }

    private fun testIsFromBackups(isFromBackups: Boolean) = runTest {
        whenever(megaNodeRepository.getNodeByHandle(id.longValue)).thenReturn(node)
        whenever(megaNodeRepository.isNodeInBackups(node)).thenReturn(isFromBackups)
        whenever(megaNodeRepository.getUserFromInShare(node, true)).thenReturn(null)
        underTest(id, WeakReference(activity))
        verify(getNodeUseCase, times(1)).removeNodeAvailableOffline(
            node = node,
            activity = activity
        )
    }
}