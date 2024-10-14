package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

class ShouldShowSyncPromotionUseCaseTest {

    private val syncPromotionPreferencesRepository: SyncPromotionPreferencesRepository = mock()
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase = mock {
        on { invoke() }.thenReturn(true)
    }

    private val underTest = ShouldShowSyncPromotionUseCase(
        syncPromotionPreferencesRepository = syncPromotionPreferencesRepository,
        isSyncFeatureEnabledUseCase = isSyncFeatureEnabledUseCase
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(syncPromotionPreferencesRepository)
    }

    @Test
    fun `test that if number of times shown condition is not valid then show sync promotion is not required`() =
        runTest {
            whenever(syncPromotionPreferencesRepository.getNumberOfTimesShown())
                .thenReturn(MAX_NUMBER_OF_TIMES)
            val lastTimestamp =
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TWO_WEEKS_IN_DAYS + 1)
            whenever(syncPromotionPreferencesRepository.getLastShownTimestamp())
                .thenReturn(lastTimestamp)
            val result = underTest()
            assertThat(result).isFalse()
        }

    @Test
    fun `test that if last shown timestamp condition is not valid then show sync promotion is not required`() =
        runTest {
            whenever(syncPromotionPreferencesRepository.getNumberOfTimesShown())
                .thenReturn(MAX_NUMBER_OF_TIMES - 1)
            val lastTimestamp =
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TWO_WEEKS_IN_DAYS)
            whenever(syncPromotionPreferencesRepository.getLastShownTimestamp())
                .thenReturn(lastTimestamp)
            val result = underTest()
            assertThat(result).isFalse()
        }

    @Test
    fun `test that if all conditions are valid then show sync promotion is required`() =
        runTest {
            whenever(syncPromotionPreferencesRepository.getNumberOfTimesShown())
                .thenReturn(MAX_NUMBER_OF_TIMES - 1)
            val lastTimestamp =
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TWO_WEEKS_IN_DAYS + 1)
            whenever(syncPromotionPreferencesRepository.getLastShownTimestamp())
                .thenReturn(lastTimestamp)
            val result = underTest()
            assertThat(result).isTrue()
        }

    companion object {
        private const val MAX_NUMBER_OF_TIMES = 6
        private const val TWO_WEEKS_IN_DAYS = 14L
    }
}