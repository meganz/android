package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileExtensionFromUriPathUseCase
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCacheFileForChatUploadUseCaseTest {
    private lateinit var underTest: GetCacheFileForChatUploadUseCase

    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val cacheRepository = mock<CacheRepository>()
    private val getFileExtensionFromUriPathUseCase = mock<GetFileExtensionFromUriPathUseCase>()
    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetCacheFileForChatUploadUseCase(
            getCacheFileUseCase,
            cacheRepository,
            getFileExtensionFromUriPathUseCase,
            getFileNameFromStringUriUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            getCacheFileUseCase,
            cacheRepository,
        )
        whenever(getFileExtensionFromUriPathUseCase(anyValueClass())) doReturn EXTENSION
    }

    @Test
    fun `test that file with the same name is returned when it does not exist`() = runTest {
        val file = stubFile()
        val expected = stubResult()
        val folder = getCacheFolder()
        whenever(getCacheFileUseCase(folder, FILE_NAME)) doReturn expected
        val actual = underTest(file)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that folder with the same name is returned when it does not exist`() = runTest {
        val folder = stubFolder()
        val expected = stubResult()
        val cacheFolder = getCacheFolder()
        whenever(getCacheFileUseCase(cacheFolder, FOLDER_NAME)) doReturn expected
        val actual = underTest(folder)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that suffix is added when file already exists`() = runTest {
        val file = stubFile()
        val expected = stubResult()
        val alreadyExists = stubResult(true)
        val folder = getCacheFolder()
        whenever(getCacheFileUseCase(folder, FILE_NAME)) doReturn alreadyExists
        whenever(
            getCacheFileUseCase(folder, FILE_NAME.nameWithSuffix(1))
        ) doReturn expected
        val actual = underTest(file)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that suffix is added when folder already exists`() = runTest {
        val folder = stubFolder()
        val expected = stubResult()
        val alreadyExists = stubResult(true)
        val cacheFolder = getCacheFolder()
        whenever(getCacheFileUseCase(cacheFolder, FOLDER_NAME)) doReturn alreadyExists
        whenever(
            getCacheFileUseCase(cacheFolder, "${FOLDER_NAME}_1")
        ) doReturn expected
        val actual = underTest(folder)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that null is returned when 1 to 99 suffix is not enough`() = runTest {
        val file = stubFile()
        val alreadyExists = stubResult(true)
        val folder = getCacheFolder()
        whenever(getCacheFileUseCase(folder, FILE_NAME)) doReturn alreadyExists
        (1..99).forEach {
            whenever(
                getCacheFileUseCase(folder, FILE_NAME.nameWithSuffix(it))
            ) doReturn alreadyExists
        }
        val actual = underTest(file)
        assertThat(actual).isNull()
    }

    private suspend fun stubFile() = UriPath("content://$FILE_NAME").also {
        whenever(getFileNameFromStringUriUseCase(it.value)) doReturn FILE_NAME
        whenever(getFileExtensionFromUriPathUseCase(it)) doReturn EXTENSION
    }

    private suspend fun stubFolder() = UriPath("content://$FOLDER_NAME").also {
        whenever(getFileNameFromStringUriUseCase(it.value)) doReturn FOLDER_NAME
        whenever(getFileExtensionFromUriPathUseCase(it)) doReturn ""
    }

    private fun stubResult(exists: Boolean = false) = mock<File> {
        on { it.exists() } doReturn exists
    }

    private fun String.nameWithSuffix(suffix: Int) =
        "${this.substringBeforeLast(".")}_$suffix.${this.substringAfterLast(".")}"

    private fun getCacheFolder() =
        "chatFolder".also {
            whenever(cacheRepository.getCacheFolderNameForTransfer(true)) doReturn it
        }

    private companion object {
        private const val EXTENSION = "ext"
        private const val FILE_NAME = "name.$EXTENSION"
        private const val FOLDER_NAME = "folder"
    }
}