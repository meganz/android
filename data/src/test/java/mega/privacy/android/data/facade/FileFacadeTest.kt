package mega.privacy.android.data.facade

import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.provider.MediaStore.MediaColumns.DATE_ADDED
import android.provider.MediaStore.MediaColumns.DATE_MODIFIED
import android.provider.MediaStore.MediaColumns.DATE_TAKEN
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.file.DocumentFileMapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentMetadata
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalCoroutinesApi
@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileFacadeTest {

    private lateinit var underTest: FileFacade
    private val context: Context = mock()
    private val documentFileMapper: DocumentFileMapper = mock()
    private val environmentMock = mockStatic(Environment::class.java)
    private val deviceGateway = mock<DeviceGateway>()
    private val documentFileWrapper = mock<DocumentFileWrapper>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileFacade(
            context = context,
            documentFileMapper = documentFileMapper,
            deviceGateway = deviceGateway,
            documentFileWrapper = documentFileWrapper,
        )
    }

    @AfterAll
    fun clearMock() {
        environmentMock.close()
    }

    @Test
    fun `test that get external path by uri returns the same as uriString param if it is already a path`() =
        runTest {
            val uriString = "/storage/emulated/0/folder/file.txt"
            val expected = uriString

            assertThat(underTest.getExternalPathByUri(uriString)).isEqualTo(expected)
        }

    @Test
    fun `test that get external path by uri returns the uri string if uriString is a file uri`() =
        runTest {
            mockStatic(Uri::class.java).use { uriMock ->
                val uriString = "file:///storage/emulated/0/folder/file.txt"
                val expected = "/storage/emulated/0/Sync/someFolder"
                val contentUriMock: Uri = mock {
                    on { scheme } doReturn "file"
                    on { path } doReturn expected
                }

                whenever(contentUriMock.toString()).thenReturn(uriString)
                whenever(Uri.parse(uriString)).thenReturn(contentUriMock)
                whenever(documentFileWrapper.getAbsolutePathFromContentUri(contentUriMock)).thenReturn(
                    expected
                )

                assertThat(underTest.getExternalPathByUri(uriString)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that get external path by uri returns the uri string if uriString is a content uri`() =
        runTest {
            mockStatic(Uri::class.java).use { uriMock ->
                val uriString =
                    "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
                val expected = "/storage/emulated/0/Sync/someFolder"
                val contentUriMock: Uri = mock {
                    on { scheme } doReturn "content"
                }

                whenever(contentUriMock.toString()).thenReturn(uriString)
                whenever(contentUriMock.lastPathSegment).thenReturn("primary:Sync/someFolder")
                whenever(Uri.parse(uriString)).thenReturn(contentUriMock)
                whenever(documentFileWrapper.getAbsolutePathFromContentUri(contentUriMock)).thenReturn(
                    expected
                )

                assertThat(underTest.getExternalPathByUri(uriString)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that buildExternalStorageFile returns correctly`() = runTest {
        val file = mock<File> {
            on { absolutePath } doReturn "/storage/emulated/0"
        }
        whenever(Environment.getExternalStorageDirectory()).thenReturn(file)
        val actual = underTest.buildExternalStorageFile("/Mega.txt")

        assertThat(actual.path).isEqualTo("/storage/emulated/0/Mega.txt")
    }

    @Test
    fun `test that get file by path returns file if file exists`() = runTest {
        val result = underTest.getFileByPath(temporaryFolder.path)

        assertThat(result).isEqualTo(temporaryFolder)
    }

    @Test
    fun `test that get file by path returns null if file does not exist`() = runTest {
        val result = underTest.getFileByPath("non/existent/path")

        assertThat(result).isNull()
    }

    @Test
    fun `test that getTotalSize returns correct file size`() = runTest {
        val expectedSize = 1000L
        val file = mock<File> {
            on { isFile } doReturn true
            on { length() } doReturn expectedSize
        }

        val actualSize = underTest.getTotalSize(file)
        assertEquals(expectedSize, actualSize)
    }

    @Test
    fun `test that getTotalSize returns correct total size if it's a directory`() = runTest {
        val file1 = mock<File> {
            on { isFile } doReturn true
            on { isDirectory } doReturn false
            on { length() } doReturn 1000L
        }
        val file2 = mock<File> {
            on { isFile } doReturn true
            on { isDirectory } doReturn false
            on { length() } doReturn 1500L
        }
        val childDir = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn true
            on { listFiles() } doReturn arrayOf(file1, file2)
        }
        val dir = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn true
            on { listFiles() } doReturn arrayOf(file1, file2, childDir)
        }

        val actualSize = underTest.getTotalSize(dir)
        assertThat(actualSize).isEqualTo(5000L)
    }

    @Test
    fun `test that delete file by uri returns correct result`() = runTest {
        val contentUriMock: Uri = mock()
        val contentResolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.delete(contentUriMock, null, null)).thenReturn(1)
        val result = underTest.deleteFileByUri(contentUriMock)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that getFileNameFromUri returns correct result from DocumentFile file`() = runTest {
        mockStatic(Uri::class.java).use { _ ->
            val expected = "File name"
            val testUri = "file:///example"
            val file = File(temporaryFolder, "file.txt")
            file.createNewFile()
            val uri = mock<Uri> {
                on { this.scheme } doReturn "file"
                on { this.path } doReturn file.path
            }
            val documentFile = mock<DocumentFile> {
                on { name } doReturn expected
            }

            whenever(Uri.parse(testUri)).thenReturn(uri)
            whenever(documentFileWrapper.fromUri(uri)) doReturn documentFile

            assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getFileNameFromUri returns correct result from DocumentFile tree uri`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expected = "File name"
                val testUri = "file:///example"
                val uri = mock<Uri>()
                val documentFile = mock<DocumentFile> {
                    on { name } doReturn expected
                }

                whenever(Uri.parse(testUri)).thenReturn(uri)
                whenever(documentFileWrapper.fromUri(uri)) doReturn documentFile

                assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getFileNameFromUri returns correct result from DocumentFile single uri`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expected = "File name"
                val testUri = "file:///example"
                val uri = mock<Uri> {
                    on { this.scheme } doReturn "file"
                    on { this.path } doReturn testUri
                }
                val documentFile = mock<DocumentFile> {
                    on { name } doReturn expected
                }

                whenever(Uri.parse(testUri)).thenReturn(uri)
                whenever(documentFileWrapper.fromUri(uri)) doReturn documentFile

                assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
            }
        }


    @Test
    fun `test that getFileNameFromUri returns correct result from content resolver`() = runTest {
        mockStatic(Uri::class.java).use { _ ->
            val expected = "File name"
            val testUri = "uri://example"
            val sizeColumn = 3
            val uri = mock<Uri>()
            val contentResolver = mock<ContentResolver>()
            val cursor = mock<Cursor>()
            whenever(Uri.parse(testUri)).thenReturn(uri)
            whenever(context.contentResolver) doReturn contentResolver
            whenever(contentResolver.query(uri, null, null, null, null)) doReturn cursor
            whenever(cursor.moveToFirst()) doReturn true
            whenever(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) doReturn sizeColumn
            whenever(cursor.getString(sizeColumn)) doReturn expected

            val actual = underTest.getFileNameFromUri(testUri)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getFileSizeFromUri returns correct result from content resolver`() = runTest {
        mockStatic(Uri::class.java).use { _ ->
            val expected = 897455L
            val testUri = "uri://example"
            val sizeColumn = 2
            val uri = mock<Uri>()
            val contentResolver = mock<ContentResolver>()
            val cursor = mock<Cursor>()
            whenever(Uri.parse(testUri)).thenReturn(uri)
            whenever(context.contentResolver) doReturn contentResolver
            whenever(contentResolver.query(uri, null, null, null, null)) doReturn cursor
            whenever(cursor.moveToFirst()) doReturn true
            whenever(cursor.getColumnIndex(OpenableColumns.SIZE)) doReturn sizeColumn
            whenever(cursor.getLong(sizeColumn)) doReturn expected

            val actual = underTest.getFileSizeFromUri(testUri)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["../", "/data/data/mega.privacy.android.app/files/Download", "/data/user/0/mega.privacy.android.app", "../data/user/0/mega.privacy.android.app", "../data/data/mega.privacy.android.app"])
    fun `test that isPathInsecure returns true when a path which contains app directory is given`(
        path: String,
    ) =
        runTest {
            val result = underTest.isPathInsecure(path)

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isPathInsecure returns false when a path which does not contain app directory is given`() =
        runTest {
            val path = "/storage/emulated/0/Android/data/mega.privacy.android/files/Download"
            val result = underTest.isPathInsecure(path)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that isMalformedPathFromExternalApp returns false when a path which does not contain app directory is given`() =
        runTest {
            val action = "action"
            val path = "/storage/emulated/0/Android/data/mega.privacy.android/files/Download"
            val result = underTest.isMalformedPathFromExternalApp(action, path)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that isMalformedPathFromExternalApp returns true selected action is ACTION_SEND`() =
        runTest {
            val action = "android.intent.action.SEND"
            val path = "/data/data/mega.privacy.android.app/files/Download"
            val result = underTest.isMalformedPathFromExternalApp(action, path)

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isMalformedPathFromExternalApp returns true selected action is ACTION_SEND_MULTIPLE`() =
        runTest {
            val action = "android.intent.action.SEND_MULTIPLE"
            val path = "/data/data/mega.privacy.android.app/files/Download"
            val result = underTest.isMalformedPathFromExternalApp(action, path)

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isMalformedPathFromExternalApp returns false selected action is not ACTION_SEND or ACTION_SEND_MULTIPLE`() =
        runTest {
            val action = "action"
            val path = "/storage/emulated/0/Android/data/mega.privacy.android/files/Download"
            val result = underTest.isMalformedPathFromExternalApp(action, path)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that isMalformedPathFromExternalApp returns false when path is secure`() =
        runTest {
            val action = "android.intent.action.SEND_MULTIPLE"
            val path = "/storage/emulated/0/Android/data/mega.privacy.android/files/Download"
            val result = underTest.isMalformedPathFromExternalApp(action, path)

            assertThat(result).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that getFileDescriptor returns correct result from content resolver with correct permissions`(
        writePermission: Boolean,
    ) = runTest {
        mockStatic(Uri::class.java).use { _ ->
            val expected = mock<ParcelFileDescriptor>()
            val testUri = UriPath("uri://example")
            val uri = mock<Uri> {
                on { scheme } doReturn "file"
            }
            val contentResolver = mock<ContentResolver>()
            whenever(Uri.parse(testUri.value)).thenReturn(uri)
            whenever(context.contentResolver) doReturn contentResolver
            whenever(
                contentResolver.openFileDescriptor(
                    testUri.toUri(),
                    if (writePermission) "rw" else "r"
                )
            ) doReturn expected

            val actual = underTest.getFileDescriptorSync(testUri, writePermission)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getDocumentEntities returns the mapped entities from a list of content uris`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }
            val doc = mock<DocumentFile>()
            val expected = mock<DocumentEntity>()
            whenever(documentFileWrapper.fromUri(uri)) doReturn doc
            whenever(documentFileMapper(doc, 0, 0)) doReturn expected

            val actual = underTest.getDocumentEntities(listOf(uri))

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that getDocumentMetadata returns the mapped entity from a content uri file`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }
            val doc = mock<DocumentFile> {
                on { name } doReturn "file.txt"
                on { this.isDirectory } doReturn false
            }
            val expected = DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
            whenever(documentFileWrapper.fromUri(uri)) doReturn doc

            val actual = underTest.getDocumentMetadataSync(uri)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that getDocumentMetadata returns the correct values from a content uri folder`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }
            val doc = mock<DocumentFile> {
                on { name } doReturn "folder"
                on { this.isDirectory } doReturn true
            }
            val expected = DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
            whenever(documentFileWrapper.fromUri(uri)) doReturn doc

            val actual = underTest.getDocumentMetadataSync(uri)

            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that getDocumentMetadata returns the correct values from a file uri`(
        isDirectory: Boolean,
    ) = runTest {
        val file = File(temporaryFolder, "file.txt")
        file.createNewFile()
        val uri = mock<Uri> {
            on { this.scheme } doReturn "file"
            on { this.path } doReturn file.path
        }
        val doc = mock<DocumentFile> {
            on { name } doReturn "name"
            on { this.isDirectory } doReturn isDirectory
        }
        val expected = DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
        whenever(documentFileWrapper.fromUri(uri)) doReturn doc

        val actual = underTest.getDocumentMetadataSync(uri)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getFolderChildUris returns and empty list if getting DocumentFile throws an exception`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }

            whenever(documentFileWrapper.fromUri(uri)) doThrow RuntimeException()

            val actual = underTest.getFolderChildUrisSync(uri)

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that getFolderChildUris returns and empty list if listFiles throws an exception`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }
            val doc = mock<DocumentFile> {
                on { this.isDirectory } doReturn true
                on { this.listFiles() } doThrow RuntimeException()
            }

            whenever(documentFileWrapper.fromUri(uri)) doReturn doc

            val actual = underTest.getFolderChildUrisSync(uri)

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that getFolderChildUris returns the correct child uris`() =
        runTest {
            val uri = mock<Uri> {
                on { this.scheme } doReturn "content"
            }
            val expected = mock<Uri>()
            val child = mock<DocumentFile> {
                on { this.uri } doReturn expected
            }
            val doc = mock<DocumentFile> {
                on { this.isDirectory } doReturn true
                on { this.listFiles() } doReturn arrayOf(child)
            }
            whenever(documentFileWrapper.fromUri(uri)) doReturn doc

            val actual = underTest.getFolderChildUrisSync(uri)

            assertThat(actual).containsExactly(expected)
        }

    @ParameterizedTest(name = "pauseTransfers: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that canReadUri returns correctly`(canRead: Boolean) = runTest {
        mockStatic(Uri::class.java).use {
            val uriString = "content://com.android.externalstorage.documents/tree/"
            val uri = mock<Uri>()
            val documentFile = mock<DocumentFile> {
                on { canRead() } doReturn canRead
            }

            whenever(Uri.parse(uriString)).thenReturn(uri)
            whenever(documentFileWrapper.fromUri(uri)) doReturn documentFile

            assertThat(underTest.canReadUri(uriString)).isEqualTo(canRead)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that childFileExistsSync returns the correct child uris`(
        result: Boolean,
    ) = mockStatic(Uri::class.java).use {
        val childName = "child.txt"
        val childDoc = mock<DocumentFile> {
            on { this.name } doReturn if (result) childName else "another"
        }
        val doc = mock<DocumentFile> {
            on { this.name } doReturn "foo"
            on { this.isDirectory } doReturn true
            on { this.listFiles() } doReturn arrayOf(childDoc)
        }
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath("content://foo")
        whenever(Uri.parse(uriPath.value)) doReturn uri
        whenever(documentFileWrapper.fromUri(uri)) doReturn doc


        val actual = underTest.childFileExistsSync(uriPath, childName)

        assertThat(actual).isEqualTo(result)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test createChildFileSync returns the file created with gateway`(
        asFolder: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(MimeTypeMap::class.java).use {

            val doc = mock<DocumentFile> {
                on { this.isDirectory } doReturn true
            }
            val childName = "child"
            val expected = UriPath("child")
            val createdUri = mock<Uri> {
                on { this.toString() } doReturn expected.value
            }
            val childDocument = mock<DocumentFile> {
                on { this.uri } doReturn createdUri
            }
            if (asFolder) {
                whenever(doc.createDirectory(childName)) doReturn childDocument
            } else {
                whenever(MimeTypeMap.getSingleton()) doReturn mock()
                whenever(
                    doc.createFile("application/octet-stream", childName)
                ) doReturn childDocument
            }
            val uri = stubGetDocumentFileFromUri(doc)
            val uriPath = UriPath("content://foo")
            whenever(Uri.parse(uriPath.value)) doReturn uri

            val actual = underTest.createChildFileSync(uriPath, childName, asFolder)
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getParentSync returns the correct parent uri`() =
        mockStatic(Uri::class.java).use {
            val expected = UriPath("parent")
            val parentUri = mock<Uri> {
                on { this.toString() } doReturn expected.value
            }
            val parentDoc = mock<DocumentFile> {
                on { this.uri } doReturn parentUri
            }
            val doc = mock<DocumentFile> {
                on { this.parentFile } doReturn parentDoc
            }
            val uri = stubGetDocumentFileFromUri(doc)
            val uriPath = UriPath("content://foo")
            whenever(Uri.parse(uriPath.value)) doReturn uri


            val actual = underTest.getParentSync(uriPath)

            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAFileSync deletes the document only if it is a file`(
        isFile: Boolean,
    ) = mockStatic(Uri::class.java).use {
        val doc = mock<DocumentFile> {
            on { this.isFile } doReturn isFile
            on { this.delete() } doReturn true
        }
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath("content://foo")
        whenever(Uri.parse(uriPath.value)) doReturn uri

        val actual = underTest.deleteIfItIsAFileSync(uriPath)

        assertThat(actual).isEqualTo(isFile)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAnEmptyFolder deletes the document only if it is a folder`(
        isFolder: Boolean,
    ) = mockStatic(Uri::class.java).use {
        val doc = mock<DocumentFile> {
            on { this.isDirectory } doReturn isFolder
            on { this.listFiles() } doReturn emptyArray()
            on { this.delete() } doReturn true
        }
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath("content://foo")
        whenever(Uri.parse(uriPath.value)) doReturn uri

        val actual = underTest.deleteIfItIsAnEmptyFolder(uriPath)

        assertThat(actual).isEqualTo(isFolder)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAnEmptyFolder deletes the document only if the folder is empty`(
        isEmpty: Boolean,
    ) = mockStatic(Uri::class.java).use {
        val files = if (isEmpty) emptyArray() else arrayOf(mock<DocumentFile>())
        val doc = mock<DocumentFile> {
            on { this.isDirectory } doReturn true
            on { this.listFiles() } doReturn files
            on { this.delete() } doReturn true
        }
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath("content://foo")
        whenever(Uri.parse(uriPath.value)) doReturn uri

        val actual = underTest.deleteIfItIsAnEmptyFolder(uriPath)

        assertThat(actual).isEqualTo(isEmpty)
    }

    @Test
    fun `test that renameFileSync renames the document`(): Unit = mockStatic(Uri::class.java).use {
        val newName = "renamed"
        val doc = mock<DocumentFile>()
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath("content://foo")
        whenever(Uri.parse(uriPath.value)) doReturn uri

        underTest.renameFileSync(uriPath, newName)
        verify(doc).renameTo(newName)
    }

    @Test
    fun `test that getFileStorageTypeName returns null if file absolute path is null`() = runTest {

        val result = underTest.getFileStorageTypeName(null)

        assertThat(result).isEqualTo(FileStorageType.Unknown)
    }

    @Test
    fun `test that getFileStorageTypeName returns Build model when file is in primary external storage`() =
        runTest {
            val primaryStorage = File("/storage/emulated/0")
            whenever(Environment.getExternalStorageDirectory()).thenReturn(primaryStorage)
            whenever(deviceGateway.getDeviceModel()).thenReturn("SM-8673")
            val testFile = "/storage/emulated/0/test.txt"

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.Internal("SM-8673"))
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `test that getFileStorageTypeName returns SD Card when file is on SD card (API 30+)`() =
        runTest {
            val primaryStorage = File("/storage/emulated/0")
            whenever(Environment.getExternalStorageDirectory()).thenReturn(primaryStorage)
            whenever(deviceGateway.getSdkVersionInt()).thenReturn(Build.VERSION_CODES.R)
            val storageManager = mock<StorageManager>()
            val directoryFile = mock<File> {
                on { absolutePath } doReturn "/storage/sdcard"
            }
            val storageVolume = mock<StorageVolume> {
                on { isRemovable } doReturn true
                on { directory } doReturn directoryFile
            }
            whenever(storageManager.storageVolumes).thenReturn(listOf(storageVolume))
            whenever(context.getSystemService(StorageManager::class.java)).thenReturn(storageManager)

            val testFile = "/storage/sdcard/test.txt"

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.SdCard)
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `test that getFileStorageTypeName returns SD Card via UUID when file is on SD card (pre API 30)`() =
        runTest {
            whenever(Environment.getExternalStorageDirectory()).thenReturn(File("/storage/emulated/0"))
            whenever(deviceGateway.getSdkVersionInt()).thenReturn(Build.VERSION_CODES.Q)

            val storageManager = mock<StorageManager>()
            val storageVolume = mock<StorageVolume> {
                on { isRemovable } doReturn true
                on { uuid } doReturn "1234-5678"
            }
            whenever(storageManager.storageVolumes).thenReturn(listOf(storageVolume))
            whenever(context.getSystemService(StorageManager::class.java)).thenReturn(storageManager)

            val testFile = "/storage/1234-5678/test.txt"

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.SdCard)
        }


    @Test
    fun `test that getFileStorageTypeName returns SD Card when file is in fallback SD path`() =
        runTest {
            whenever(Environment.getExternalStorageDirectory()).thenReturn(File("/storage/emulated/0"))

            val storageManager = mock<StorageManager> {
                on { storageVolumes } doReturn emptyList()
            }
            whenever(context.getSystemService(StorageManager::class.java)).thenReturn(storageManager)

            val testFile = "/mnt/extSdCard/test.txt"

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.SdCard)
        }

    @Test
    fun `test that getFileStorageTypeName returns null when file is not in primary or SD storage`() =
        runTest {
            whenever(Environment.getExternalStorageDirectory()).thenReturn(File("/storage/emulated/0"))

            val storageManager = mock<StorageManager> {
                on { storageVolumes } doReturn emptyList()
            }
            whenever(context.getSystemService(StorageManager::class.java)).thenReturn(storageManager)

            val testFile = "/some/unknown/path.txt"

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.Unknown)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that doesUriPathExist returns value from document file`(expected: Boolean) = runTest {
        mockStatic(Uri::class.java).use {
            val documentFile = mock<DocumentFile> {
                on { this.exists() } doReturn expected
            }
            val uri = stubGetDocumentFileFromUri(documentFile)
            val uriPath = UriPath("content://foo")
            whenever(Uri.parse(uriPath.value)) doReturn uri

            val actual = underTest.doesUriPathExist(uriPath)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that hasPersistedPermission returns value from contentResolver`(expected: Boolean) =
        runTest {
            val uri = mock<Uri>()
            val contentResolver = mock<ContentResolver>()
            val uriPermission = mock<UriPermission> {
                on { this.uri } doReturn uri
                on { this.isReadPermission } doReturn expected
                on { this.isWritePermission } doReturn expected
            }
            whenever(context.contentResolver) doReturn contentResolver
            whenever(contentResolver.persistedUriPermissions) doReturn listOf(uriPermission)

            val actual = underTest.hasPersistedPermission(uri, true)

            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that takePersistedPermission correctly calls contentResolver method`(writePermission: Boolean) =
        runTest {
            val uri = mock<Uri>()
            val contentResolver = mock<ContentResolver>()
            whenever(context.contentResolver) doReturn contentResolver
            underTest.takePersistablePermission(uri, writePermission)

            verify(contentResolver).takePersistableUriPermission(
                uri,
                if (writePermission) Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION else Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    @Test
    fun `test that child file is returned when the name matches`(
    ) = mockStatic(Uri::class.java).use {
        val uriValue = "content://foo"
        val uri1 = mock<Uri> {
            on { this.scheme } doReturn "content"
            on { toString() } doReturn uriValue
        }
        val childName1 = "child.txt"
        val childName2 = "another"
        val childDoc1 = mock<DocumentFile> {
            on { this.name } doReturn childName1
            on { uri } doReturn uri1
        }
        val childDoc2 = mock<DocumentFile> {
            on { this.name } doReturn childName2
        }
        val doc = mock<DocumentFile> {
            on { this.name } doReturn "foo"
            on { this.isDirectory } doReturn true
            on { this.listFiles() } doReturn arrayOf(childDoc1, childDoc2)
        }
        val uri = stubGetDocumentFileFromUri(doc)
        val uriPath = UriPath(uriValue)
        whenever(Uri.parse(uriPath.value)) doReturn uri
        whenever(documentFileWrapper.fromUri(uri)) doReturn doc


        val actual = underTest.getChildByName(uriPath, childName1)

        assertThat(actual?.value).isEqualTo(uriPath.value)
    }

    @Test
    fun `test that removePersistentPermission does not release permission if uri is not a document uri`() =
        runTest {
            mockStatic(DocumentsContract::class.java).use { documentsContractMock ->
                whenever(DocumentsContract.isDocumentUri(any(), any())).doReturn(false)

                val uriPath = UriPath("content://com.example.invalid")
                underTest.removePersistentPermission(uriPath)
                val contentResolver = mock<ContentResolver>()
                whenever(context.contentResolver).thenReturn(contentResolver)

                verify(contentResolver, never()).releasePersistableUriPermission(
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `test that removePersistentPermission releases permission if uri is a document uri`() =
        runTest {
            mockStatic(DocumentsContract::class.java).use { documentsContractMock ->
                mockStatic(Uri::class.java).use { uriMock ->
                    val mockUri = mock<Uri>()
                    val uriPath =
                        UriPath("content://com.android.externalstorage.documents/tree/primary%3ADocuments")
                    whenever(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments"))
                        .thenReturn(mockUri)

                    whenever(DocumentsContract.isDocumentUri(any(), any())).doReturn(true)
                    whenever(DocumentsContract.isTreeUri(any())).doReturn(true)
                    whenever(DocumentsContract.getTreeDocumentId(any())).doReturn("primary:Documents")
                    val contentResolver = mock<ContentResolver>()
                    whenever(context.contentResolver).thenReturn(contentResolver)

                    underTest.removePersistentPermission(uriPath)

                    verify(context.contentResolver).releasePersistableUriPermission(
                        mockUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }

    @Test
    fun `test that getLastModifiedTime returns the last modified time for path from File`() =
        runTest {
            val fileName = "file.txt"
            val uriPath = UriPath("${temporaryFolder.absolutePath}/$fileName")
            val file = File(temporaryFolder, fileName)
            file.createNewFile()
            val expected = Instant.fromEpochMilliseconds(file.lastModified())

            assertThat(underTest.getLastModifiedTime(uriPath)).isEqualTo(expected)
        }

    @Test
    fun `test that getLastModifiedTime returns the last modified time for uri from DocumentFile`() =
        runTest {
            mockStatic(Uri::class.java).use {
                val expectedTime = 123456789L
                val expected = Instant.fromEpochMilliseconds(expectedTime)
                val documentFile = mock<DocumentFile> {
                    on { this.exists() } doReturn true
                    on { lastModified() } doReturn expectedTime
                }
                val uri = stubGetDocumentFileFromUri(documentFile)
                val uriPath = UriPath("content://foo")

                whenever(Uri.parse(uriPath.value)) doReturn uri

                assertThat(underTest.getLastModifiedTimeSync(uriPath)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getLastModifiedTime returns correct result for uri from content resolver and DATE_MODIFIED`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expectedTime = 123456789L
                val expected = Instant.fromEpochMilliseconds(expectedTime * 1000)
                val documentFile = mock<DocumentFile> {
                    on { this.exists() } doReturn true
                    on { lastModified() } doReturn 0
                }
                val uri = stubGetDocumentFileFromUri(documentFile)
                val uriPath = UriPath("content://foo")
                val contentResolver = mock<ContentResolver>()
                val cursor = mock<Cursor>()
                val client = mock<ContentProviderClient>()
                val sizeColumn = 2

                whenever(Uri.parse("content://foo")).thenReturn(uri)
                whenever(context.contentResolver) doReturn contentResolver
                whenever(contentResolver.acquireContentProviderClient(uri)) doReturn client
                whenever(client.query(uri, null, null, null, null)) doReturn cursor
                whenever(cursor.moveToFirst()) doReturn true
                whenever(cursor.getColumnIndex(DATE_MODIFIED)) doReturn sizeColumn
                whenever(cursor.getLong(sizeColumn)) doReturn expectedTime

                assertThat(underTest.getLastModifiedTimeSync(uriPath)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getLastModifiedTime returns correct result for uri from content resolver and DATE_ADDED`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expectedTime = 123456789L
                val expected = Instant.fromEpochMilliseconds(expectedTime * 1000)
                val documentFile = mock<DocumentFile> {
                    on { this.exists() } doReturn true
                    on { lastModified() } doReturn 0
                }
                val uri = stubGetDocumentFileFromUri(documentFile)
                val uriPath = UriPath("content://foo")
                val contentResolver = mock<ContentResolver>()
                val cursor = mock<Cursor>()
                val client = mock<ContentProviderClient>()
                val sizeColumn = 2

                whenever(Uri.parse("content://foo")).thenReturn(uri)
                whenever(context.contentResolver) doReturn contentResolver
                whenever(contentResolver.acquireContentProviderClient(uri)) doReturn client
                whenever(client.query(uri, null, null, null, null)) doReturn cursor
                whenever(cursor.moveToFirst()) doReturn true
                whenever(cursor.getColumnIndex(DATE_MODIFIED)) doReturn -1
                whenever(cursor.getColumnIndex(DATE_ADDED)) doReturn sizeColumn
                whenever(cursor.getLong(sizeColumn)) doReturn expectedTime

                assertThat(underTest.getLastModifiedTimeSync(uriPath)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getLastModifiedTime returns correct result for uri from content resolver and DATE_TAKEN`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expectedTime = 123456789L
                val expected = Instant.fromEpochMilliseconds(expectedTime)
                val documentFile = mock<DocumentFile> {
                    on { this.exists() } doReturn true
                    on { lastModified() } doReturn 0
                }
                val uri = stubGetDocumentFileFromUri(documentFile)
                val uriPath = UriPath("content://foo")
                val contentResolver = mock<ContentResolver>()
                val cursor = mock<Cursor>()
                val client = mock<ContentProviderClient>()
                val sizeColumn = 2

                whenever(Uri.parse("content://foo")).thenReturn(uri)
                whenever(context.contentResolver) doReturn contentResolver
                whenever(contentResolver.acquireContentProviderClient(uri)) doReturn client
                whenever(client.query(uri, null, null, null, null)) doReturn cursor
                whenever(cursor.moveToFirst()) doReturn true
                whenever(cursor.getColumnIndex(DATE_MODIFIED)) doReturn -1
                whenever(cursor.getColumnIndex(DATE_ADDED)) doReturn -1
                whenever(cursor.getColumnIndex(DATE_TAKEN)) doReturn sizeColumn
                whenever(cursor.getLong(sizeColumn)) doReturn expectedTime

                assertThat(underTest.getLastModifiedTimeSync(uriPath)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that getLastModifiedTime returns null for uri`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                val expectedTime = 123456789L
                val documentFile = mock<DocumentFile> {
                    on { this.exists() } doReturn true
                    on { lastModified() } doReturn 0
                }
                val uri = stubGetDocumentFileFromUri(documentFile)
                val uriPath = UriPath("content://foo")
                val contentResolver = mock<ContentResolver>()
                val cursor = mock<Cursor>()
                val client = mock<ContentProviderClient>()

                whenever(Uri.parse("content://foo")).thenReturn(uri)
                whenever(context.contentResolver) doReturn contentResolver
                whenever(contentResolver.acquireContentProviderClient(uri)) doReturn client
                whenever(client.query(uri, null, null, null, null)) doReturn cursor
                whenever(cursor.moveToFirst()) doReturn true
                whenever(cursor.getColumnIndex(DATE_MODIFIED)) doReturn -1
                whenever(cursor.getColumnIndex(DATE_ADDED)) doReturn -1
                whenever(cursor.getColumnIndex(DATE_TAKEN)) doReturn -1

                assertThat(underTest.getLastModifiedTimeSync(uriPath)).isNull()
            }
        }


    private fun stubGetDocumentFileFromUri(documentFile: DocumentFile): Uri {
        val uri = mock<Uri> {
            on { this.scheme } doReturn "content"
        }
        whenever(documentFileWrapper.fromUri(uri)) doReturn documentFile
        return uri
    }
}
