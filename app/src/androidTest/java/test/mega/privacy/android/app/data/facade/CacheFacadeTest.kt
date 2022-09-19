package test.mega.privacy.android.app.data.facade

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.facade.CacheFacade
import mega.privacy.android.app.data.gateway.CacheGateway
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class CacheFacadeTest {
    private lateinit var underTest: CacheGateway
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        deleteCache()
        underTest = CacheFacade(
            context = context,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        deleteCache()
    }

    /**
     * Create a directory
     *
     * @param file the File to create
     * @return the file created, null if the file could not be created
     */
    private fun createDirectory(file: File): File? {
        return if (file.mkdir()) {
            file
        } else {
            fail("The directory could not be created")
            null
        }
    }

    /**
     * Create a file
     *
     * @param file the File to create
     * @return the file created, null if the file could not be created
     */
    private fun createFile(file: File): File? {
        return if (file.createNewFile()) {
            file
        } else {
            fail("The file could not be created")
            null
        }
    }

    /**
     * Delete the cache
     */
    private fun deleteCache() {
        context.cacheDir.deleteRecursively()
        context.filesDir.deleteRecursively()
    }

    /**
     * Test that getOrCreateCacheFolder returns the file given in parameter
     * when the file exists in cache
     */
    @Test
    fun test_that_getOrCreateCacheFolder_returns_the_file_if_exist() = runTest {
        val folderName = "test"
        val expected = File(context.cacheDir, folderName)
        createDirectory(expected)

        assertThat(expected.exists()).isEqualTo(true)
        assertThat(underTest.getOrCreateCacheFolder(folderName)).isEqualTo(expected)
    }

    /**
     * Test that getOrCreateCacheFolder returns the file given in parameter
     * and create it in cache if the file does not exist
     */
    @Test
    fun test_that_getOrCreateCacheFolder_creates_the_file_in_cacheDir_if_not_exist_and_return_the_file() =
        runTest {
            val folderName = "test"
            val expected = File(context.cacheDir, folderName)

            assertThat(expected.exists()).isEqualTo(false)
            assertThat(underTest.getOrCreateCacheFolder(folderName)).isEqualTo(expected)
            assertThat(expected.exists()).isEqualTo(true)
        }

    /**
     * Test that getOrCreateChatCacheFolder returns the chatTempMEGA file
     * when the file exist in cache
     */
    @Test
    fun test_that_getOrCreateChatCacheFolder_returns_the_file_CHAT_TEMPORARY_FOLDER_in_filesDir_if_exist() =
        runTest {
            val folderName = "chatTempMEGA"
            val expected = File(context.filesDir, folderName)
            createDirectory(expected)

            assertThat(expected.exists()).isEqualTo(true)
            assertThat(underTest.getOrCreateChatCacheFolder()).isEqualTo(expected)
        }

    /**
     * Test that getOrCreateChatCacheFolder returns the chatTempMEGA file
     * and create it in cache if the file does not exist
     */
    @Test
    fun test_that_getOrCreateChatCacheFolder_creates_the_file_CHAT_TEMPORARY_FOLDER_in_filesDir_if_not_exist_and_return_the_file() =
        runTest {
            val folderName = "chatTempMEGA"
            val expected = File(context.filesDir, folderName)

            assertThat(expected.exists()).isEqualTo(false)
            assertThat(underTest.getOrCreateChatCacheFolder()).isEqualTo(expected)
            assertThat(expected.exists()).isEqualTo(true)
        }

    /**
     * Test that getCacheFile returns the file given in parameter
     * It does not matter if the file exists or not in cache
     */
    @Test
    fun test_that_getCacheFile_returns_the_file_under_folder_name_in_cacheDir() = runTest {
        val fileName = "fileName"
        val folderName = "folderName"
        val directoryFile = File(context.cacheDir, folderName)
        createDirectory(directoryFile)
        val expected = File(directoryFile, fileName)
        createFile(expected)

        assertThat(underTest.getCacheFile(folderName, fileName)).isEqualTo(expected)
    }
}