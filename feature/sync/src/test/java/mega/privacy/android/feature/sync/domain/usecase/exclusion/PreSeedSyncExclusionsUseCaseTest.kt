package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.exception.ExclusionsError
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PreSeedSyncExclusionsUseCaseTest {

    private lateinit var underTest: PreSeedSyncExclusionsUseCase

    private val syncExclusionsRepository = mock<SyncExclusionsRepository>()

    private val localRoot =
        UriPath("content://com.android.externalstorage.documents/tree/primary%3ASync")

    private val defaultEntries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) }

    @BeforeAll
    fun setup() {
        underTest = PreSeedSyncExclusionsUseCase(
            syncExclusionsRepository = syncExclusionsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncExclusionsRepository)
    }

    @Test
    fun `test that invoke returns the success reported by the repository`() = runTest {
        stubWrite(Result.success(Unit))

        val actual = underTest(localRoot, staged(ruleFor("*.iso")))

        assertThat(actual.isSuccess).isTrue()
    }

    @ParameterizedTest
    @MethodSource("writeFailures")
    fun `test that invoke returns the failure reported by the repository`(
        error: ExclusionsError,
    ) = runTest {
        stubWrite(Result.failure(error))

        val actual = underTest(localRoot, staged(ruleFor("*.iso")))

        assertThat(actual.isFailure).isTrue()
        assertThat(actual.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that invoke writes to the staged local root`() = runTest {
        stubWrite(Result.success(Unit))

        underTest(localRoot, staged(ruleFor("*.iso")))

        verify(syncExclusionsRepository).setExclusions(eq(localRoot), any())
    }

    @Test
    fun `test that invoke prepends every default when the staged exclusions carry none`() =
        runTest {
            stubWrite(Result.success(Unit))

            underTest(localRoot, staged(ruleFor("*.iso")))

            assertThat(written().entries.take(defaultEntries.size))
                .containsExactlyElementsIn(defaultEntries)
                .inOrder()
        }

    @Test
    fun `test that invoke keeps the staged entries in their original order after the defaults`() =
        runTest {
            stubWrite(Result.success(Unit))
            val stagedEntries = listOf(
                MegaIgnoreEntry.RuleEntry(ruleFor("*.iso")),
                MegaIgnoreEntry.PreservedEntry("# authored by hand"),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 1_048_576L),
                MegaIgnoreEntry.RuleEntry(ruleFor("*.dmg")),
            )

            underTest(localRoot, ParsedMegaIgnore(entries = stagedEntries))

            assertThat(written().entries.drop(defaultEntries.size))
                .containsExactlyElementsIn(stagedEntries)
                .inOrder()
        }

    @Test
    fun `test that invoke does not seed when the staged exclusions already carry a default`() =
        runTest {
            stubWrite(Result.success(Unit))
            val staged = staged(MegaIgnoreDefaults.RULES.first(), ruleFor("*.iso"))

            underTest(localRoot, staged)

            assertThat(written()).isEqualTo(staged)
        }

    @Test
    fun `test that invoke keeps a staged default disabled`() = runTest {
        stubWrite(Result.success(Unit))
        val disabledDefault = MegaIgnoreDefaults.RULES.first().copy(enabled = false)

        underTest(localRoot, staged(disabledDefault))

        val rules = written().entries.filterIsInstance<MegaIgnoreEntry.RuleEntry>()
        assertThat(rules.single { it.rule.pattern == disabledDefault.pattern }.rule.enabled)
            .isFalse()
    }

    @Test
    fun `test that invoke does not restore a default deleted from the staged exclusions`() =
        runTest {
            stubWrite(Result.success(Unit))
            val deleted = MegaIgnoreDefaults.RULES.first()
            val staged = staged(*MegaIgnoreDefaults.RULES.drop(1).toTypedArray())

            underTest(localRoot, staged)

            val rules = written().entries.filterIsInstance<MegaIgnoreEntry.RuleEntry>()
            assertThat(rules.none { it.rule.pattern == deleted.pattern }).isTrue()
        }

    @Test
    fun `test that invoke does not restore the original of an edited default`() = runTest {
        stubWrite(Result.success(Unit))
        val original = MegaIgnoreDefaults.RULES.first()
        val narrowed = original.copy(target = SyncExclusionTarget.Files)
        val staged = staged(narrowed, *MegaIgnoreDefaults.RULES.drop(1).toTypedArray())

        underTest(localRoot, staged)

        val rules = written().entries.filterIsInstance<MegaIgnoreEntry.RuleEntry>()
        assertThat(rules.none { it.rule == original }).isTrue()
    }

    @Test
    fun `test that invoke writes the staged exclusions unchanged when they carry every default`() =
        runTest {
            stubWrite(Result.success(Unit))
            val staged = staged(*MegaIgnoreDefaults.RULES.toTypedArray())

            underTest(localRoot, staged)

            assertThat(written()).isEqualTo(staged)
        }

    @Test
    fun `test that invoke seeds a staged rule that only looks like a default`() = runTest {
        stubWrite(Result.success(Unit))
        val lookalike = MegaIgnoreDefaults.RULES.first().copy(
            target = SyncExclusionTarget.Directories,
        )

        underTest(localRoot, staged(lookalike))

        assertThat(written().entries.take(defaultEntries.size))
            .containsExactlyElementsIn(defaultEntries)
            .inOrder()
    }

    @Test
    fun `test that invoke keeps a staged include after the seeded defaults`() = runTest {
        stubWrite(Result.success(Unit))
        val include = ruleFor("*.tmp").copy(exclusionClass = SyncExclusionClass.Include)

        underTest(localRoot, staged(include))

        val rules = written().entries.filterIsInstance<MegaIgnoreEntry.RuleEntry>()
        assertThat(rules.last().rule).isEqualTo(include)
    }

    @Test
    fun `test that invoke preserves the byte order mark flag`() = runTest {
        stubWrite(Result.success(Unit))

        underTest(localRoot, ParsedMegaIgnore(entries = emptyList(), hadBom = true))

        assertThat(written().hadBom).isTrue()
    }

    @Test
    fun `test that invoke adds nothing but the defaults`() = runTest {
        stubWrite(Result.success(Unit))
        val stagedEntries = listOf(
            MegaIgnoreEntry.RuleEntry(ruleFor("*.iso")),
            MegaIgnoreEntry.PreservedEntry("# authored by hand"),
        )

        underTest(localRoot, ParsedMegaIgnore(entries = stagedEntries))

        assertThat(written().entries - stagedEntries.toSet())
            .containsExactlyElementsIn(defaultEntries)
    }

    @Test
    fun `test that invoke keeps entries this app does not model`() = runTest {
        stubWrite(Result.success(Unit))
        val preserved = MegaIgnoreEntry.PreservedEntry("-b:*.regex")

        underTest(localRoot, ParsedMegaIgnore(entries = listOf(preserved)))

        assertThat(written().entries).contains(preserved)
    }

    @Test
    fun `test that invoke makes no call other than the write`() = runTest {
        stubWrite(Result.success(Unit))

        underTest(localRoot, staged(ruleFor("*.iso")))

        verify(syncExclusionsRepository).setExclusions(any(), any())
        verifyNoMoreInteractions(syncExclusionsRepository)
    }

    private suspend fun stubWrite(result: Result<Unit>) {
        whenever(syncExclusionsRepository.setExclusions(any(), any())) doReturn result
    }

    private suspend fun written(): ParsedMegaIgnore {
        val captor = argumentCaptor<ParsedMegaIgnore>()
        verify(syncExclusionsRepository).setExclusions(any(), captor.capture())
        return captor.firstValue
    }

    private fun staged(vararg rules: SyncExclusionRule) =
        ParsedMegaIgnore(entries = rules.map { MegaIgnoreEntry.RuleEntry(it) })

    companion object {
        @JvmStatic
        fun writeFailures(): Stream<Arguments> = Stream.of(
            Arguments.of(ExclusionsError.WriteFailed()),
            Arguments.of(ExclusionsError.NoWriteAccess()),
            Arguments.of(ExclusionsError.ProviderRejectedDotfile()),
            Arguments.of(ExclusionsError.NoPermission()),
        )

        fun ruleFor(pattern: String) = SyncExclusionRule(
            pattern = pattern,
            matchType = SyncExclusionMatchType.SubtreeName,
        )
    }
}
