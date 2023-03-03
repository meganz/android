package test.mega.privacy.android.app.domain.usecase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsAvailableOffline
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class GetNodeLocationInfoTest {
    private lateinit var underTest: GetNodeLocationInfo
    private val megaNodeUtilWrapper = mock<MegaNodeUtilWrapper>()
    private val nodeRepository = mock<NodeRepository>()
    private val isAvailableOffline = mock<IsAvailableOffline>()

    private val node = mock<TypedNode> {
        on { id }.thenReturn(nodeId)
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setUp() {
        underTest = DefaultGetNodeLocationInfo(
            megaNodeUtilWrapper,
            nodeRepository,
            isAvailableOffline,
        )
    }

    @Test
    fun `test if node has owner util wrapper is called with inComingShare true`() = runTest {
        whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(mock())
        whenever(isAvailableOffline(node)).thenReturn(false)
        underTest(node)
        verify(megaNodeUtilWrapper).getNodeLocationInfo(any(), eq(true), eq(handle))
    }

    @Test
    fun `test if node has no owner util wrapper is called with inComingShare false`() = runTest {
        whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
        whenever(isAvailableOffline(node)).thenReturn(false)
        underTest(node)
        verify(megaNodeUtilWrapper).getNodeLocationInfo(any(), eq(false), eq(handle))
    }

    @Test
    fun `test if node is available offline util wrapper is called with proper value as adapter type`() =
        runTest {
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(isAvailableOffline(node)).thenReturn(true)
            underTest(node)
            verify(megaNodeUtilWrapper).getNodeLocationInfo(
                eq(Constants.OFFLINE_ADAPTER),
                any(),
                eq(handle)
            )
        }

    @Test
    fun `test if node is not available offline util wrapper is called with proper value as adapter type`() =
        runTest {
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(isAvailableOffline(node)).thenReturn(false)
            underTest(node)
            verify(megaNodeUtilWrapper).getNodeLocationInfo(
                AdditionalMatchers.not(eq(Constants.OFFLINE_ADAPTER)),
                any(),
                eq(handle)
            )
        }

    private companion object {
        const val handle = 1L
        val nodeId = NodeId(handle)
    }
}