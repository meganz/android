package mega.privacy.android.core.nodecomponents.action.eventhandler

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.mobile.analytics.event.CloudDriveCopyMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveMenuItemEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsActionEventSenderTest {

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }

    private lateinit var underTest: NodeOptionsActionEventSender
    private val nodeOptionsActionEventMapper = mock<NodeOptionsActionEventMapper>()

    @BeforeEach
    fun setUp() {
        underTest = NodeOptionsActionEventSender(nodeOptionsActionEventMapper)
    }

    @Test
    fun `test that invoke calls mapper and tracks event when event is returned`() {
        val action = mock<CopyMenuAction>()
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE
        val expectedEvent = CloudDriveCopyMenuItemEvent
        whenever(nodeOptionsActionEventMapper(action, nodeSourceType)).thenReturn(expectedEvent)

        underTest(action, nodeSourceType)

        verify(nodeOptionsActionEventMapper).invoke(action, nodeSourceType)
        assertThat(analyticsExtension.events).contains(expectedEvent)
    }

    @Test
    fun `test that invoke calls mapper and tracks event for MoveMenuAction`() {
        val action = mock<MoveMenuAction>()
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE
        val expectedEvent = CloudDriveMoveMenuItemEvent
        whenever(nodeOptionsActionEventMapper(action, nodeSourceType)).thenReturn(expectedEvent)

        underTest(action, nodeSourceType)

        verify(nodeOptionsActionEventMapper).invoke(action, nodeSourceType)
        assertThat(analyticsExtension.events).contains(expectedEvent)
    }

    @Test
    fun `test that invoke does not track event when mapper returns null`() {
        val action = mock<CopyMenuAction>()
        val nodeSourceType = NodeSourceType.HOME
        whenever(nodeOptionsActionEventMapper(action, nodeSourceType)).thenReturn(null)

        underTest(action, nodeSourceType)

        verify(nodeOptionsActionEventMapper).invoke(action, nodeSourceType)
        assertThat(analyticsExtension.events).isEmpty()
    }

    @Test
    fun `test that invoke does not track event when mapper returns null for null source type`() {
        val action = mock<CopyMenuAction>()
        whenever(nodeOptionsActionEventMapper(action, null)).thenReturn(null)

        underTest(action, null)

        verify(nodeOptionsActionEventMapper).invoke(action, null)
        assertThat(analyticsExtension.events).isEmpty()
    }

    @Test
    fun `test that multiple events are tracked correctly`() {
        val action1 = mock<CopyMenuAction>()
        val action2 = mock<MoveMenuAction>()
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE
        val event1 = CloudDriveCopyMenuItemEvent
        val event2 = CloudDriveMoveMenuItemEvent

        whenever(nodeOptionsActionEventMapper(action1, nodeSourceType)).thenReturn(event1)
        whenever(nodeOptionsActionEventMapper(action2, nodeSourceType)).thenReturn(event2)

        underTest(action1, nodeSourceType)
        underTest(action2, nodeSourceType)

        assertThat(analyticsExtension.events).containsExactly(event1, event2)
    }
}
