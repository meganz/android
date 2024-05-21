package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressVideoForChatUseCaseTest {

    private lateinit var underTest: CompressVideoForChatUseCase

    private val defaultSettingsRepository = mock<SettingsRepository>()
    private val getCacheFileForUploadUseCase =
        mock<GetCacheFileForUploadUseCase>()
    private val compressVideoUseCase = mock<CompressVideoUseCase>()


    @BeforeAll
    fun setup() {
        underTest = CompressVideoForChatUseCase(
            defaultSettingsRepository,
            getCacheFileForUploadUseCase,
            compressVideoUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            defaultSettingsRepository,
            getCacheFileForUploadUseCase,
            compressVideoUseCase,
        )

    @ParameterizedTest
    @MethodSource("provideParams")
    fun `test that it returns compressed video in the chat cache folder`(
        videoQuality: VideoQuality,
    ) = runTest {
        val file = stubFile()
        val expected = stubDestination()
        whenever(defaultSettingsRepository.getChatVideoQualityPreference()) doReturn videoQuality
        whenever(
            compressVideoUseCase(
                rootPath = expected.parent,
                filePath = file.absolutePath,
                newFilePath = expected.absolutePath,
                quality = videoQuality,
            )
        ) doReturn flowOf(VideoCompressionState.Finished)

        underTest(file).test {
            assertThat(awaitItem()).isEqualTo(ChatUploadCompressionState.Compressed(expected))
            awaitComplete()
        }
        verify(compressVideoUseCase).invoke(
            expected.parent,
            file.absolutePath,
            expected.absolutePath,
            videoQuality,
        )
    }

    @Test
    fun `test that progress is returned`() = runTest {
        val expected = 0.5f
        val file = stubFile()
        val destination = stubDestination()
        whenever(defaultSettingsRepository.getChatVideoQualityPreference()) doReturn VideoQuality.HIGH
        whenever(
            compressVideoUseCase(
                rootPath = destination.parent,
                filePath = file.absolutePath,
                newFilePath = destination.absolutePath,
                quality = VideoQuality.HIGH,
            )
        ) doReturn flowOf(VideoCompressionState.Progress(expected, 1, 1, ""))

        underTest(file).test {
            assertThat(awaitItem())
                .isEqualTo(ChatUploadCompressionState.Compressing(Progress(expected)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun provideParams() = VideoQuality.entries.filter { it != VideoQuality.ORIGINAL }

    private suspend fun stubDestination(): File {
        val destination = mock<File> {
            on { it.name } doReturn "destination"
            on { it.absolutePath } doReturn "root/destination"
            on { it.parent } doReturn "root"
            on { it.exists() } doReturn true
        }
        whenever(getCacheFileForUploadUseCase(any(), any())) doReturn destination
        return destination
    }

    private fun stubFile() =
        mock<File> {
            on { it.name } doReturn "file.mp4"
            on { it.absolutePath } doReturn "path"
        }
}