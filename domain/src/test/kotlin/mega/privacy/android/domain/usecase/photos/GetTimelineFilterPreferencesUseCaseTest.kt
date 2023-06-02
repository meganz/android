package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetTimelineFilterPreferencesUseCaseTest {
    private lateinit var underTest: GetTimelineFilterPreferencesUseCase

    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = GetTimelineFilterPreferencesUseCase(photosRepository = photosRepository)
    }

    @Test
    fun `test that null is returned if there is no preference`() = runTest {
        whenever(photosRepository.getTimelineFilterPreferences()).thenReturn(null)

        Truth.assertThat(underTest()).isNull()
    }

    @Test
    fun `test that the proper preferences is returned`() = runTest {
        val expectedPreferences = mapOf(Pair("abc", "bcd"))
        whenever(photosRepository.getTimelineFilterPreferences()).thenReturn(
            mapOf(Pair("abc", "bcd"))
        )

        val actualPreferences = underTest()

        Truth.assertThat(actualPreferences).isNotNull()
        Truth.assertThat(actualPreferences).isEqualTo(expectedPreferences)
    }
}