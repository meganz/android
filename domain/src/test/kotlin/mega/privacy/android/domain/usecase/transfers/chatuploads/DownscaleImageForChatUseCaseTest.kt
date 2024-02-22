package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.transfers.chatuploads.DownscaleImageForChatUseCase.Companion.DOWNSCALE_IMAGES_PX
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownscaleImageForChatUseCaseTest {
    private lateinit var underTest: DownscaleImageForChatUseCase

    private val defaultSettingsRepository = mock<SettingsRepository>()
    private val networkRepository = mock<NetworkRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getCacheFileForChatUploadUseCase =
        mock<GetCacheFileForChatUploadUseCase>()

    @BeforeAll
    fun setup() {
        underTest = DownscaleImageForChatUseCase(
            defaultSettingsRepository,
            networkRepository,
            fileSystemRepository,
            getCacheFileForChatUploadUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            defaultSettingsRepository,
            networkRepository,
            fileSystemRepository,
            getCacheFileForChatUploadUseCase,
        )

    @Test
    fun `test that gifs are not scaled`() = runTest {
        val file = File("img.gif")
        assertThat(underTest(file)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that webp are not scaled`() = runTest {
        val file = File("img.webp")
        assertThat(underTest(file)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that image is not scaled when quality settings is Original`() = runTest {
        val file = File("img.jpg")
        whenever(defaultSettingsRepository.getChatImageQuality()) doReturn flowOf(ChatImageQuality.Original)
        assertThat(underTest(file)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that image is not scaled when quality settings is Automatic and there is WiFi connection`() =
        runTest {
            val file = File("img.jpg")
            whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                    flowOf(ChatImageQuality.Automatic)
            whenever(networkRepository.isOnWifi()) doReturn true
            assertThat(underTest(file)).isNull()
            verifyNoInteractions(fileSystemRepository)
        }

    @Test
    fun `test that image is scaled when quality settings is Automatic and there is no WiFi connection`() =
        runTest {
            val file = File("img.jpg")
            whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                    flowOf(ChatImageQuality.Automatic)
            whenever(networkRepository.isOnWifi()) doReturn false
            stubDestination()
            assertThat(underTest(file)).isNotNull()
        }

    @Test
    fun `test that image is scaled when quality settings is Optimised`() =
        runTest {
            val file = File("img.jpg")
            whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                    flowOf(ChatImageQuality.Optimised)
            stubDestination()
            assertThat(underTest(file)).isNotNull()
        }

    @Test
    fun `test that scaled image from repository is returned when needs to be scaled`() =
        runTest {
            val file = File("img.jpg")
            whenever(defaultSettingsRepository.getChatImageQuality()) doReturn
                    flowOf(ChatImageQuality.Optimised)
            val expected = stubDestination()
            val actual = underTest(file)
            assertThat(actual).isEqualTo(expected)
            verify(fileSystemRepository).downscaleImage(file, expected, DOWNSCALE_IMAGES_PX)
        }

    private suspend fun stubDestination(): File {
        val destination = mock<File> {
            on { it.name } doReturn "destination"
            on { it.exists() } doReturn true
        }
        whenever(getCacheFileForChatUploadUseCase(any())) doReturn destination
        return destination
    }
}