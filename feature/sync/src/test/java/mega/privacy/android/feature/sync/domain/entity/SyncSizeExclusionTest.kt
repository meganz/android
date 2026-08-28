package mega.privacy.android.feature.sync.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncSizeExclusionTest {

    @Test
    fun `test that isUnbounded is true when neither bound is set`() {
        assertThat(SyncSizeExclusion().isUnbounded).isTrue()
    }

    @Test
    fun `test that isUnbounded is false when only the smaller bound is set`() {
        assertThat(SyncSizeExclusion(excludeSmallerThanBytes = 100L).isUnbounded).isFalse()
    }

    @Test
    fun `test that isUnbounded is false when only the larger bound is set`() {
        assertThat(SyncSizeExclusion(excludeLargerThanBytes = 100L).isUnbounded).isFalse()
    }

    @Test
    fun `test that isUnbounded is false when both bounds are set`() {
        assertThat(
            SyncSizeExclusion(
                excludeSmallerThanBytes = 10L,
                excludeLargerThanBytes = 100L,
            ).isUnbounded
        ).isFalse()
    }
}
