package mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAccessPermissionIconMapperTest {

    @Test
    fun `test that the mapper returns the correct icon for each access permission`() {
        val mapper = NodeAccessPermissionIconMapper()
        assertThat(mapper(AccessPermission.READ)).isEqualTo(iconPackR.drawable.ic_eye_medium_thin_outline)
        assertThat(mapper(AccessPermission.READWRITE))
            .isEqualTo(iconPackR.drawable.ic_edit_medium_thin_outline)
        assertThat(mapper(AccessPermission.FULL)).isEqualTo(iconPackR.drawable.ic_star_medium_thin_outline)
        assertThat(mapper(AccessPermission.UNKNOWN)).isNull()
    }
}