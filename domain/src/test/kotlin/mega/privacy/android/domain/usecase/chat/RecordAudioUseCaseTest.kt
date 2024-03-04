package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaRecorderRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordAudioUseCaseTest {
    private lateinit var underTest: RecordAudioUseCase

    private val mediaRecorderRepository = mock<MediaRecorderRepository>()

    @BeforeAll
    fun setup() {
        underTest = RecordAudioUseCase(mediaRecorderRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaRecorderRepository)
    }

    @Test
    fun `test that media recorder repository result is returned when the use case is invoked`() =
        runTest {
            val destination = mock<File>()
            val expected = mock<Flow<Int>>()
            whenever(mediaRecorderRepository.recordAudio(destination)) doReturn expected
            val actual = underTest(destination)
            assertThat(actual).isEqualTo(expected)
        }
}