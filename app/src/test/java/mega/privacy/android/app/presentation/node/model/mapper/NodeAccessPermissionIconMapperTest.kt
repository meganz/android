package mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAccessPermissionIconMapperTest {

    @Test
    fun `test that the mapper returns the correct icon for each access permission`() {
        val mapper = NodeAccessPermissionIconMapper()
        assertThat(mapper(AccessPermission.READ)).isEqualTo(R.drawable.ic_shared_read)
        assertThat(mapper(AccessPermission.READWRITE))
            .isEqualTo(R.drawable.ic_shared_read_write)
        assertThat(mapper(AccessPermission.FULL)).isEqualTo(R.drawable.ic_shared_fullaccess)
        assertThat(mapper(AccessPermission.UNKNOWN)).isNull()
    }
}