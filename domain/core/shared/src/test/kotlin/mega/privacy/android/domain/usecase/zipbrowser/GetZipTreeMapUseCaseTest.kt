package mega.privacy.android.domain.usecase.zipbrowser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.repository.ZipBrowserRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.zip.ZipFile

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetZipTreeMapUseCaseTest {
    private lateinit var underTest: GetZipTreeMapUseCase
    private val zipBrowserRepository = mock<ZipBrowserRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetZipTreeMapUseCase(zipBrowserRepository = zipBrowserRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(zipBrowserRepository)
    }

    @Test
    fun `test that result is empty`() =
        runTest {
            whenever(zipBrowserRepository.getZipNodeTree(anyOrNull())).thenReturn(emptyMap())
            assertThat(underTest(mock())).isEmpty()
        }

    @Test
    fun `test that the ZipNodeTree is returned`() =
        runTest {
            val key = "The key of zip node tree"
            val testZipNodeTree = mock<ZipTreeNode>()
            val testMap = mapOf(key to testZipNodeTree)
            whenever(zipBrowserRepository.getZipNodeTree(anyOrNull())).thenReturn(testMap)

            val actual = underTest(mock())
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys.toList()[0]).isEqualTo(key)
            assertThat(actual[key]).isEqualTo(testZipNodeTree)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            val zipFile = mock<ZipFile>()
            underTest(zipFile)
            verify(zipBrowserRepository).getZipNodeTree(zipFile)
        }
}