package mega.privacy.android.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.document.DocumentMetadata
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

/**
 * Test class for [FileSystemRepositoryImpl]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileSystemRepositoryImplTest {
    private lateinit var underTest: FileSystemRepository
    private val context: Context = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val cacheGateway: CacheGateway = mock()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val fileGateway: FileGateway = mock()
    private val deviceGateway = mock<DeviceGateway>()
    private val sdCardGateway = mock<SDCardGateway>()
    private val fileAttributeGateway = mock<FileAttributeGateway>()
    private val mimeTypeMapper = mock<MimeTypeMapper>()
    private val documentFileWrapper = mock<DocumentFileWrapper>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initUnderTest()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initUnderTest() {
        underTest = FileSystemRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher,
            cacheGateway = cacheGateway,
            fileTypeInfoMapper = fileTypeInfoMapper,
            fileGateway = fileGateway,
            deviceGateway = deviceGateway,
            sdCardGateway = sdCardGateway,
            fileAttributeGateway = fileAttributeGateway,
            documentFileWrapper = documentFileWrapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            cacheGateway,
            fileTypeInfoMapper,
            fileGateway,
            deviceGateway,
            sdCardGateway,
            fileAttributeGateway,
            mimeTypeMapper,
            documentFileWrapper,
        )
    }

    @Test
    fun `test that the local DCIM folder path is retrieved`() = runTest {
        val testPath = "test/local/dcim/path"

        whenever(fileGateway.localDCIMFolderPath).thenReturn(testPath)
        assertThat(underTest.localDCIMFolderPath).isEqualTo(testPath)
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

    @Test
    fun `test that create new video uri returns correct value`() =
        runTest {
            val uri = mock<Uri> {
                on { toString() } doReturn "uri"
            }
            whenever(fileGateway.createNewVideoUri(any())).thenReturn(uri)
            assertThat(underTest.createNewVideoUri("name")).isEqualTo("uri")
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
    fun `test that getFileNameFromUri returns correct value from gateway`() = runTest {
        val uri = "uri//:example.txt"
        val expected = "example"
        whenever(fileGateway.getFileNameFromUri(any())).thenReturn(expected)
        assertThat(underTest.getFileNameFromUri(uri)).isEqualTo(expected)
    }

    @Test
    fun `test that getFileSizeFromUri returns correct value from gateway`() = runTest {
        val uri = "uri//:example.txt"
        val expected = 56534465L
        whenever(fileGateway.getFileSizeFromUri(any())).thenReturn(expected)
        assertThat(underTest.getFileSizeFromUri(uri)).isEqualTo(expected)
    }

    @Test
    fun `test that copyContentUriToFile calls gateway method`() = runTest {
        val uri = UriPath("uri//:example.txt")
        val file = mock<File>()
        underTest.copyContentUriToFile(uri, file)
        verify(fileGateway).copyContentUriToFile(uri, file)
    }

    @Nested
    @DisplayName("SD Card related methods")
    inner class SDCard {
        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that isSDCardPath returns correctly as per doesFolderExists gateway value`(
            expected: Boolean,
        ) = runTest {
            whenever(sdCardGateway.doesFolderExists(any())).thenReturn(expected)
            whenever(sdCardGateway.isSDCardUri(any())).thenReturn(false)
            assertThat(underTest.isSDCardPathOrUri("something")).isEqualTo(expected)
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that isSDCardPath returns correctly as per isSDCardUri gateway value`(
            expected: Boolean,
        ) = runTest {
            whenever(sdCardGateway.doesFolderExists(any())).thenReturn(false)
            whenever(sdCardGateway.isSDCardUri(any())).thenReturn(expected)
            assertThat(underTest.isSDCardPathOrUri("something")).isEqualTo(expected)
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that isSDCardCachePath returns gateway value`(expected: Boolean) = runTest {
            whenever(sdCardGateway.isSDCardCachePath(any())).thenReturn(expected)
            assertThat(underTest.isSDCardCachePath("something")).isEqualTo(expected)
        }

        @ParameterizedTest(name = "when file exists is: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that getFileLengthFromSdCardContentUri returns correctly`(fileExists: Boolean) =
            runTest {
                mockStatic(Uri::class.java).use { mockedUri ->
                    val fileContentUri = "test/path"
                    val uri = mock<Uri>()
                    val length = if (fileExists) 123L else 0L
                    val documentFile = if (fileExists) mock<DocumentFile> {
                        on { length() } doReturn 123L
                    } else null


                    mockedUri.`when`<Uri> { Uri.parse(fileContentUri) }.thenReturn(uri)
                    whenever(documentFileWrapper.fromSingleUri(uri)).thenReturn(documentFile)
                    assertThat(underTest.getFileLengthFromSdCardContentUri(fileContentUri))
                        .isEqualTo(length)
                }
            }

        @Test
        fun `test that getFileLengthFromSdCardContentUri returns 0L if uri is null`() = runTest {
            mockStatic(Uri::class.java).use { mockedUri ->
                val fileContentUri = "test/path"

                mockedUri.`when`<Uri> { Uri.parse(fileContentUri) }.thenReturn(null)
                verifyNoInteractions(documentFileWrapper)
                assertThat(underTest.getFileLengthFromSdCardContentUri(fileContentUri))
                    .isEqualTo(0L)
            }
        }

        @ParameterizedTest(name = "when file exists is: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that deleteFileFromSdCardContentUri returns correctly`(fileExists: Boolean) =
            runTest {
                mockStatic(Uri::class.java).use { mockedUri ->
                    val fileContentUri = "test/path"
                    val uri = mock<Uri>()
                    val documentFile = if (fileExists) mock<DocumentFile> {
                        on { delete() } doReturn true
                    } else null

                    mockedUri.`when`<Uri> { Uri.parse(fileContentUri) }.thenReturn(uri)
                    whenever(documentFileWrapper.fromSingleUri(uri)).thenReturn(documentFile)
                    assertThat(underTest.deleteFileFromSdCardContentUri(fileContentUri))
                        .isEqualTo(fileExists)
                }
            }

        @Test
        fun `test that deleteFileFromSdCardContentUri returns false if uri is null`() = runTest {
            mockStatic(Uri::class.java).use { mockedUri ->
                val fileContentUri = "test/path"

                mockedUri.`when`<Uri> { Uri.parse(fileContentUri) }.thenReturn(null)
                verifyNoInteractions(documentFileWrapper)
                assertThat(underTest.deleteFileFromSdCardContentUri(fileContentUri)).isFalse()
            }
        }
    }

    @ParameterizedTest(name = "delete voice clip: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that voice clip can be deleted`(deleteVoiceClip: Boolean) =
        runTest {
            whenever(cacheGateway.getVoiceClipFile(any())).thenReturn(mock())
            whenever(fileGateway.deleteFile(any())).thenReturn(deleteVoiceClip)
            assertThat(underTest.deleteVoiceClip("name")).isEqualTo(deleteVoiceClip)
        }

    @Test
    fun `test that fileTypeInfo gets the duration from file attribute gateway`() = runTest {
        val filePath = "path/video.mp4"
        val file = File(filePath)
        val duration = 4567.milliseconds
        whenever(mimeTypeMapper(any())).thenReturn("mime")
        whenever(fileAttributeGateway.getVideoDuration(file.absolutePath)) doReturn duration
        underTest.getFileTypeInfo(file)
        val argumentCaptor = argumentCaptor<String>()
        verify(fileAttributeGateway).getVideoDuration(argumentCaptor.capture())
        assertThat(argumentCaptor.firstValue).endsWith(filePath)
    }

    @Test
    fun `test that getLocalFile invokes gateway method`() = runTest {
        val file = mock<File>()
        val fileNode = mock<TypedFileNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "name"
            on { size } doReturn 123L
            on { modificationTime } doReturn 456L
        }
        whenever(
            fileGateway.getLocalFile(
                fileName = fileNode.name,
                fileSize = fileNode.size,
                lastModifiedDate = fileNode.modificationTime
            )
        ).thenReturn(file)
        assertThat(underTest.getLocalFile(fileNode)).isEqualTo(file)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isExternalStorageContentUri returns gateway result`(expected: Boolean) =
        runTest {
            whenever(fileGateway.isExternalStorageContentUri(any())).thenReturn(expected)

            val actual = underTest.isExternalStorageContentUri("someUri")

            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteFileByUri deletes the file correctly`(expected: Boolean) = runTest {
        Mockito.mockStatic(Uri::class.java).use { _ ->
            val testUri = "file://test/file/path"
            val uri = mock<Uri>()
            whenever(Uri.parse(testUri)).thenReturn(uri)
            whenever(fileGateway.deleteFileByUri(uri)).thenReturn(expected)

            val actual = underTest.deleteFileByUri(testUri)

            verify(fileGateway).deleteFileByUri(uri)
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that get file in document folder returns correct value`() = runTest {
        val folderUri = UriPath("file://test/file/path")
        val entity = mock<DocumentEntity>()
        whenever(fileGateway.getFilesInDocumentFolder(folderUri)).thenReturn(
            DocumentFolder(listOf(entity))
        )
        assertThat(underTest.getFilesInDocumentFolder(folderUri)).isEqualTo(
            DocumentFolder(listOf(entity))
        )
    }

    @Test
    fun `test that copy files invokes gateway method`() = runTest {
        val file = mock<File>()
        val destination = mock<File>()
        underTest.copyFiles(file, destination)
        verify(fileGateway).copyFileToFolder(file, destination)
    }

    @Test
    fun `test that getFileInfoType function returns the correct value`() = runTest {
        val name = "name"
        val expectedFileInfoType = UnMappedFileTypeInfo("")
        whenever(fileTypeInfoMapper(name)).thenReturn(expectedFileInfoType)
        assertThat(underTest.getFileTypeInfoByName(name)).isEqualTo(expectedFileInfoType)
    }

    @Test
    fun `test that isMalformedPathFromExternalApp method from file gateway is called when repository method is called`() =
        runTest {
            val action = "action"
            val path = "path"
            underTest.isMalformedPathFromExternalApp(action, path)
            verify(fileGateway).isMalformedPathFromExternalApp(action, path)
        }

    @Test
    fun `test that isPathInsecure method from file gateway is called when repository method is called`() =
        runTest {
            val path = "path"
            underTest.isPathInsecure(path)
            verify(fileGateway).isPathInsecure(path)
        }

    @Test
    fun `test that a duplicate of the old file is returned because of an error renaming the old file`() =
        runTest {
            val originalUriPath = UriPath("test/uri/path")
            val newFilename = "newFilename"
            val oldFile = File(originalUriPath.value)
            val newFile = File(oldFile.parentFile, newFilename)

            whenever(
                fileGateway.renameFile(
                    oldFile = oldFile,
                    newName = newFilename,
                )
            ).thenReturn(false)

            val result = underTest.renameFileAndDeleteOriginal(
                originalUriPath = originalUriPath,
                newFilename = newFilename,
            )

            verify(fileGateway, times(0)).deleteFile(oldFile)
            assertThat(result).isEqualTo(newFile)
        }

    @Test
    fun `test that the renamed file is returned and the old file is deleted`() = runTest {
        val originalUriPath = UriPath("test/uri/path")
        val newFilename = "newFilename"
        val oldFile = File(originalUriPath.value)
        val newFile = File(oldFile.parentFile, newFilename)
        whenever(
            fileGateway.renameFile(
                oldFile = oldFile,
                newName = newFilename,
            )
        ).thenReturn(true)
        whenever(fileGateway.deleteFile(oldFile)).thenReturn(true)

        val result = underTest.renameFileAndDeleteOriginal(
            originalUriPath = originalUriPath,
            newFilename = newFilename,
        )

        assertThat(result).isEqualTo(newFile)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that file descriptor from gateway is returned with correct parameters when getFileDescriptor is invoked`(
        writePermission: Boolean,
    ) = runTest {
        val expected = 354
        val parcelFileDescriptor = mock<ParcelFileDescriptor> {
            on { detachFd() } doReturn expected
        }
        val uriPath = mock<UriPath>()
        whenever(fileGateway.getFileDescriptor(uriPath, writePermission)) doReturn
                parcelFileDescriptor

        val actual = underTest.getDetachedFileDescriptor(uriPath, writePermission)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getDocumentMetadata returns correct value from gateway`() = runTest {
        mockStatic(Uri::class.java).use {
            val uriPath = UriPath("file://test/file/path.txt")
            val expected = mock<DocumentMetadata>()
            val uri = mock<Uri> {
                on { scheme } doReturn "file"
            }
            whenever(Uri.parse(uriPath.value)) doReturn uri
            whenever(fileGateway.getDocumentMetadata(uri)) doReturn expected

            val actual = underTest.getDocumentMetadata(uriPath)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getFolderChildUriPaths returns correct value from gateway`() = runTest {
        mockStatic(Uri::class.java).use {
            val uriPath = UriPath("file://test/file/path")
            val expected = UriPath("file://child.txt")
            val result = mock<Uri> {
                on { toString() } doReturn expected.value
            }
            val uri = mock<Uri> {
                on { scheme } doReturn "file"
            }
            whenever(Uri.parse(uriPath.value)) doReturn uri
            whenever(fileGateway.getFolderChildUris(uri)) doReturn listOf(result)

            val actual = underTest.getFolderChildUriPaths(uriPath)

            assertThat(actual).containsExactly(expected)
        }
    }
}
