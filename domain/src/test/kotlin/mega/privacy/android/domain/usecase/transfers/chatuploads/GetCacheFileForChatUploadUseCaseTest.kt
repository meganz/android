package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetCacheFileForChatUploadUseCase.Companion.CHAT_TEMPORARY_FOLDER
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
class GetCacheFileForChatUploadUseCaseTest {
    private lateinit var underTest: GetCacheFileForChatUploadUseCase

    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetCacheFileForChatUploadUseCase(
            getCacheFileUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            getCacheFileUseCase,
        )

    @Test
    fun `test that file with the same name is returned when it does not exist`() = runTest {
        val file = stubFile()
        val expected = stubResult()
        whenever(getCacheFileUseCase(CHAT_TEMPORARY_FOLDER, file.name)) doReturn expected
        val actual = underTest(file)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that suffix is added when file already exists`() = runTest {
        val file = stubFile()
        val expected = stubResult()
        val alreadyExists = stubResult(true)
        whenever(getCacheFileUseCase(CHAT_TEMPORARY_FOLDER, file.name)) doReturn alreadyExists
        whenever(
            getCacheFileUseCase(CHAT_TEMPORARY_FOLDER, file.nameWithSuffix(1))
        ) doReturn expected
        val actual = underTest(file)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that null is returned when 1 to 99 suffix is not enough`() = runTest {
        val file = stubFile()
        val alreadyExists = stubResult(true)
        whenever(getCacheFileUseCase(CHAT_TEMPORARY_FOLDER, file.name)) doReturn alreadyExists
        (1..99).forEach {
            whenever(
                getCacheFileUseCase(CHAT_TEMPORARY_FOLDER, file.nameWithSuffix(it))
            ) doReturn alreadyExists
        }
        val actual = underTest(file)
        assertThat(actual).isNull()
    }

    private fun stubFile() = mock<File> {
        on { it.name } doReturn "name.ext"
    }

    private fun stubResult(exists: Boolean = false) = mock<File> {
        on { it.exists() } doReturn exists
    }

    private fun File.nameWithSuffix(suffix: Int) =
        "${this.nameWithoutExtension}_$suffix.${this.extension}"
}