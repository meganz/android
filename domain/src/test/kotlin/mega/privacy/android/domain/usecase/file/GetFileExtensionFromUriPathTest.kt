package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileExtensionFromUriPathTest {

    private lateinit var underTest: GetFileExtensionFromUriPath

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileExtensionFromUriPath(
            fileSystemRepository
        )
    }

    @Test
    fun `test that extension is returned from path when UriPath is a path`() = runTest {
        val expected = "txt"
        val uriPath = UriPath("/folder/file.$expected")

        val actual = underTest(uriPath)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that extension is returned from repository when UriPath is not a path`() = runTest {
        val expected = "txt"
        val uriPath = UriPath("content://folder/file.$expected")
        whenever(fileSystemRepository.getFileNameFromUri(uriPath.value)) doReturn "file.$expected"

        val actual = underTest(uriPath)

        assertThat(actual).isEqualTo(expected)
    }
}