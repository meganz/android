package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestoreDefaultSyncExclusionsUseCaseTest {

    private val underTest = RestoreDefaultSyncExclusionsUseCase()

    @Test
    fun `test that invoke returns exactly the default rules in order`() {
        val result = underTest(
            ParsedMegaIgnore(
                entries = listOf(
                    MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                    MegaIgnoreEntry.RuleEntry(excludeRule("*.iso")),
                ),
            )
        )

        assertThat(result.rules).containsExactlyElementsIn(MegaIgnoreDefaults.RULES).inOrder()
    }

    @Test
    fun `test that invoke puts the defaults first and keeps every other line after them in order`() {
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                MegaIgnoreEntry.PreservedEntry(REGEX_INCLUDE),
                MegaIgnoreEntry.RuleEntry(includeRule("*.tmp")),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 4096L),
                MegaIgnoreEntry.RuleEntry(excludeRule("*.iso")),
                MegaIgnoreEntry.InvalidEntry("Thumbs.db", InvalidLineReason.MissingExclusionClass),
            ),
        )

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(
            defaultEntries() + listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.PreservedEntry(REGEX_INCLUDE),
                MegaIgnoreEntry.RuleEntry(includeRule("*.tmp")),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 4096L),
                MegaIgnoreEntry.InvalidEntry("Thumbs.db", InvalidLineReason.MissingExclusionClass),
            )
        ).inOrder()
    }

    @Test
    fun `test that invoke keeps an include the user added to override a default`() {
        val include = MegaIgnoreEntry.RuleEntry(includeRule("*.tmp"))
        val current = ParsedMegaIgnore(entries = defaultEntries() + include)

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(defaultEntries() + include).inOrder()
        assertThat(result.entries.last()).isEqualTo(include)
    }

    @Test
    fun `test that invoke keeps the size bounds and their position`() {
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.SizeSmallerEntry(bytes = 1024L),
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 4096L),
            ),
        )

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(
            defaultEntries() + listOf(
                MegaIgnoreEntry.SizeSmallerEntry(bytes = 1024L),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 4096L),
            )
        ).inOrder()
        assertThat(result.size).isEqualTo(
            SyncSizeExclusion(excludeSmallerThanBytes = 1024L, excludeLargerThanBytes = 4096L)
        )
    }

    @Test
    fun `test that invoke keeps a regex include as a preserved line`() {
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry(REGEX_INCLUDE),
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
            ),
        )

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(
            defaultEntries() + MegaIgnoreEntry.PreservedEntry(REGEX_INCLUDE)
        ).inOrder()
    }

    @Test
    fun `test that invoke keeps an invalid line and still reports the file as rejected by the sdk`() {
        val invalid = MegaIgnoreEntry.InvalidEntry(
            line = "Thumbs.db",
            reason = InvalidLineReason.MissingExclusionClass,
        )
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                invalid,
            ),
        )

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(defaultEntries() + invalid).inOrder()
        assertThat(result.isRejectedBySdk).isTrue()
    }

    @Test
    fun `test that invoke keeps a synced megaignore directive the file already held`() {
        val directive = MegaIgnoreEntry.PreservedEntry("+sync:.megaignore")
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                directive,
            ),
        )

        val result = underTest(current)

        assertThat(result.entries).containsExactlyElementsIn(defaultEntries() + directive).inOrder()
    }

    @Test
    fun `test that invoke returns only the defaults when the current file is empty`() {
        val result = underTest(ParsedMegaIgnore(entries = emptyList()))

        assertThat(result.entries).containsExactlyElementsIn(defaultEntries()).inOrder()
    }

    @Test
    fun `test that invoke changes nothing when called a second time`() {
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**")),
                MegaIgnoreEntry.RuleEntry(includeRule("*.tmp")),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 4096L),
            ),
        )

        val restored = underTest(current)

        assertThat(underTest(restored)).isEqualTo(restored)
    }

    @Test
    fun `test that invoke re-enables a default the user had turned off`() {
        val disabled = MegaIgnoreDefaults.RULES.first().copy(enabled = false)

        val result = underTest(
            ParsedMegaIgnore(entries = listOf(MegaIgnoreEntry.RuleEntry(disabled)))
        )

        assertThat(result.rules).containsExactlyElementsIn(MegaIgnoreDefaults.RULES).inOrder()
        assertThat(result.rules.all { it.enabled }).isTrue()
    }

    @Test
    fun `test that invoke carries the byte order mark through`() {
        val result = underTest(
            ParsedMegaIgnore(
                entries = listOf(MegaIgnoreEntry.RuleEntry(excludeRule("Homework/**"))),
                hadBom = true,
            )
        )

        assertThat(result.hadBom).isTrue()
    }

    private fun defaultEntries(): List<MegaIgnoreEntry.RuleEntry> =
        MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) }

    private fun excludeRule(pattern: String) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = SyncExclusionClass.Exclude,
        matchType = SyncExclusionMatchType.Path,
        strategy = SyncMatchStrategy.GlobCaseSensitive,
    )

    private fun includeRule(pattern: String) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = SyncExclusionClass.Include,
        matchType = SyncExclusionMatchType.SubtreeName,
    )

    private companion object {
        const val REGEX_INCLUDE = "+r:.*\\.tmp$"
    }
}
