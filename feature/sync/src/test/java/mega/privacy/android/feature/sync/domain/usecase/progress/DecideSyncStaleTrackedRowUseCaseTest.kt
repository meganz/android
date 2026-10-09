package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncStaleTrackedRowUseCase.StaleStep
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DecideSyncStaleTrackedRowUseCaseTest {

    private lateinit var underTest: DecideSyncStaleTrackedRowUseCase

    @BeforeAll
    fun setUp() {
        underTest = DecideSyncStaleTrackedRowUseCase()
    }

    @ParameterizedTest(name = "{0} at {1}, present {2}, kept before {3} gives {4}")
    @MethodSource("trackedRows")
    fun `test that invoke decides a tracked row after a reseed as the reconcile table says`(
        state: TransferState,
        notificationNumber: Long,
        isPresent: Boolean,
        isPreviouslyKept: Boolean,
        expected: StaleStep,
    ) {
        val step = underTest(state, notificationNumber, WATERMARK, isPresent, isPreviouslyKept)

        assertThat(step).isEqualTo(expected)
    }

    private fun trackedRows(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_QUEUED, 4L, true, false, StaleStep.Untouched),
        Arguments.of(TransferState.STATE_QUEUED, 6L, false, false, StaleStep.Untouched),
        Arguments.of(TransferState.STATE_QUEUED, 5L, false, false, StaleStep.Remove),
        Arguments.of(TransferState.STATE_ACTIVE, 4L, false, true, StaleStep.Remove),
        Arguments.of(TransferState.STATE_COMPLETING, 4L, false, false, StaleStep.KeepCompleting),
        Arguments.of(TransferState.STATE_COMPLETING, 4L, false, true, StaleStep.Remove),
        Arguments.of(TransferState.STATE_COMPLETING, 4L, true, true, StaleStep.Untouched),
        Arguments.of(TransferState.STATE_COMPLETING, 6L, false, true, StaleStep.Untouched),
    )

    private companion object {
        const val WATERMARK = 5L
    }
}
