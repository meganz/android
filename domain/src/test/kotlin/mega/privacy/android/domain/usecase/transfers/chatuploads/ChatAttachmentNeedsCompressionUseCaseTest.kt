package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatAttachmentNeedsCompressionUseCaseTest {
    private lateinit var underTest: ChatAttachmentNeedsCompressionUseCase

    private val isImageFileUseCase = mock<IsImageFileUseCase>()
    private val isVideoFileUseCase = mock<IsVideoFileUseCase>()
    private val defaultSettingsRepository = mock<SettingsRepository>()
    private val networkRepository = mock<NetworkRepository>()

    @BeforeAll
    fun setup() {
        underTest = ChatAttachmentNeedsCompressionUseCase(
            isImageFileUseCase,
            isVideoFileUseCase,
            defaultSettingsRepository,
            networkRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            isImageFileUseCase,
            isVideoFileUseCase,
            defaultSettingsRepository,
            networkRepository,
        )

    @Nested
    @DisplayName("Test Image files")
    inner class Image {
        private val jpg = File("img.jpg")
        private val gif = File("img.gif")

        @BeforeEach
        fun setup() {
            wheneverBlocking { isVideoFileUseCase(any()) } doReturn false
            wheneverBlocking { isImageFileUseCase(jpg.absolutePath) } doReturn true
            wheneverBlocking { isImageFileUseCase(gif.absolutePath) } doReturn true
        }

        @Test
        fun `test that returns false when image is a GIF`() =
            runTest {
                val actual = underTest(gif)

                Truth.assertThat(actual).isFalse()
            }

        @Test
        fun `test that returns false when quality settings is Original`() =
            runTest {
                whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                        flowOf(ChatImageQuality.Original)

                val actual = underTest(jpg)

                Truth.assertThat(actual).isFalse()
            }

        @Test
        fun `test that returns false when quality settings is Automatic and there is WiFi connection`() =
            runTest {
                whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                        flowOf(ChatImageQuality.Automatic)
                whenever(networkRepository.isOnWifi()) doReturn true

                val actual = underTest(jpg)

                Truth.assertThat(actual).isFalse()
            }

        @Test
        fun `test that returns true when quality settings is Automatic and there is no WiFi connection`() =
            runTest {
                whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                        flowOf(ChatImageQuality.Automatic)
                whenever(networkRepository.isOnWifi()) doReturn false

                val actual = underTest(jpg)

                Truth.assertThat(actual).isTrue()
            }

        @Test
        fun `test that returns true when quality settings is Optimised`() =
            runTest {
                whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                        flowOf(ChatImageQuality.Optimised)

                val actual = underTest(jpg)

                Truth.assertThat(actual).isTrue()
            }

    }

    @Nested
    @DisplayName("Test Video files")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Video {
        private val mp4 = File("img.mp4")
        private val mpg = File("img.mpg")

        @BeforeEach
        fun setup() {
            wheneverBlocking { isImageFileUseCase(any()) } doReturn false
            wheneverBlocking { isVideoFileUseCase(mp4.absolutePath) } doReturn true
            wheneverBlocking { isVideoFileUseCase(mpg.absolutePath) } doReturn true
        }

        @Test
        fun `test that it returns false when extension is not 'mp4'`() = runTest {
            val actual = underTest(mpg)

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `test that it returns false when settings is original`() = runTest {
            whenever(defaultSettingsRepository.getChatVideoQualityPreference()) doReturn VideoQuality.ORIGINAL

            val actual = underTest(mpg)

            Truth.assertThat(actual).isFalse()
        }

        @ParameterizedTest
        @MethodSource("provideParams")
        fun `test that it returns true when settings is not original`(
            videoQuality: VideoQuality,
        ) = runTest {

            whenever(defaultSettingsRepository.getChatVideoQualityPreference()) doReturn videoQuality

            val actual = underTest(mp4)

            Truth.assertThat(actual)
                .isTrue()
        }

        private fun provideParams() = VideoQuality.entries.filter { it != VideoQuality.ORIGINAL }
    }
}