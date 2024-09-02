package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCacheFileForUploadUseCaseTest {
    private lateinit var underTest: GetCacheFileForUploadUseCase

    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val cacheRepository = mock<CacheRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetCacheFileForUploadUseCase(
            getCacheFileUseCase,
            cacheRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            getCacheFileUseCase,
            cacheRepository,
        )

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that file with the same name is returned when it does not exist`(
        isChatUpload: Boolean,
    ) = runTest {
        val file = stubFile()
        val expected = stubResult()
        val folder = getCacheFolder(isChatUpload)
        whenever(getCacheFileUseCase(folder, file.name)) doReturn expected
        val actual = underTest(file, isChatUpload)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that folder with the same name is returned when it does not exist`(
        isChatUpload: Boolean,
    ) = runTest {
        val folder = stubFolder()
        val expected = stubResult()
        val cacheFolder = getCacheFolder(isChatUpload)
        whenever(getCacheFileUseCase(cacheFolder, folder.name)) doReturn expected
        val actual = underTest(folder, isChatUpload)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that suffix is added when file already exists`(
        isChatUpload: Boolean,
    ) = runTest {
        val file = stubFile()
        val expected = stubResult()
        val alreadyExists = stubResult(true)
        val folder = getCacheFolder(isChatUpload)
        whenever(getCacheFileUseCase(folder, file.name)) doReturn alreadyExists
        whenever(
            getCacheFileUseCase(folder, file.nameWithSuffix(1))
        ) doReturn expected
        val actual = underTest(file, isChatUpload)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that suffix is added when folder already exists`(
        isChatUpload: Boolean,
    ) = runTest {
        val folder = stubFolder()
        val expected = stubResult()
        val alreadyExists = stubResult(true)
        val cacheFolder = getCacheFolder(isChatUpload)
        whenever(getCacheFileUseCase(cacheFolder, folder.name)) doReturn alreadyExists
        whenever(
            getCacheFileUseCase(cacheFolder, "${folder.name}_1")
        ) doReturn expected
        val actual = underTest(folder, isChatUpload)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when 1 to 99 suffix is not enough`(
        isChatUpload: Boolean,
    ) = runTest {
        val file = stubFile()
        val alreadyExists = stubResult(true)
        val folder = getCacheFolder(isChatUpload)
        whenever(getCacheFileUseCase(folder, file.name)) doReturn alreadyExists
        (1..99).forEach {
            whenever(
                getCacheFileUseCase(folder, file.nameWithSuffix(it))
            ) doReturn alreadyExists
        }
        val actual = underTest(file, isChatUpload)
        assertThat(actual).isNull()
    }

    private fun stubFile() = mock<File> {
        on { it.name } doReturn "name.ext"
    }

    private fun stubFolder() = mock<File> {
        on { it.name } doReturn "folder"
    }

    private fun stubResult(exists: Boolean = false) = mock<File> {
        on { it.exists() } doReturn exists
    }

    private fun File.nameWithSuffix(suffix: Int) =
        "${this.nameWithoutExtension}_$suffix.${this.extension}"

    private fun getCacheFolder(isChatUpload: Boolean) =
        (if (isChatUpload) "chatFolder" else "noChatFolder").also {
            whenever(cacheRepository.getCacheFolderNameForUpload(isChatUpload)) doReturn it
        }
}