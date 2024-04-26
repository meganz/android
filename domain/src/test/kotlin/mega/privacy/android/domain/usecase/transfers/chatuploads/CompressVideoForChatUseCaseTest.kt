package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
    private val getCacheFileForChatUploadUseCase =
        mock<GetCacheFileForChatUploadUseCase>()
    private val compressVideoUseCase = mock<CompressVideoUseCase>()


    @BeforeAll
    fun setup() {
        underTest = CompressVideoForChatUseCase(
            defaultSettingsRepository,
            getCacheFileForChatUploadUseCase,
            compressVideoUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            defaultSettingsRepository,
            getCacheFileForChatUploadUseCase,
            compressVideoUseCase,
        )

    @ParameterizedTest
    @MethodSource("provideParams")
    fun `test that it returns compressed video in the chat cache folder`(
        videoQuality: VideoQuality,
    ) = runTest {
        val path = "path"
        val file = mock<File> {
            on { it.name } doReturn "file.mp4"
            on { it.absolutePath } doReturn path
        }
        val expected = stubDestination()
        whenever(defaultSettingsRepository.getChatVideoQualityPreference()) doReturn videoQuality
        whenever(
            compressVideoUseCase(
                rootPath = expected.parent,
                filePath = path,
                newFilePath = expected.absolutePath,
                quality = videoQuality,
            )
        ) doReturn emptyFlow()

        val actual = underTest(file)
        assertThat(actual).isEqualTo(expected)
        verify(compressVideoUseCase).invoke(
            expected.parent,
            path,
            expected.absolutePath,
            videoQuality,
        )
    }

    private fun provideParams() = VideoQuality.entries.filter { it != VideoQuality.ORIGINAL }

    private suspend fun stubDestination(): File {
        val destination = mock<File> {
            on { it.name } doReturn "destination"
            on { it.absolutePath } doReturn "root/destination"
            on { it.parent } doReturn "root"
            on { it.exists() } doReturn true
        }
        whenever(getCacheFileForChatUploadUseCase(any())) doReturn destination
        return destination
    }
}