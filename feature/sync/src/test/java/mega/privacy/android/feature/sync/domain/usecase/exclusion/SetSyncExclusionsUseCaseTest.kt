package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
internal class SetSyncExclusionsUseCaseTest {

    private lateinit var underTest: SetSyncExclusionsUseCase

    private val syncExclusionsRepository = mock<SyncExclusionsRepository>()

    private val localRoot =
        UriPath("content://com.android.externalstorage.documents/tree/primary%3ASync")

    private val interleaved = ParsedMegaIgnore(
        entries = listOf(
            MegaIgnoreEntry.RuleEntry(ruleFor("*.tmp")),
            MegaIgnoreEntry.PreservedEntry("# authored by hand"),
            MegaIgnoreEntry.SizeLargerEntry(bytes = 1_048_576L),
            MegaIgnoreEntry.RuleEntry(ruleFor("Thumbs.db")),
        ),
    )

    @BeforeAll
    fun setup() {
        underTest = SetSyncExclusionsUseCase(syncExclusionsRepository = syncExclusionsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncExclusionsRepository)
    }

    @Test
    fun `test that invoke returns the success reported by the repository`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        val actual = underTest(localRoot, interleaved)

        assertThat(actual.isSuccess).isTrue()
    }

    @Test
    fun `test that invoke passes the local root and the exclusions to the repository unchanged`() =
        runTest {
            whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                    Result.success(Unit)

            underTest(localRoot, interleaved)

            verify(syncExclusionsRepository).setExclusions(localRoot, interleaved)
        }

    @Test
    fun `test that invoke hands the same exclusions instance to the repository`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        underTest(localRoot, interleaved)

        val captor = argumentCaptor<ParsedMegaIgnore>()
        verify(syncExclusionsRepository).setExclusions(any(), captor.capture())
        assertThat(captor.firstValue).isSameInstanceAs(interleaved)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideWriteFailures")
    fun `test that invoke returns a typed failure when the repository write fails`(
        name: String,
        error: Throwable,
    ) = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.failure(error)

        val actual = underTest(localRoot, interleaved)

        assertThat(actual.isFailure).isTrue()
        assertThat(actual.exceptionOrNull()).isInstanceOf(error.javaClass)
        assertThat(actual.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that invoke lets cancellation through rather than reporting it as a failure`() {
        syncExclusionsRepository.stub {
            onBlocking { setExclusions(localRoot, interleaved) } doThrow
                    CancellationException("Scope closed")
        }

        assertThrows(CancellationException::class.java) {
            runBlocking { underTest(localRoot, interleaved) }
        }
    }

    private fun provideWriteFailures(): Stream<Arguments> = Stream.of(
        Arguments.of("NoWriteAccess", ExclusionsError.NoWriteAccess()),
        Arguments.of("WriteFailed", ExclusionsError.WriteFailed()),
        Arguments.of("RuntimeException", RuntimeException("Not an ExclusionsError")),
    )

    private companion object {
        fun ruleFor(pattern: String) = SyncExclusionRule(
            pattern = pattern,
            matchType = SyncExclusionMatchType.SubtreeName,
        )
    }
}
