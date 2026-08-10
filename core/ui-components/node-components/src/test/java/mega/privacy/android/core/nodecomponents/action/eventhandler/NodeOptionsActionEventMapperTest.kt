package mega.privacy.android.core.nodecomponents.action.eventhandler

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.action.eventhandler.mapper.CloudDriveActionEventMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.mobile.analytics.event.CloudDriveCopyMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveMenuItemEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsActionEventMapperTest {

    private lateinit var underTest: NodeOptionsActionEventMapper
    private val cloudDriveActionEventMapper = mock<CloudDriveActionEventMapper>()

    @BeforeEach
    fun setUp() {
        underTest = NodeOptionsActionEventMapper(cloudDriveActionEventMapper)
    }

    @AfterEach
    fun resetMocks() {
        reset(cloudDriveActionEventMapper)
    }

    @Test
    fun `test that CLOUD_DRIVE source type delegates to CloudDriveActionEventMapper`() {
        val action = mock<CopyMenuAction>()
        val expectedEvent = CloudDriveCopyMenuItemEvent
        whenever(cloudDriveActionEventMapper(action)).thenReturn(expectedEvent)

        val result = underTest(action, NodeSourceType.CLOUD_DRIVE)

        assertThat(result).isEqualTo(expectedEvent)
        verify(cloudDriveActionEventMapper).invoke(action)
    }

    @Test
    fun `test that CLOUD_DRIVE source type with MoveMenuAction delegates correctly`() {
        val action = mock<MoveMenuAction>()
        val expectedEvent = CloudDriveMoveMenuItemEvent
        whenever(cloudDriveActionEventMapper(action)).thenReturn(expectedEvent)

        val result = underTest(action, NodeSourceType.CLOUD_DRIVE)

        assertThat(result).isEqualTo(expectedEvent)
        verify(cloudDriveActionEventMapper).invoke(action)
    }

    @Test
    fun `test that HOME source type returns null`() {
        val action = mock<CopyMenuAction>()
        val result = underTest(action, NodeSourceType.HOME)
        assertThat(result).isNull()
    }

    @Test
    fun `test that null source type returns null`() {
        val action = mock<CopyMenuAction>()
        val result = underTest(action, null)
        assertThat(result).isNull()
    }

    @Test
    fun `test that CLOUD_DRIVE source type returns null when CloudDriveActionEventMapper returns null`() {
        val action = mock<CopyMenuAction>()
        whenever(cloudDriveActionEventMapper(action)).thenReturn(null)

        val result = underTest(action, NodeSourceType.CLOUD_DRIVE)

        assertThat(result).isNull()
        verify(cloudDriveActionEventMapper).invoke(action)
    }
}
