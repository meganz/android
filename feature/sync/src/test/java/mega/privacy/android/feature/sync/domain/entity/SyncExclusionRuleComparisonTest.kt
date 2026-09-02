package mega.privacy.android.feature.sync.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionRuleComparisonTest {

    @Test
    fun `test that a rule equals itself ignoring enabled`() {
        assertThat(reference.equalsIgnoringEnabled(reference)).isTrue()
    }

    @Test
    fun `test that a disabled rule equals its enabled twin ignoring enabled`() {
        assertThat(reference.equalsIgnoringEnabled(reference.copy(enabled = false))).isTrue()
    }

    @Test
    fun `test that an enabled rule equals its disabled twin ignoring enabled`() {
        assertThat(reference.copy(enabled = false).equalsIgnoringEnabled(reference)).isTrue()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRulesDifferingInOneField")
    fun `test that a rule differing in any other field is not equal ignoring enabled`(
        field: String,
        other: SyncExclusionRule,
    ) {
        assertThat(reference.equalsIgnoringEnabled(other)).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRulesDifferingInOneField")
    fun `test that a rule differing in any other field is not equal ignoring enabled either way round`(
        field: String,
        other: SyncExclusionRule,
    ) {
        assertThat(other.equalsIgnoringEnabled(reference)).isFalse()
    }

    private fun provideRulesDifferingInOneField(): Stream<Arguments> = Stream.of(
        Arguments.of("pattern", reference.copy(pattern = "Other*")),
        Arguments.of("exclusionClass", reference.copy(exclusionClass = SyncExclusionClass.Include)),
        Arguments.of("target", reference.copy(target = SyncExclusionTarget.Directories)),
        Arguments.of("matchType", reference.copy(matchType = SyncExclusionMatchType.LocalName)),
        Arguments.of("strategy", reference.copy(strategy = SyncMatchStrategy.GlobCaseSensitive)),
    )

    private val reference = SyncExclusionRule(
        pattern = "Reference*",
        exclusionClass = SyncExclusionClass.Exclude,
        target = SyncExclusionTarget.All,
        matchType = SyncExclusionMatchType.SubtreeName,
        strategy = SyncMatchStrategy.GlobCaseInsensitive,
        enabled = true,
    )
}
