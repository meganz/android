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
import mega.privacy.android.feature.sync.domain.usecase.sync.RescanSyncUseCase
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
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.CancellationException
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetSyncExclusionsUseCaseTest {

    private lateinit var underTest: SetSyncExclusionsUseCase

    private val syncExclusionsRepository = mock<SyncExclusionsRepository>()
    private val rescanSyncUseCase = mock<RescanSyncUseCase>()

    private val localRoot =
        UriPath("content://com.android.externalstorage.documents/tree/primary%3ASync")

    private val syncId = 8_113L

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
        underTest = SetSyncExclusionsUseCase(
            syncExclusionsRepository = syncExclusionsRepository,
            rescanSyncUseCase = rescanSyncUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncExclusionsRepository, rescanSyncUseCase)
    }

    @Test
    fun `test that invoke returns the success reported by the repository`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        val actual = underTest(syncId, localRoot, interleaved)

        assertThat(actual.isSuccess).isTrue()
    }

    @Test
    fun `test that invoke passes the local root and the exclusions to the repository unchanged`() =
        runTest {
            whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                    Result.success(Unit)

            underTest(syncId, localRoot, interleaved)

            verify(syncExclusionsRepository).setExclusions(localRoot, interleaved)
        }

    @Test
    fun `test that invoke hands the same exclusions instance to the repository`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        underTest(syncId, localRoot, interleaved)

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

        val actual = underTest(syncId, localRoot, interleaved)

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
            runBlocking { underTest(syncId, localRoot, interleaved) }
        }
    }

    @Test
    fun `test that invoke rescans the sync once the write succeeds`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        underTest(syncId, localRoot, interleaved)

        verify(rescanSyncUseCase).invoke(syncId)
    }

    @Test
    fun `test that invoke writes the exclusions before it rescans the sync`() = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.success(Unit)

        underTest(syncId, localRoot, interleaved)

        val order = inOrder(syncExclusionsRepository, rescanSyncUseCase)
        order.verify(syncExclusionsRepository).setExclusions(localRoot, interleaved)
        order.verify(rescanSyncUseCase).invoke(syncId)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideWriteFailures")
    fun `test that invoke does not rescan the sync when the write fails`(
        name: String,
        error: Throwable,
    ) = runTest {
        whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                Result.failure(error)

        underTest(syncId, localRoot, interleaved)

        verifyNoInteractions(rescanSyncUseCase)
    }

    @Test
    fun `test that invoke returns a failure carrying the throwable when the rescan fails`() =
        runTest {
            val error = RuntimeException("The SDK rejected the rescan")
            whenever(syncExclusionsRepository.setExclusions(localRoot, interleaved)) doReturn
                    Result.success(Unit)
            rescanSyncUseCase.stub { onBlocking { invoke(syncId) } doThrow error }

            val actual = underTest(syncId, localRoot, interleaved)

            assertThat(actual.isFailure).isTrue()
            assertThat(actual.exceptionOrNull()).isSameInstanceAs(error)
        }

    @Test
    fun `test that invoke lets cancellation from the rescan through rather than reporting it as a failure`() {
        syncExclusionsRepository.stub {
            onBlocking { setExclusions(localRoot, interleaved) } doReturn Result.success(Unit)
        }
        rescanSyncUseCase.stub {
            onBlocking { invoke(syncId) } doThrow CancellationException("Scope closed")
        }

        assertThrows(CancellationException::class.java) {
            runBlocking { underTest(syncId, localRoot, interleaved) }
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
