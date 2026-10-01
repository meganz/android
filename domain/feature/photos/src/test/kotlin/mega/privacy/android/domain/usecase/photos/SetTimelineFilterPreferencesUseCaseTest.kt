package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class SetTimelineFilterPreferencesUseCaseTest {

    private lateinit var underTest: SetTimelineFilterPreferencesUseCase

    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = SetTimelineFilterPreferencesUseCase(photosRepository = photosRepository)
    }

    @Test
    fun `test that the correct result is returned`() = runTest {
        val newPreferences = mapOf(Pair("abc", "123"))
        val expectedString = newPreferences.toString()

        whenever(photosRepository.setTimelineFilterPreferences(any())).thenReturn(
            expectedString
        )

        try {
            val actualString = underTest(newPreferences)
            Truth.assertThat(actualString).isEqualTo(expectedString)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }
}