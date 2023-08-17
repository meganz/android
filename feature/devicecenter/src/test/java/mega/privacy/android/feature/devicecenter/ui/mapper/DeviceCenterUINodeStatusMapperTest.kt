package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

/**
 * Test class for [DeviceCenterUINodeStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterUINodeStatusMapperTest {
    private lateinit var underTest: DeviceCenterUINodeStatusMapper

    @BeforeAll
    fun setUp() {
        underTest = DeviceCenterUINodeStatusMapper()
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        deviceCenterNodeStatus: DeviceCenterNodeStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(underTest(deviceCenterNodeStatus)).isEqualTo(expectedNodeUiStatus)
    }

    @Test
    fun `test that a non matching device status returns an unknown ui device status`() {
        assertThat(underTest(DeviceCenterNodeStatus.Unknown)).isEqualTo(DeviceCenterUINodeStatus.Unknown)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(DeviceCenterNodeStatus.Stopped, DeviceCenterUINodeStatus.Stopped),
        Arguments.of(DeviceCenterNodeStatus.Disabled, DeviceCenterUINodeStatus.Disabled),
        Arguments.of(DeviceCenterNodeStatus.Offline, DeviceCenterUINodeStatus.Offline),
        Arguments.of(DeviceCenterNodeStatus.UpToDate, DeviceCenterUINodeStatus.UpToDate),
        Arguments.of(DeviceCenterNodeStatus.Blocked(mock()), DeviceCenterUINodeStatus.Blocked),
        Arguments.of(DeviceCenterNodeStatus.Overquota, DeviceCenterUINodeStatus.Overquota),
        Arguments.of(DeviceCenterNodeStatus.Paused, DeviceCenterUINodeStatus.Paused),
        Arguments.of(DeviceCenterNodeStatus.Initializing, DeviceCenterUINodeStatus.Initializing),
        Arguments.of(
            DeviceCenterNodeStatus.Syncing(50),
            DeviceCenterUINodeStatus.SyncingWithPercentage(50),
        ),
        Arguments.of(
            DeviceCenterNodeStatus.Syncing(0),
            DeviceCenterUINodeStatus.Syncing,
        ),
        Arguments.of(
            DeviceCenterNodeStatus.NoCameraUploads,
            DeviceCenterUINodeStatus.NoCameraUploads,
        ),
    )
}