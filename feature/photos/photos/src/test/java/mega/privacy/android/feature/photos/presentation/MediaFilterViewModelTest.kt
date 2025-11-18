package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.MediaFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaFilterViewModelTest {

    private lateinit var underTest: MediaFilterViewModel

    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase = mock()
    private val mediaFilterUiStateMapper: MediaFilterUiStateMapper = mock()

    @BeforeEach
    fun setup() {
        underTest = MediaFilterViewModel(
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            mediaFilterUiStateMapper = mediaFilterUiStateMapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getTimelineFilterPreferencesUseCase,
            mediaFilterUiStateMapper
        )
    }

    @Test
    fun `test that the correct mapped ui state is returned`() = runTest {
        val preferenceMap = mapOf<String, String?>()
        whenever(getTimelineFilterPreferencesUseCase()) doReturn preferenceMap
        val isRemembered = Random.nextBoolean()
        val mediaType = FilterMediaType.entries.random()
        val mediaSource = FilterMediaSource.entries.random()
        val mappedUiState = MediaFilterUiState(
            isRemembered = isRemembered,
            mediaType = mediaType,
            mediaSource = mediaSource
        )
        whenever(mediaFilterUiStateMapper(preferenceMap = preferenceMap)) doReturn mappedUiState

        underTest.uiState.test {
            assertThat(expectMostRecentItem()).isEqualTo(mappedUiState)
        }
    }
}
