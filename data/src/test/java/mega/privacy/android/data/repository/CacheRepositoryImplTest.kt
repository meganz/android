package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheRepositoryImplTest {
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val cacheFolderGateway: CacheFolderGateway = mock()
    private val underTest = CacheRepositoryImpl(cacheFolderGateway, ioDispatcher)

    @BeforeEach
    fun resetMocks() {
        reset(cacheFolderGateway)
    }

    @Test
    fun `test that cache size is same as expected and actual`() = runTest {
        val expected = 1234L
        whenever(cacheFolderGateway.getCacheSize()).thenReturn(expected)
        val actual = underTest.getCacheSize()
        verify(cacheFolderGateway).getCacheSize()
        assertEquals(expected, actual)
    }

    @Test
    fun `test that gateway method is invoked when clear cache is invoked`() = runTest {
        underTest.clearCache()
        verify(cacheFolderGateway).clearCache()
    }

    @Test
    fun `test that cache file is same as expected and actual`() {
        val folderName = "folder"
        val fileName = "file"
        val expected = mock<File>()
        whenever(cacheFolderGateway.getCacheFile(folderName, fileName)).thenReturn(expected)
        val actual = underTest.getCacheFile(folderName, fileName)
        verify(cacheFolderGateway).getCacheFile(folderName, fileName)
        assertEquals(expected, actual)
    }

    @Test
    fun `test that file preview path is same as expected and actual`() = runTest {
        val fileName = "file"
        val expected = File("path")
        whenever(cacheFolderGateway.getPreviewFile(fileName)).thenReturn(expected)
        val actual = underTest.getPreviewFile(fileName)
        verify(cacheFolderGateway).getPreviewFile(fileName)
        assertEquals(expected, actual)
    }

    @Test
    fun `test that preview download path for node is same as expected and actual`() = runTest {
        val expected = "path"
        whenever(cacheFolderGateway.getPreviewDownloadPathForNode()).thenReturn(expected)
        val actual = underTest.getPreviewDownloadPathForNode()
        verify(cacheFolderGateway).getPreviewDownloadPathForNode()
        assertEquals(expected, actual)
    }

    @Test
    fun `test that cache folder is returned from gateway`() = runTest {
        val expected = mock<File>()
        val folderName = "folderName"
        whenever(cacheFolderGateway.getCacheFolderAsync(folderName)) doReturn expected

        val actual = underTest.getCacheFolder((folderName))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that is returned from gateway`(expected: Boolean) = runTest {
        val file = mock<File>()
        whenever(cacheFolderGateway.isFileInCacheDirectory(file)) doReturn expected
        val actual = underTest.isFileInCacheDirectory(file)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that correct name is returned for cache folder for uploads`() = runTest {
        val actual = underTest.getCacheFolderNameForTransfer(false)
        assertThat(actual).isEqualTo(CacheFolderConstant.TEMPORARY_FOLDER)
    }

    @Test
    fun `test that correct name is returned for cache folder for chat uploads`() = runTest {
        val actual = underTest.getCacheFolderNameForTransfer(true)
        assertThat(actual).isEqualTo(CacheFolderConstant.CHAT_TEMPORARY_FOLDER)
    }
}