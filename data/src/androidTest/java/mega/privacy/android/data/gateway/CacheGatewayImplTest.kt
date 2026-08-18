package mega.privacy.android.data.gateway

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.BatteryInfo
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class CacheGatewayImplTest {
    private lateinit var underTest: CacheGateway
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        deleteCache()
        underTest = CacheGatewayImpl(
            context = context,
            ioDispatcher = UnconfinedTestDispatcher(),
            deviceGateway = FakeDeviceGateway(),
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
            val folderName = CacheFolderConstant.CHAT_TEMPORARY_FOLDER
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
            val folderName = CacheFolderConstant.CHAT_TEMPORARY_FOLDER
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

    /**
     * Test that clearCacheDirectory remove all of its contents
     */
    @Test
    fun test_that_clearCacheDirectory_remove_all_contents_inside_cache_directory() = runTest {
        (0..10).map {
            val random = (0..5).random()
            val file = File(context.cacheDir, it.toString())
            if (random % 2 == 0) {
                val folder = createDirectory(file)
                (0..5).map { i ->
                    val fileInFolder = File(folder, i.toString())
                    createFile(fileInFolder)

                }
            } else {
                createFile(file)
            }
        }
        underTest.clearCacheDirectory()
        val actual = 0
        val expected = context.cacheDir.listFiles()?.size
        assertThat(actual).isEqualTo(expected)
    }

    /**
     * Test that getThumbnailCacheFolderPath returns the Thumbnail Cache Folder path
     * when it exists in cache
     */
    @Test
    fun test_that_getThumbnailCacheFolderPath_returns_the_folder_path_if_exist() = runTest {
        val folderName = CacheFolderConstant.THUMBNAIL_FOLDER
        val expected = File(context.cacheDir, folderName)
        createDirectory(expected)

        assertThat(expected.exists()).isEqualTo(true)
        assertThat(underTest.getThumbnailCacheFolderPath()).isEqualTo(expected.path)
    }

    /**
     * Test that getThumbnailCacheFolderPath returns the Thumbnail Cache Folder path
     * and creates it in cache if the folder does not exist
     */
    @Test
    fun test_that_getThumbnailCacheFolderPath_creates_the_folder_in_cacheDir_if_not_exist_and_return_its_path() =
        runTest {
            val folderName = CacheFolderConstant.THUMBNAIL_FOLDER
            val expected = File(context.cacheDir, folderName)

            assertThat(expected.exists()).isEqualTo(false)
            assertThat(underTest.getThumbnailCacheFolderPath()).isEqualTo(expected.path)
            assertThat(expected.exists()).isEqualTo(true)
        }


    /**
     * Test that getPreviewCacheFolderPath returns the Preview Cache Folder path
     * when it exists in cache
     */
    @Test
    fun test_that_getPreviewCacheFolderPath_returns_the_folder_path_if_exist() = runTest {
        val folderName = CacheFolderConstant.PREVIEW_FOLDER
        val expected = File(context.cacheDir, folderName)
        createDirectory(expected)

        assertThat(expected.exists()).isEqualTo(true)
        assertThat(underTest.getPreviewCacheFolderPath()).isEqualTo(expected.path)
    }

    /**
     * Test that getPreviewCacheFolderPath returns the Preview Cache Folder path
     * and creates it in cache if the folder does not exist
     */
    @Test
    fun test_that_getPreviewCacheFolderPath_creates_the_folder_in_cacheDir_if_not_exist_and_return_its_path() =
        runTest {
            val folderName = CacheFolderConstant.PREVIEW_FOLDER
            val expected = File(context.cacheDir, folderName)

            assertThat(expected.exists()).isEqualTo(false)
            assertThat(underTest.getPreviewCacheFolderPath()).isEqualTo(expected.path)
            assertThat(expected.exists()).isEqualTo(true)
        }


    /**
     * Test that getFullSizeCacheFolderPath returns the Full Size Cache Folder path
     * when it exists in cache
     */
    @Test
    fun test_that_getFullSizeCacheFolderPath_returns_the_folder_path_if_exist() = runTest {
        val folderName = CacheFolderConstant.TEMPORARY_FOLDER
        val expected = File(context.cacheDir, folderName)
        createDirectory(expected)

        assertThat(expected.exists()).isEqualTo(true)
        assertThat(underTest.getFullSizeCacheFolderPath()).isEqualTo(expected.path)
    }

    /**
     * Test that getFullSizeCacheFolderPath returns the Full Size Cache Folder path
     * and creates it in cache if the folder does not exist
     */
    @Test
    fun test_that_getFullSizeCacheFolderPath_creates_the_folder_in_cacheDir_if_not_exist_and_return_its_path() =
        runTest {
            val folderName = CacheFolderConstant.TEMPORARY_FOLDER
            val expected = File(context.cacheDir, folderName)

            assertThat(expected.exists()).isEqualTo(false)
            assertThat(underTest.getFullSizeCacheFolderPath()).isEqualTo(expected.path)
            assertThat(expected.exists()).isEqualTo(true)
        }

    @Test
    fun test_that_passphrase_still_exist_after_calling_clearAppData() = runTest {
        val passphraseFile = File(context.filesDir, MegaDatabaseConstant.PASSPHRASE_FILE_NAME)
        createFile(passphraseFile)
        assertThat(passphraseFile.exists()).isEqualTo(true)
        underTest.clearAppData(setOf(MegaDatabaseConstant.PASSPHRASE_FILE_NAME))
        assertThat(passphraseFile.exists()).isEqualTo(true)
    }

    /**
     * [CacheGatewayImpl] only relies on [DeviceGateway.nanoTime]; every other member is
     * irrelevant to these filesystem tests and is left unimplemented.
     */
    private class FakeDeviceGateway : DeviceGateway {
        override val nanoTime: Long get() = System.nanoTime()

        override fun getManufacturerName(): String = unsupported()
        override fun getDeviceModel(): String = unsupported()
        override fun getDeviceName(): String = unsupported()
        override fun getCurrentDeviceLanguage(): String = unsupported()
        override fun getSdkVersionInt(): Int = unsupported()
        override fun getSdkVersionName(): String = unsupported()
        override fun getCurrentTimeInMillis(): Long = unsupported()
        override fun getElapsedRealtime(): Long = unsupported()
        override suspend fun getDeviceMemory(): Long? = unsupported()
        override suspend fun getDiskSpaceBytes(path: String): Long = unsupported()
        override fun is24HourFormat(): Boolean = unsupported()
        override val now: Long get() = unsupported()
        override suspend fun getLocalIpAddress(): String? = unsupported()
        override fun getCurrentHourOfDay(): Int = unsupported()
        override fun getCurrentMinute(): Int = unsupported()
        override fun getBatteryInfo(intent: Intent?): BatteryInfo = unsupported()
        override val monitorBatteryInfo: Flow<BatteryInfo> get() = unsupported()
        override val monitorDevicePowerConnectionState: Flow<String?> get() = unsupported()
        override fun isInPowerSaveMode(): Boolean = unsupported()
        override val monitorPowerSaveState: Flow<Boolean> get() = unsupported()
        override val monitorThermalState: Flow<Int> get() = unsupported()
        override fun getAvailableProcessors(): Int = unsupported()
        override fun getTimezone(): String = unsupported()
        override fun getCurrentTimezoneOffset(): String = unsupported()

        private fun unsupported(): Nothing =
            throw UnsupportedOperationException("Not used by CacheGatewayImpl")
    }
}