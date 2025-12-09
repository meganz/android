package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.days

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorEnableCameraUploadBannerVisibilityUseCaseTest {

    private lateinit var underTest: MonitorEnableCameraUploadBannerVisibilityUseCase

    private val environmentRepository: EnvironmentRepository = mock()
    private val photosRepository: PhotosRepository = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private var enableCameraUploadBannerDismissedTimestampFlow = MutableStateFlow<Long?>(null)

    @BeforeEach
    fun setup() {
        whenever(
            photosRepository.enableCameraUploadBannerDismissedTimestamp
        ) doReturn enableCameraUploadBannerDismissedTimestampFlow
        underTest = MonitorEnableCameraUploadBannerVisibilityUseCase(
            ioDispatcher = ioDispatcher,
            environmentRepository = environmentRepository,
            photosRepository = photosRepository
        )
    }

    @AfterEach
    fun tearDown() {
        enableCameraUploadBannerDismissedTimestampFlow = MutableStateFlow(null)
        reset(environmentRepository, photosRepository)
    }

    @Test
    fun `test that the enable CU banner is visible by default`() = runTest {
        enableCameraUploadBannerDismissedTimestampFlow.emit(null)

        underTest.enableCameraUploadBannerVisibilityFlow.test {
            assertThat(expectMostRecentItem()).isTrue()
        }
    }

    @Test
    fun `test that the enable CU banner is hidden when the time delta hasn't reached the threshold since the last dismissal`() =
        runTest {
            val now = System.currentTimeMillis()
            enableCameraUploadBannerDismissedTimestampFlow.emit(now)
            whenever(environmentRepository.now) doReturn now

            underTest.enableCameraUploadBannerVisibilityFlow.test {
                assertThat(expectMostRecentItem()).isFalse()
            }
        }

    @Test
    fun `test that the enable CU banner is visible when the time delta has reached the threshold since the last dismissal`() =
        runTest {
            val dismissTime = System.currentTimeMillis()
            enableCameraUploadBannerDismissedTimestampFlow.emit(dismissTime)
            val now = dismissTime + 15.days.inWholeMilliseconds
            whenever(environmentRepository.now) doReturn now

            underTest.enableCameraUploadBannerVisibilityFlow.test {
                assertThat(expectMostRecentItem()).isTrue()
            }
        }
}
