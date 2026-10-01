package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ZipBrowserRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.zip.ZipFile

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnzipFileUseCaseTest {
    private lateinit var underTest: UnzipFileUseCase
    private val zipBrowserRepository = mock<ZipBrowserRepository>()
    private val testZipFile = mock<ZipFile>()
    private val testUnzipRootPath = "UnzipRootPath"

    @BeforeAll
    fun setUp() {
        underTest = UnzipFileUseCase(zipBrowserRepository = zipBrowserRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(zipBrowserRepository)
    }

    @Test
    fun `test that result is false when parameters are null`() =
        runTest {
            whenever(zipBrowserRepository.unzipFile(anyOrNull(), anyOrNull())).thenReturn(false)
            assertThat(underTest(null, null)).isFalse()
        }

    @Test
    fun `test that result is false when parameters are not null`() =
        runTest {
            whenever(zipBrowserRepository.unzipFile(testZipFile, testUnzipRootPath)).thenReturn(
                false
            )
            assertThat(underTest(testZipFile, testUnzipRootPath)).isFalse()
        }

    @Test
    fun `test that result is true`() =
        runTest {
            whenever(
                zipBrowserRepository.unzipFile(
                    testZipFile,
                    testUnzipRootPath
                )
            ).thenReturn(true)
            assertThat(underTest(testZipFile, testUnzipRootPath)).isTrue()
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testZipFile, testUnzipRootPath)
            verify(zipBrowserRepository).unzipFile(testZipFile, testUnzipRootPath)
        }

    @Test
    fun `test that the repository function is not called when zipFile is null`() =
        runTest {
            underTest(null, testUnzipRootPath)
            verifyNoInteractions(zipBrowserRepository)
        }

    @Test
    fun `test that the repository function is not called when unzipRootPath is null`() =
        runTest {
            underTest(testZipFile, null)
            verifyNoInteractions(zipBrowserRepository)
        }

    @Test
    fun `test that the repository function is not called when both zipFile and unzipRootPath are null`() =
        runTest {
            underTest(null, null)
            verifyNoInteractions(zipBrowserRepository)
        }
}