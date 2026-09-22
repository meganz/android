package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import mega.privacy.android.shared.resources.R as sharedR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionEditErrorMapperTest {

    private val underTest = SyncExclusionEditErrorMapper()

    @Test
    fun `test that invoke names the duplicate for a rule the file already holds`() {
        val actual = underTest(SyncExclusionEditError.DuplicateRule(pattern = "*.tmp"))

        assertThat(actual).isEqualTo(sharedR.string.sync_exclusions_add_name_duplicate_error)
    }

    @Test
    fun `test that invoke names the range for bounds that would hold the same value`() {
        val actual = underTest(SyncExclusionEditError.EqualSizeBounds(bytes = 1_024L))

        assertThat(actual).isEqualTo(sharedR.string.sync_exclusions_manage_snackbar_size_conflict)
    }

    @Test
    fun `test that invoke falls back to the general failure for a stale target`() {
        val actual = underTest(SyncExclusionEditError.StaleRuleTarget(ruleIndex = 3))

        assertThat(actual).isEqualTo(sharedR.string.general_text_error)
    }

    @Test
    fun `test that invoke falls back to the general failure for a target this app cannot author`() {
        val actual = underTest(
            SyncExclusionEditError.UnsupportedTarget(target = SyncExclusionTarget.Symlinks),
        )

        assertThat(actual).isEqualTo(sharedR.string.general_text_error)
    }

    @Test
    fun `test that invoke falls back to the general failure for an unusable bound`() {
        val actual = underTest(SyncExclusionEditError.UnusableSizeBound(bytes = 0L))

        assertThat(actual).isEqualTo(sharedR.string.general_text_error)
    }
}
