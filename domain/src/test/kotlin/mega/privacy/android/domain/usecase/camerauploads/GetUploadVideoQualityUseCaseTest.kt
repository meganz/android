package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [GetUploadVideoQualityUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUploadVideoQualityUseCaseTest {

    private lateinit var underTest: GetUploadVideoQualityUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetUploadVideoQualityUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "and returns {0}, and use case returns {0}")
    @MethodSource("provideMethodParams")
    fun `test that get upload video quality is invoked`(
        input: VideoQuality?,
        expected: VideoQuality,
    ) = runTest {
        whenever(cameraUploadRepository.getUploadVideoQuality()).thenReturn(input)
        val actual = underTest()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideMethodParams() = Stream.of(
        Arguments.of(VideoQuality.LOW, VideoQuality.LOW),
        Arguments.of(null, VideoQuality.ORIGINAL),
    )
}
