package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ExclusionsSnapshot
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.exception.ExclusionsError
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CancellationException
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncExclusionsUseCaseTest {

    private lateinit var underTest: GetSyncExclusionsUseCase

    private val syncExclusionsRepository = mock<SyncExclusionsRepository>()

    private val localRoot =
        UriPath("content://com.android.externalstorage.documents/tree/primary%3ASync")

    private val exclusions = ParsedMegaIgnore(
        entries = listOf(MegaIgnoreEntry.RuleEntry(ruleFor("*.tmp"))),
    )

    @BeforeAll
    fun setup() {
        underTest = GetSyncExclusionsUseCase(syncExclusionsRepository = syncExclusionsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncExclusionsRepository)
    }

    @Test
    fun `test that invoke returns the snapshot read by the repository`() = runTest {
        val snapshot = ExclusionsSnapshot(exclusions = exclusions, isWritable = true)
        whenever(syncExclusionsRepository.getExclusions(localRoot)) doReturn
                Result.success(snapshot)

        val actual = underTest(localRoot)

        assertThat(actual.getOrNull()).isSameInstanceAs(snapshot)
    }

    @Test
    fun `test that invoke passes the local root to the repository unchanged`() = runTest {
        whenever(syncExclusionsRepository.getExclusions(localRoot)) doReturn
                Result.success(ExclusionsSnapshot(exclusions = exclusions, isWritable = true))

        underTest(localRoot)

        verify(syncExclusionsRepository).getExclusions(localRoot)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideReadFailures")
    fun `test that invoke returns a typed failure when the repository read fails`(
        name: String,
        error: Throwable,
    ) = runTest {
        whenever(syncExclusionsRepository.getExclusions(localRoot)) doReturn Result.failure(error)

        val actual = underTest(localRoot)

        assertThat(actual.isFailure).isTrue()
        assertThat(actual.exceptionOrNull()).isInstanceOf(error.javaClass)
        assertThat(actual.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that invoke lets cancellation through rather than reporting it as a failure`() {
        syncExclusionsRepository.stub {
            onBlocking { getExclusions(localRoot) } doThrow CancellationException("Scope closed")
        }

        assertThrows(CancellationException::class.java) {
            runBlocking { underTest(localRoot) }
        }
    }

    @Test
    fun `test that invoke returns a success holding an empty model when the sync has no rules`() =
        runTest {
            val snapshot = ExclusionsSnapshot(
                exclusions = ParsedMegaIgnore(entries = emptyList()),
                isWritable = true,
            )
            whenever(syncExclusionsRepository.getExclusions(localRoot)) doReturn
                    Result.success(snapshot)

            val actual = underTest(localRoot)

            assertThat(actual.isSuccess).isTrue()
            assertThat(actual.isFailure).isFalse()
            assertThat(actual.exceptionOrNull()).isNull()
            assertThat(actual.getOrNull()?.exclusions?.entries).isEmpty()
        }

    @Test
    fun `test that invoke returns isWritable false unchanged when the sync root is read only`() =
        runTest {
            val snapshot = ExclusionsSnapshot(exclusions = exclusions, isWritable = false)
            whenever(syncExclusionsRepository.getExclusions(localRoot)) doReturn
                    Result.success(snapshot)

            val actual = underTest(localRoot)

            assertThat(actual.getOrNull()?.isWritable).isFalse()
            assertThat(actual.getOrNull()?.exclusions).isSameInstanceAs(exclusions)
        }

    private fun provideReadFailures(): Stream<Arguments> = Stream.of(
        Arguments.of("ReadFailed", ExclusionsError.ReadFailed()),
        Arguments.of("NoPermission", ExclusionsError.NoPermission()),
        Arguments.of("ProviderRejectedDotfile", ExclusionsError.ProviderRejectedDotfile()),
        Arguments.of("RuntimeException", RuntimeException("Not an ExclusionsError")),
    )

    private companion object {
        fun ruleFor(pattern: String) = SyncExclusionRule(
            pattern = pattern,
            matchType = SyncExclusionMatchType.SubtreeName,
        )
    }
}
