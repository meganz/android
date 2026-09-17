package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreFileMapper
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreLineMapper
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetSyncSizeBoundsUseCaseTest {

    private val underTest = SetSyncSizeBoundsUseCase()
    private val fileMapper = MegaIgnoreFileMapper(MegaIgnoreLineMapper())

    @Test
    fun `test that invoke declares both bounds when the file had none`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(comment, rule)),
            bounds = SyncSizeExclusion(
                excludeSmallerThanBytes = 1_024,
                excludeLargerThanBytes = 2_048,
            ),
        ).getOrThrow()

        assertThat(actual.size).isEqualTo(
            SyncSizeExclusion(excludeSmallerThanBytes = 1_024, excludeLargerThanBytes = 2_048)
        )
    }

    @Test
    fun `test that invoke declares one bound when only one is supplied`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(rule)),
            bounds = SyncSizeExclusion(excludeLargerThanBytes = 4_096),
        ).getOrThrow()

        assertThat(actual.size)
            .isEqualTo(SyncSizeExclusion(excludeLargerThanBytes = 4_096))
        assertThat(actual.entries.filterIsInstance<MegaIgnoreEntry.SizeSmallerEntry>()).isEmpty()
    }

    @Test
    fun `test that invoke clears both bounds when neither is supplied`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(smaller(1_024), rule, larger(2_048))),
            bounds = SyncSizeExclusion(),
        ).getOrThrow()

        assertThat(actual.size).isEqualTo(SyncSizeExclusion())
        assertThat(actual.entries).containsExactly(rule)
    }

    @Test
    fun `test that invoke drops the bound the caller left out rather than keeping it`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(smaller(1_024), larger(2_048))),
            bounds = SyncSizeExclusion(excludeLargerThanBytes = 4_096),
        ).getOrThrow()

        assertThat(actual.size).isEqualTo(SyncSizeExclusion(excludeLargerThanBytes = 4_096))
    }

    /**
     * The SDK applies the last line of each kind, so a duplicate left behind becomes the bound the
     * moment the last one is removed — the cleared bound would appear to come back.
     */
    @Test
    fun `test that invoke removes every earlier duplicate of a bound, not only the last`() {
        val actual = underTest(
            current = ParsedMegaIgnore(
                listOf(smaller(512), rule, smaller(1_024), larger(2_048), larger(4_096))
            ),
            bounds = SyncSizeExclusion(),
        ).getOrThrow()

        assertThat(actual.entries).containsExactly(rule)
    }

    @Test
    fun `test that invoke leaves every other line in the position it held`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(comment, smaller(512), rule, blank, invalid)),
            bounds = SyncSizeExclusion(excludeSmallerThanBytes = 1_024),
        ).getOrThrow()

        assertThat(actual.entries)
            .containsExactly(comment, smaller(1_024), rule, blank, invalid).inOrder()
    }

    @Test
    fun `test that invoke keeps the byte order mark the file was read with`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(rule), hadBom = true),
            bounds = SyncSizeExclusion(excludeLargerThanBytes = 2_048),
        ).getOrThrow()

        assertThat(actual.hadBom).isTrue()
    }

    @Test
    fun `test that invoke refuses a pair that would make the SDK abandon the file`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(rule)),
            bounds = SyncSizeExclusion(
                excludeSmallerThanBytes = 1_024,
                excludeLargerThanBytes = 1_024,
            ),
        )

        val error = actual.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.EqualSizeBounds::class.java)
        assertThat((error as SyncExclusionEditError.EqualSizeBounds).bytes).isEqualTo(1_024)
    }

    @ParameterizedTest(name = "a bound of {0} is refused")
    @ValueSource(longs = [0L, -1L, Long.MIN_VALUE])
    fun `test that invoke refuses a bound that is not a usable size`(bytes: Long) {
        val fromLarger = underTest(
            current = ParsedMegaIgnore(listOf(rule)),
            bounds = SyncSizeExclusion(excludeLargerThanBytes = bytes),
        )
        val fromSmaller = underTest(
            current = ParsedMegaIgnore(listOf(rule)),
            bounds = SyncSizeExclusion(excludeSmallerThanBytes = bytes),
        )

        listOf(fromLarger, fromSmaller).forEach { result ->
            val error = result.exceptionOrNull()
            assertThat(error).isInstanceOf(SyncExclusionEditError.UnusableSizeBound::class.java)
            assertThat((error as SyncExclusionEditError.UnusableSizeBound).bytes).isEqualTo(bytes)
        }
    }

    @Test
    fun `test that invoke accepts a lower bound above the upper one, which the SDK allows`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(rule)),
            bounds = SyncSizeExclusion(
                excludeSmallerThanBytes = 2_048,
                excludeLargerThanBytes = 1_024,
            ),
        )

        assertThat(actual.isSuccess).isTrue()
    }

    /**
     * A bound too large to hold in a `Long` is parsed as a preserved line rather than as a bound,
     * so it is invisible to both [ParsedMegaIgnore.size] and this use case, and clearing leaves it
     * in force. The lower bound is the harmful direction — an over-large upper bound is the SDK's
     * own default and changes nothing. Pinned as the known parser gap it is.
     */
    @Test
    fun `test that invoke cannot clear a bound too large to be modelled`() {
        val oversized = MegaIgnoreEntry.PreservedEntry("exclude-smaller:9223372036854775808")

        val actual = underTest(
            current = ParsedMegaIgnore(listOf(oversized, smaller(2_048))),
            bounds = SyncSizeExclusion(),
        ).getOrThrow()

        assertThat(actual.size).isEqualTo(SyncSizeExclusion())
        assertThat(actual.entries).containsExactly(oversized)
    }

    /**
     * A bound that collided with the bounds before it is a well-formed line held as invalid. Taking
     * the bounds around it away would make it live again, so it has to go with them — otherwise
     * clearing the bounds hands the sync a limit the user never set and the app cannot see.
     */
    @Test
    fun `test that invoke takes away a bound the file holds as an equal-bounds collision`() {
        val collided = MegaIgnoreEntry.InvalidEntry(
            "exclude-larger:100",
            InvalidLineReason.EqualSizeBounds,
        )

        val actual = underTest(
            current = ParsedMegaIgnore(listOf(smaller(100), collided, larger(200))),
            bounds = SyncSizeExclusion(),
        ).getOrThrow()

        assertThat(actual.entries).isEmpty()
    }

    @Test
    fun `test that invoke leaves an invalid line alone when its own text is what is wrong`() {
        val malformed = MegaIgnoreEntry.InvalidEntry(
            "exclude-larger:abc",
            InvalidLineReason.MalformedSizeValue,
        )

        val actual = underTest(
            current = ParsedMegaIgnore(listOf(malformed, larger(200))),
            bounds = SyncSizeExclusion(),
        ).getOrThrow()

        assertThat(actual.entries).containsExactly(malformed)
    }

    @Test
    fun `test that invoke declares each bound exactly once`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(smaller(1), smaller(2), larger(3), larger(4))),
            bounds = SyncSizeExclusion(
                excludeSmallerThanBytes = 1_024,
                excludeLargerThanBytes = 2_048,
            ),
        ).getOrThrow()

        assertThat(actual.entries).containsExactly(smaller(1_024), larger(2_048)).inOrder()
    }

    @Test
    fun `test that invoke keeps the bounds where the file already held them`() {
        val actual = underTest(
            current = ParsedMegaIgnore(listOf(comment, smaller(512), rule)),
            bounds = SyncSizeExclusion(excludeSmallerThanBytes = 1_024),
        ).getOrThrow()

        assertThat(actual.entries).containsExactly(comment, smaller(1_024), rule).inOrder()
    }

    /**
     * The check both high-severity review findings failed: the produced file is only correct if the
     * SDK reads back the bounds that were asked for, and still reads the file at all.
     */
    @Test
    fun `test that the file written from the result declares the bounds and is still read`() {
        val current = fileMapper("exclude-smaller:100\nexclude-larger:100\n-n:*.tmp\n")
        val bounds = SyncSizeExclusion(
            excludeSmallerThanBytes = 1_024,
            excludeLargerThanBytes = 4_096,
        )

        val reparsed = fileMapper(fileMapper(underTest(current, bounds).getOrThrow()))

        assertThat(reparsed.size).isEqualTo(bounds)
        assertThat(reparsed.isRejectedBySdk).isFalse()
        assertThat(reparsed.rules).hasSize(1)
    }

    @Test
    fun `test that the file written after clearing declares no bound and is still read`() {
        val current = fileMapper("exclude-smaller:100\nexclude-larger:100\n-n:*.tmp\n")

        val reparsed = fileMapper(
            fileMapper(underTest(current, SyncSizeExclusion()).getOrThrow())
        )

        assertThat(reparsed.size).isEqualTo(SyncSizeExclusion())
        assertThat(reparsed.isRejectedBySdk).isFalse()
        assertThat(reparsed.rules).hasSize(1)
    }

    private fun smaller(bytes: Long) = MegaIgnoreEntry.SizeSmallerEntry(bytes)
    private fun larger(bytes: Long) = MegaIgnoreEntry.SizeLargerEntry(bytes)

    private val comment = MegaIgnoreEntry.PreservedEntry("# notes")
    private val blank = MegaIgnoreEntry.PreservedEntry("")
    private val invalid = MegaIgnoreEntry.InvalidEntry("-q:nope", InvalidLineReason.UnknownDirective)
    private val rule = MegaIgnoreEntry.RuleEntry(
        SyncExclusionRule(pattern = "*.tmp", matchType = SyncExclusionMatchType.SubtreeName)
    )
}
