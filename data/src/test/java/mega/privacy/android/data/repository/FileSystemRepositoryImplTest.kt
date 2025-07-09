package mega.privacy.android.data.repository

import android.content.Context
import android.net.Uri
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
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.file.DocumentFileMapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
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
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.InputStream
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    private val fileAttributeGateway = mock<FileAttributeGateway>()
    private val mimeTypeMapper = mock<MimeTypeMapper>()
    private val documentFileWrapper = mock<DocumentFileWrapper>()
    private val documentFileMapper = mock<DocumentFileMapper>()

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
            fileAttributeGateway = fileAttributeGateway,
            documentFileWrapper = documentFileWrapper,
            documentFileMapper = documentFileMapper,
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
            fileAttributeGateway,
            mimeTypeMapper,
            documentFileWrapper,
            documentFileMapper,
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
            mockStatic(Uri::class.java).use {
                val testCoordinates = Pair(6.0, 9.0)
                val uri = mock<Uri> {
                    on { this.scheme } doReturn "file"
                }
                val uriPath = UriPath("/folder/foo.mp4")
                whenever(Uri.fromFile(File(uriPath.value))) doReturn uri
                whenever(fileAttributeGateway.getVideoGPSCoordinates(any()))
                    .thenReturn(testCoordinates)

                assertThat(underTest.getVideoGPSCoordinates(uriPath)).isEqualTo(
                    testCoordinates
                )
            }
        }

        @Test
        fun `test that the photo GPS coordinates are retrieved`() = runTest {
            mockStatic(Uri::class.java).use {
                val testCoordinates = Pair(6.0, 9.1)
                val uri = mock<Uri> {
                    on { this.scheme } doReturn "file"
                }
                val uriPath = UriPath("/folder/foo.jpg")
                whenever(Uri.fromFile(File(uriPath.value))) doReturn uri
                whenever(fileAttributeGateway.getPhotoGPSCoordinates(any<String>()))
                    .thenReturn(testCoordinates)

                assertThat(underTest.getPhotoGPSCoordinates(uriPath)).isEqualTo(
                    testCoordinates
                )
            }
        }

        @Test
        fun `test that the video GPS coordinates are retrieved from an Uri`() = runTest {
            mockStatic(Uri::class.java).use {
                val testCoordinates = Pair(6.1, 8.0)
                val uri = mock<Uri> {
                    on { this.scheme } doReturn "content"
                }
                val uriPath = UriPath("content:://foo.mp4")
                whenever(Uri.parse(uriPath.value)) doReturn uri
                whenever(fileAttributeGateway.getVideoGPSCoordinates(uri, context))
                    .thenReturn(testCoordinates)

                assertThat(underTest.getVideoGPSCoordinates(uriPath)).isEqualTo(testCoordinates)
            }
        }

        @Test
        fun `test that the photo GPS coordinates are retrieved from an Input Stream`() {
            mockStatic(Uri::class.java).use {
                runTest {
                    val testCoordinates = Pair(6.0, 8.1)
                    val inputStream = mock<InputStream>()
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                    }
                    val uriPath = UriPath("content:://foo.jpg")
                    whenever(Uri.parse(uriPath.value)) doReturn uri
                    whenever(fileGateway.getInputStream(uriPath)) doReturn inputStream
                    whenever(fileAttributeGateway.getPhotoGPSCoordinates(inputStream))
                        .thenReturn(testCoordinates)

                    assertThat(underTest.getPhotoGPSCoordinates(uriPath)).isEqualTo(testCoordinates)
                }
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
    fun `test that fileTypeInfo gets the duration from file attribute gateway when using an Uri path`() =
        runTest {
            val fileName = "video.mp4"
            val fileUri = "content://$fileName"
            val uriPath = UriPath(fileUri)
            val duration = 4567.milliseconds
            whenever(mimeTypeMapper(any())).thenReturn("mime")
            whenever(fileAttributeGateway.getVideoDuration(uriPath.value)) doReturn duration
            underTest.getFileTypeInfo(uriPath, fileName)
            val argumentCaptor = argumentCaptor<String>()
            verify(fileAttributeGateway).getVideoDuration(argumentCaptor.capture())
            assertThat(argumentCaptor.firstValue).isEqualTo(fileUri)
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
        mockStatic(Uri::class.java).use { _ ->
            val testUri = "file://test/file/path"
            val uri = mock<Uri>()
            whenever(Uri.parse(testUri)).thenReturn(uri)
            whenever(fileGateway.deleteFileByUri(uri)).thenReturn(expected)

            val actual = underTest.deleteFileByUri(testUri)

            verify(fileGateway).deleteFileByUri(uri)
            assertThat(actual).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteSyncDocumentFileBySyncContentUri deletes the file correctly`(expected: Boolean) =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val fileName = "a.jpg"
                val testUriPath = UriPath("content://test/file/path/$fileName")
                val uri = mock<Uri>() {
                    on { lastPathSegment } doReturn fileName
                }
                whenever(Uri.parse(testUriPath.value)).thenReturn(uri)
                val documentFile = mock<DocumentFile>() {
                    on { delete() } doReturn expected
                }
                whenever(
                    documentFileWrapper.getDocumentFileForSyncContentUri(
                        testUriPath.value,
                    )
                ).thenReturn(documentFile)

                val actual = underTest.deleteSyncDocumentFileBySyncContentUri(testUriPath)
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

    @ParameterizedTest(name = "pauseTransfers: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that canReadUri returns correctly`(canRead: Boolean) = runTest {
        val uri = "content://com.android.externalstorage.documents/tree/"

        whenever(fileGateway.canReadUri(uri)).thenReturn(canRead)

        assertThat(underTest.canReadUri(uri)).isEqualTo(canRead)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isFolderPath returns the gateway value`(expected: Boolean) = runTest {
        val path = "foo"
        whenever(fileGateway.isFolderPath(path)) doReturn expected

        assertThat(underTest.isFolderPath(path)).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that uriPathExists`(
        expected: Boolean,
    ) = runTest {
        val uriPath = UriPath("foo")
        whenever(fileGateway.doesUriPathExist(uriPath)) doReturn expected

        assertThat(underTest.doesUriPathExist(uriPath)).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that hasPersistedPermission returns the gateway value`(
        expected: Boolean,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            val uriPath = UriPath("content://test/file/path")
            val uri = mock<Uri> {
                on { scheme } doReturn "content"
            }
            whenever(Uri.parse(uriPath.value)) doReturn uri
            whenever(fileGateway.hasPersistedPermission(uri, true)) doReturn expected

            assertThat(underTest.hasPersistedPermission(uriPath, true)).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that takePersistedPermission calls the gateway method`(
        writePermission: Boolean,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            val uriPath = UriPath("content://test/file/path")
            val uri = mock<Uri> {
                on { scheme } doReturn "content"
            }
            whenever(Uri.parse(uriPath.value)) doReturn uri

            underTest.takePersistablePermission(uriPath, writePermission)

            verify(fileGateway).takePersistablePermission(uri, writePermission)
        }
    }

    @Test
    fun `test that getDocumentFile returns the correct value when called with uri`() = runTest {
        val uriString = "content://test/file/path"

        mockStatic(DocumentFile::class.java).use {
            val documentFile = mock<DocumentFile> {
                on { isDirectory } doReturn false
            }
            val expected = mock<DocumentEntity>()

            whenever(documentFileWrapper.getDocumentFile(uriString)) doReturn documentFile
            whenever(documentFileMapper(documentFile, 0, 0)) doReturn expected

            assertThat(underTest.getDocumentFileIfContentUri(uriString)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getDocumentFile returns the correct value when called with uri and file name`() =
        runTest {
            val uriString = "content://test/file/path"
            val fileName = "file.txt"

            mockStatic(DocumentFile::class.java).use {
                val documentFile = mock<DocumentFile>()
                val expected = mock<DocumentEntity>()

                whenever(
                    documentFileWrapper.getDocumentFile(
                        uriString,
                        fileName
                    )
                ) doReturn documentFile
                whenever(documentFileMapper(documentFile, 0, 0)) doReturn expected

                assertThat(underTest.getDocumentFileIfContentUri(uriString, fileName))
                    .isEqualTo(expected)
            }
        }

    @Test
    @OptIn(ExperimentalTime::class)
    fun `test that getLastModifiedTime invokes and returns correctly`() = runTest {
        val uriPath = UriPath("filePath")
        val lastModifiedTime = Instant.fromEpochMilliseconds(123456789L)

        whenever(fileGateway.getLastModifiedTime(uriPath)) doReturn lastModifiedTime

        assertThat(underTest.getLastModifiedTime(uriPath)).isEqualTo(lastModifiedTime)
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun `test that getLastModifiedTimeForSyncContentUri invokes and returns correctly`() = runTest {
        val uriPath = UriPath("filePath")
        val lastModifiedTime = Instant.fromEpochMilliseconds(123456789L)

        whenever(fileGateway.getLastModifiedTimeForSyncContentUri(uriPath)) doReturn lastModifiedTime

        assertThat(underTest.getLastModifiedTimeForSyncContentUri(uriPath)).isEqualTo(
            lastModifiedTime
        )
    }

    @Test
    fun `test that renameDocumentWithTheSameName renames all documents with the same name`() =
        runTest {
            mockStatic(Uri::class.java).use {
                val fileNames = listOf("f01.txt")
                val uriPaths = listOf(
                    UriPath("content://test/file/path/f01.txt"),
                )
                val documentFiles =
                    List(uriPaths.size) { mock<DocumentFile>() }
                val uris = listOf(
                    mock<Uri> {
                        on { lastPathSegment } doReturn "f01.txt"
                    })

                uriPaths.forEachIndexed { index, uriPath ->
                    whenever(Uri.parse(uriPath.value)) doReturn uris[index]
                    whenever(
                        documentFileWrapper.getDocumentFileForSyncContentUri(
                            uriPath.value,
                        )
                    ).thenReturn(
                        documentFiles[index]
                    )
                    whenever(
                        documentFiles[index].renameTo("f01 (${index + 1}).txt")
                    ).thenReturn(true)
                }

                underTest.renameDocumentWithTheSameName(uriPaths)

                uriPaths.forEachIndexed { index, uriPath ->
                    verify(documentFiles[index]).renameTo("f01 (${index + 1}).txt")
                }
            }
        }
}
