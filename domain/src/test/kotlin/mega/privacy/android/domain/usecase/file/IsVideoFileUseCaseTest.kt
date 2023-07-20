package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsVideoFileUseCaseTest {

    private lateinit var underTest: IsVideoFileUseCase

    private lateinit var fileSystemRepository: FileSystemRepository

    @BeforeAll
    fun setup() {
        fileSystemRepository = mock()
        underTest = IsVideoFileUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest(name = "when local path is {0} and content type is {1} the result is {2}")
    @MethodSource("provideParameters")
    fun `test that IsVideoFileUseCase returns correctly`(
        localPath: String,
        contentType: String?,
        expectedResult: Boolean,
    ) = runTest {
        whenever(fileSystemRepository.getGuessContentTypeFromName(localPath)).thenReturn(contentType)
        Truth.assertThat(underTest.invoke(localPath)).isEqualTo(expectedResult)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of("/", null, false),
        Arguments.of("/any/video.mp4", "video", true),
        Arguments.of("/any/image.png", "", false)
    )
}