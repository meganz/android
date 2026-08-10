package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeSourceType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveContentTest {

    @ParameterizedTest(name = "when source type is {0}")
    @ValueSource(strings = ["INCOMING_SHARES", "OUTGOING_SHARES", "LINKS"])
    fun `test that sortOptionsSourceType maps shared sources to CLOUD_DRIVE`(name: String) {
        val sourceType = NodeSourceType.valueOf(name)

        assertThat(sourceType.sortOptionsSourceType()).isEqualTo(NodeSourceType.CLOUD_DRIVE)
    }

    @ParameterizedTest(name = "when source type is {0}")
    @EnumSource(
        value = NodeSourceType::class,
        names = ["INCOMING_SHARES", "OUTGOING_SHARES", "LINKS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that sortOptionsSourceType leaves non-shared sources unchanged`(
        sourceType: NodeSourceType,
    ) {
        assertThat(sourceType.sortOptionsSourceType()).isEqualTo(sourceType)
    }
}
