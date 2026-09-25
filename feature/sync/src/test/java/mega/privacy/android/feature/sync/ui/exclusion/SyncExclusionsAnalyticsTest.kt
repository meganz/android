package mega.privacy.android.feature.sync.ui.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsBiggerThanSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsBiggerThanSizeLimitEditedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsNotBetweenSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsNotBetweenSizeLimitEditedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsSmallerThanSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsSmallerThanSizeLimitEditedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionsAnalyticsTest {

    @Test
    fun `test that toChangesSavedEvent counts every rule the model holds`() {
        val actual = withCustomRuleAndBound.toChangesSavedEvent()

        assertThat(actual.info).isEqualTo(
            mapOf("ruleCount" to 9, "customRuleCount" to 2, "hasSizeLimit" to true),
        )
    }

    @Test
    fun `test that toSetupConfiguredEvent counts every rule the model holds`() {
        val actual = withCustomRuleAndBound.toSetupConfiguredEvent()

        assertThat(actual.info).isEqualTo(
            mapOf("ruleCount" to 9, "customRuleCount" to 2, "hasSizeLimit" to true),
        )
    }

    @Test
    fun `test that toChangesSavedEvent counts a disabled default as a default`() {
        val model = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES.first().copy(enabled = false)),
            ),
        )

        val actual = model.toChangesSavedEvent()

        assertThat(actual.info["ruleCount"]).isEqualTo(1)
        assertThat(actual.info["customRuleCount"]).isEqualTo(0)
    }

    @Test
    fun `test that toChangesSavedEvent reports no size limit when the model declares no bound`() {
        val actual = ParsedMegaIgnore(entries = defaultEntries).toChangesSavedEvent()

        assertThat(actual.info["hasSizeLimit"]).isEqualTo(false)
        assertThat(actual.info["customRuleCount"]).isEqualTo(0)
    }

    @Test
    fun `test that toSetupConfiguredEvent reports no size limit when the model declares no bound`() {
        val actual = ParsedMegaIgnore(entries = defaultEntries).toSetupConfiguredEvent()

        assertThat(actual.info["hasSizeLimit"]).isEqualTo(false)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("addedEvents")
    fun `test that toSizeLimitAddedEvent classifies the bounds the way the form authors them`(
        bounds: SyncSizeExclusion,
        expected: Any,
    ) {
        assertThat(bounds.toSizeLimitAddedEvent()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("editedEvents")
    fun `test that toSizeLimitEditedEvent classifies the bounds the way the form authors them`(
        bounds: SyncSizeExclusion,
        expected: Any,
    ) {
        assertThat(bounds.toSizeLimitEditedEvent()).isEqualTo(expected)
    }

    @Test
    fun `test that toSizeLimitAddedEvent returns null when no bound is declared`() {
        assertThat(SyncSizeExclusion().toSizeLimitAddedEvent()).isNull()
    }

    @Test
    fun `test that toSizeLimitEditedEvent returns null when no bound is declared`() {
        assertThat(SyncSizeExclusion().toSizeLimitEditedEvent()).isNull()
    }

    private fun addedEvents(): Stream<Arguments> = Stream.of(
        Arguments.of(larger, AndroidSyncExclusionsBiggerThanSizeLimitAddedEvent),
        Arguments.of(smaller, AndroidSyncExclusionsSmallerThanSizeLimitAddedEvent),
        Arguments.of(range, AndroidSyncExclusionsNotBetweenSizeLimitAddedEvent),
        Arguments.of(invertedRange, AndroidSyncExclusionsNotBetweenSizeLimitAddedEvent),
    )

    private fun editedEvents(): Stream<Arguments> = Stream.of(
        Arguments.of(larger, AndroidSyncExclusionsBiggerThanSizeLimitEditedEvent),
        Arguments.of(smaller, AndroidSyncExclusionsSmallerThanSizeLimitEditedEvent),
        Arguments.of(range, AndroidSyncExclusionsNotBetweenSizeLimitEditedEvent),
        Arguments.of(invertedRange, AndroidSyncExclusionsNotBetweenSizeLimitEditedEvent),
    )

    private val larger = SyncSizeExclusion(excludeLargerThanBytes = 2048)
    private val smaller = SyncSizeExclusion(excludeSmallerThanBytes = 1024)
    private val range = SyncSizeExclusion(excludeSmallerThanBytes = 1024, excludeLargerThanBytes = 2048)
    private val invertedRange =
        SyncSizeExclusion(excludeSmallerThanBytes = 2048, excludeLargerThanBytes = 1024)

    private val defaultEntries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) }

    /**
     * A narrowed default no longer compares equal to one, so it counts as the user's own, while a
     * disabled one is still a default. A preserved line is not a rule at all.
     */
    private val withCustomRuleAndBound = ParsedMegaIgnore(
        entries = defaultEntries.drop(2) +
                MegaIgnoreEntry.RuleEntry(
                    MegaIgnoreDefaults.RULES[0].copy(target = SyncExclusionTarget.Files),
                ) +
                MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES[1].copy(enabled = false)) +
                MegaIgnoreEntry.RuleEntry(
                    SyncExclusionRule(
                        pattern = "*.iso",
                        matchType = SyncExclusionMatchType.SubtreeName,
                        enabled = false,
                    ),
                ) +
                MegaIgnoreEntry.PreservedEntry("-r:^backup_.*") +
                MegaIgnoreEntry.SizeLargerEntry(bytes = 1_048_576L),
    )
}
