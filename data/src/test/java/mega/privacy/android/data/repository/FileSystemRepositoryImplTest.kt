package mega.privacy.android.data.repository

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertFailsWith

/**
 * Test class for [FileSystemRepositoryImpl]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileSystemRepositoryImplTest {
    private lateinit var underTest: FileSystemRepository

    private val context: Context = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaApiFolderGateway: MegaApiFolderGateway = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val shareDataMapper: ShareDataMapper = mock()
    private val megaExceptionMapper: MegaExceptionMapper = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val cacheGateway: CacheGateway = mock()
    private val nodeMapper: NodeMapper = mock()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper = mock()
    private val fileGateway: FileGateway = mock()
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper = mock()
    private val fileVersionsOptionCache: Cache<Boolean> = mock()
    private val streamingGateway = mock<StreamingGateway>()
    private val deviceGateway = mock<DeviceGateway>()
    private val sdCardGateway = mock<SDCardGateway>()
    private val fileAttributeGateway = mock<FileAttributeGateway>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(megaApiGateway.globalUpdates).thenReturn(emptyFlow())
        initUnderTest()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initUnderTest() {
        underTest = FileSystemRepositoryImpl(
            context = context,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = ioDispatcher,
            megaLocalStorageGateway = megaLocalStorageGateway,
            shareDataMapper = shareDataMapper,
            megaExceptionMapper = megaExceptionMapper,
            sortOrderIntMapper = sortOrderIntMapper,
            cacheGateway = cacheGateway,
            nodeMapper = nodeMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            offlineNodeInformationMapper = offlineNodeInformationMapper,
            fileGateway = fileGateway,
            chatFilesFolderUserAttributeMapper = chatFilesFolderUserAttributeMapper,
            fileVersionsOptionCache = fileVersionsOptionCache,
            streamingGateway = streamingGateway,
            deviceGateway = deviceGateway,
            sdCardGateway = sdCardGateway,
            fileAttributeGateway = fileAttributeGateway,
            sharingScope = TestScope(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            megaApiGateway,
            megaApiFolderGateway,
            megaChatApiGateway,
            megaLocalStorageGateway,
            shareDataMapper,
            megaExceptionMapper,
            sortOrderIntMapper,
            cacheGateway,
            nodeMapper,
            fileTypeInfoMapper,
            offlineNodeInformationMapper,
            fileGateway,
            chatFilesFolderUserAttributeMapper,
            fileVersionsOptionCache,
            streamingGateway,
            deviceGateway,
            sdCardGateway,
            fileAttributeGateway,
        )
    }

    @Test
    fun `test that the local DCIM folder path is retrieved`() = runTest {
        val testPath = "test/local/dcim/path"

        whenever(fileGateway.localDCIMFolderPath).thenReturn(testPath)
        assertThat(underTest.localDCIMFolderPath).isEqualTo(testPath)
    }

    @Test
    fun `test that data return from cache when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption)
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(0)).set(any())
            verify(megaApiGateway, times(0)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh true`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption.not())
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(true)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(null)
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @Test
    fun `test that local file url string is returned if node exists`() = runTest {
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
        val expected = "expectedUrl"
        whenever(streamingGateway.getLocalLink(any())).thenReturn(expected)

        val actual = underTest.getFileStreamingUri(mock { on { id }.thenReturn(NodeId(1L)) })

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that temporary file is created successfully`() = runTest {
        val localPath = "/path/to/local"
        val newPath = "/path/to/new"
        val rootPath = "/path/to/root"
        whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenReturn(Unit)
        val actual = underTest.createTempFile(rootPath, localPath, newPath)
        assertThat(actual).isEqualTo(newPath)
    }

    @Test
    fun `test that not enough storage exception is thrown when there is not enough storage`() =
        runTest {
            val localPath = "/path/to/local"
            val newPath = "/path/to/new"
            val rootPath = "/path/to/root"
            whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenThrow(
                NotEnoughStorageException()
            )
            assertFailsWith(
                exceptionClass = NotEnoughStorageException::class,
                block = { underTest.createTempFile(rootPath, localPath, newPath) }
            )
        }

    @Test
    fun `test that file not created exception is thrown when file creation is not successful`() =
        runTest {
            val localPath = "/path/to/local"
            val newPath = "/path/to/new"
            val rootPath = "/path/to/root"
            whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenThrow(
                FileNotCreatedException()
            )
            assertFailsWith(
                exceptionClass = FileNotCreatedException::class,
                block = { underTest.createTempFile(rootPath, localPath, newPath) }
            )
        }

    @ParameterizedTest(name = "folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the folder could exist`(folderExists: Boolean) = runTest {
        whenever(fileGateway.isFileAvailable(fileString = any())).thenReturn(folderExists)
        assertThat(underTest.doesFolderExists("test/folder")).isEqualTo(folderExists)
    }

    @ParameterizedTest(name = "folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the folder in the SD Card exists`(folderExists: Boolean) = runTest {
        whenever(sdCardGateway.getDirectoryFile(any())).thenReturn(mock())
        whenever(fileGateway.isDocumentFileAvailable(any())).thenReturn(folderExists)
        assertThat(underTest.isFolderInSDCardAvailable("test/folder/path")).isEqualTo(folderExists)
    }

    @ParameterizedTest(name = "external directory exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the external storage directory could exist`(externalDirExists: Boolean) =
        runTest {
            whenever(fileGateway.doesExternalStorageDirectoryExists()).thenReturn(externalDirExists)
            assertThat(underTest.doesExternalStorageDirectoryExists()).isEqualTo(externalDirExists)
        }

    @ParameterizedTest(name = "delete root directory: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the camera uploads temporary root directory could be deleted`(deleteRootDirectory: Boolean) =
        runTest {
            whenever(cacheGateway.getCameraUploadsCacheFolder()).thenReturn(mock())
            whenever(fileGateway.deleteDirectory(any())).thenReturn(deleteRootDirectory)

            assertThat(underTest.deleteCameraUploadsTemporaryRootDirectory()).isEqualTo(
                deleteRootDirectory
            )
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is API_ENOENT and call getFileVersionsOption with forceRefresh true`() =
        runTest {
            val expectedFileVersionsOption = false
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(null)
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(true)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertThat(expectedFileVersionsOption).isEqualTo(actual)
        }

    @ParameterizedTest(name = " megaApi call returns {0}")
    @NullAndEmptySource
    @ValueSource(strings = ["testName"])
    fun `test that escapeFsIncompatible returns correctly if`(
        fileName: String?,
    ) = runTest {
        whenever(megaApiGateway.escapeFsIncompatible(any(), any())).thenReturn(fileName)
        assertThat(underTest.escapeFsIncompatible("file name", "dest/path")).isEqualTo(fileName)
    }


    @Nested
    @DisplayName("GPS Coordinates")
    inner class GPSCoordinatesTest {
        @Test
        fun `test that the video GPS coordinates are retrieved`() = runTest {
            val testCoordinates = Pair(6.0, 9.0)

            whenever(fileAttributeGateway.getVideoGPSCoordinates(any())).thenReturn(testCoordinates)
            assertThat(underTest.getVideoGPSCoordinates("")).isEqualTo(testCoordinates)
        }

        @Test
        fun `test that the photo GPS coordinates are retrieved`() {
            runTest {
                val testCoordinates = Pair(6.0, 9.0)

                whenever(fileAttributeGateway.getPhotoGPSCoordinates(any())).thenReturn(
                    testCoordinates
                )
                assertThat(underTest.getPhotoGPSCoordinates("")).isEqualTo(testCoordinates)
            }
        }
    }

    @Test
    fun `test that create new image uri returns correct value`() =
        runTest {
            val uri = mock<Uri> {
                on { toString() } doReturn "uri"
            }
            whenever(fileGateway.createNewImageUri(any())).thenReturn(uri)
            assertThat(underTest.createNewImageUri("name")).isEqualTo("uri")
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isContentUri returns correct value`(
        expected: Boolean,
    ) = runTest {
        val uri = "uri//:example.txt"
        whenever(fileGateway.isContentUri(uri)).thenReturn(expected)
        assertThat(underTest.isContentUri(uri)).isEqualTo(expected)
    }

    @Test
    fun `test that getFileNameFromUri returns correct value`() = runTest {
        val uri = "uri//:example.txt"
        val expected = "example"
        whenever(fileGateway.getFileNameFromUri(any())).thenReturn(expected)
        assertThat(underTest.getFileNameFromUri(uri)).isEqualTo(expected)
    }

    @Test
    fun `test that getFileExtensionFromUri returns correct value`() = runTest {
        val uri = "uri//:example.txt"
        val expected = "txt"
        whenever(fileGateway.getFileExtensionFromUri(any())).thenReturn(expected)
        assertThat(underTest.getFileExtensionFromUri(uri)).isEqualTo(expected)
    }

    @Test
    fun `test that copyContentUriToFile calls gateway method`() = runTest {
        val uri = "uri//:example.txt"
        val file = mock<File>()
        underTest.copyContentUriToFile(uri, file)
        verify(fileGateway).copyContentUriToFile(uri, file)
    }

    @Nested
    @DisplayName("SD Card related methods")
    inner class SDCard {
        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that isSDCardPath returns gateway value`(expected: Boolean) = runTest {
            whenever(sdCardGateway.doesFolderExists(any())).thenReturn(expected)
            assertThat(underTest.isSDCardPath("something")).isEqualTo(expected)
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that isSDCardCachePath returns gateway value`(expected: Boolean) = runTest {
            whenever(sdCardGateway.isSDCardCachePath(any())).thenReturn(expected)
            assertThat(underTest.isSDCardCachePath("something")).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("My chats files folder")
    inner class MyChatsFilesFolder {

        private val globalUpdatesFlow = MutableSharedFlow<GlobalUpdate>()

        @BeforeEach
        fun resetCache() {
            whenever(megaApiGateway.globalUpdates).thenReturn(globalUpdatesFlow)
            initUnderTest()
        }

        @Test
        fun `test that my chats files folder id is retrieved from the gateway if not set`() = runTest {
            val handle = 11L
            stubGetMyChatFilesFolder(handle)
            val actual = underTest.getMyChatsFilesFolderId()
            assertThat(actual?.longValue).isEqualTo(handle)
        }

        @Test
        fun `test that my chats files folder id is cached`() = runTest {
            stubGetMyChatFilesFolder()
            underTest.getMyChatsFilesFolderId()
            verify(megaApiGateway).getMyChatFilesFolder(any())
            clearInvocations(megaApiGateway)
            underTest.getMyChatsFilesFolderId()
            verify(megaApiGateway, never()).getMyChatFilesFolder(any())
        }

        @Test
        fun `test that updates are monitored after my chats files folder id is set`() = runTest {
            val handle = 11L
            stubGetMyChatFilesFolder(handle + 1)
            val initial = underTest.getMyChatsFilesFolderId()
            assertThat(initial?.longValue).isNotEqualTo(handle)

            stubGetMyChatFilesFolder(handle)
            globalUpdatesFlow.emit(stubGlobalMyChatsFilesFolderUpdate())

            yield() // listening to global updates is in another scope, we need to yield to get the update
            val expected = underTest.getMyChatsFilesFolderId()
            assertThat(expected?.longValue).isEqualTo(handle)
        }

        private fun stubGetMyChatFilesFolder(folderHandle: Long = 1L) {
            val megaError = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
                on { errorString } doReturn ""
            }
            val megaRequest = mock<MegaRequest> {
                on { nodeHandle } doReturn folderHandle
            }
            whenever(megaApiGateway.getMyChatFilesFolder(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
        }

        private fun stubGlobalMyChatsFilesFolderUpdate(): GlobalUpdate.OnUsersUpdate {
            val userHandle = 77L
            val megaUser = mock<MegaUser> {
                on { this.handle } doReturn userHandle
                on { isOwnChange } doReturn 0
                on { this.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER.toLong()) } doReturn true
            }
            whenever(megaApiGateway.myUser).thenReturn(megaUser)
            return mock<GlobalUpdate.OnUsersUpdate> {
                on { users } doReturn arrayListOf(megaUser)
            }
        }
    }
}
