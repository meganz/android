package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileFacadeTest {

    private lateinit var underTest: FileFacade
    private val context: Context = mock()
    private val documentFileMapper: DocumentFileMapper = mock()
    private val environmentMock = mockStatic(Environment::class.java)
    private val deviceGateway = mock<DeviceGateway>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileFacade(
            context = context,
            documentFileMapper = documentFileMapper,
            deviceGateway = deviceGateway
        )
    }

    @AfterAll
    fun clearMock() {
        environmentMock.close()
    }

    @Test
    fun `test that get external path by content uri returns the uri string`() = runTest {
        val uriMock = mockStatic(Uri::class.java)
        val contentUri =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
        val expected = "/storage/emulated/0/Sync/someFolder"
        val contentUriMock: Uri = mock()
        whenever(contentUriMock.toString()).thenReturn(contentUri)
        whenever(contentUriMock.lastPathSegment).thenReturn("primary:Sync/someFolder")
        whenever(Uri.parse(contentUri)).thenReturn(contentUriMock)
        whenever(Environment.getExternalStorageDirectory()).thenReturn(
            File("/storage/emulated/0")
        )

        val actual = underTest.getExternalPathByContentUri(contentUri)

        assertThat(expected).isEqualTo(actual)

        uriMock.close()
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
            mockStatic(DocumentFile::class.java).use {
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
                whenever(DocumentFile.fromFile(file)) doReturn documentFile

                assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that getFileNameFromUri returns correct result from DocumentFile tree uri`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                mockStatic(DocumentFile::class.java).use {
                    mockStatic(DocumentsContract::class.java).use {
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
                        whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                        whenever(DocumentFile.fromTreeUri(context, uri)) doReturn documentFile

                        assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
                    }
                }
            }
        }

    @Test
    fun `test that getFileNameFromUri returns correct result from DocumentFile single uri`() =
        runTest {
            mockStatic(Uri::class.java).use { _ ->
                mockStatic(DocumentFile::class.java).use {
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
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn documentFile

                    assertThat(underTest.getFileNameFromUri(testUri)).isEqualTo(expected)
                }
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
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                    }
                    val doc = mock<DocumentFile>()
                    val expected = mock<DocumentEntity>()
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn doc
                    whenever(documentFileMapper(doc, 0, 0)) doReturn expected

                    val actual = underTest.getDocumentEntities(listOf(uri))

                    assertThat(actual).containsExactly(expected)
                }
            }
        }

    @Test
    fun `test that getDocumentMetadata returns the mapped entity from a content uri file`() =
        runTest {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                    }
                    val doc = mock<DocumentFile> {
                        on { name } doReturn "file.txt"
                        on { this.isDirectory } doReturn false
                    }
                    val expected = DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn doc

                    val actual = underTest.getDocumentMetadataSync(uri)

                    assertThat(actual).isEqualTo(expected)
                }
            }
        }

    @Test
    fun `test that getDocumentMetadata returns the correct values from a content uri folder`() =
        runTest {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                    }
                    val doc = mock<DocumentFile> {
                        on { name } doReturn "folder"
                        on { this.isDirectory } doReturn true
                    }
                    val expected = DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                    whenever(DocumentFile.fromTreeUri(context, uri)) doReturn doc

                    val actual = underTest.getDocumentMetadataSync(uri)

                    assertThat(actual).isEqualTo(expected)
                }
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that getDocumentMetadata returns the correct values from a file uri`(
        isDirectory: Boolean,
    ) = runTest {
        mockStatic(DocumentFile::class.java).use {
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
            whenever(DocumentFile.fromFile(file)) doReturn doc

            val actual = underTest.getDocumentMetadataSync(uri)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getFolderChildUris returns the correct child uris`() =
        runTest {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
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
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                    whenever(DocumentFile.fromTreeUri(context, uri)) doReturn doc

                    val actual = underTest.getFolderChildUrisSync(uri)

                    assertThat(actual).containsExactly(expected)
                }
            }
        }

    @ParameterizedTest(name = "pauseTransfers: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that canReadUri returns correctly`(canRead: Boolean) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uriString = "content://com.android.externalstorage.documents/tree/"
                    val uri = mock<Uri>()
                    val documentFile = mock<DocumentFile> {
                        on { canRead() } doReturn canRead
                    }

                    whenever(Uri.parse(uriString)).thenReturn(uri)
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn documentFile

                    assertThat(underTest.canReadUri(uriString)).isEqualTo(canRead)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that childFileExistsSync returns the correct child uris`(
        result: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
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


                val actual = underTest.childFileExistsSync(uriPath, childName)

                assertThat(actual).isEqualTo(result)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test createChildFileSync returns the file created with gateway`(
        asFolder: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
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
        }
    }

    @Test
    fun `test that getParentSync returns the correct parent uri`() =
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
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
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAFileSync deletes the document only if it is a file`(
        isFile: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
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
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAnEmptyFolder deletes the document only if it is a folder`(
        isFolder: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
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
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that deleteIfItIsAnEmptyFolder deletes the document only if the folder is empty`(
        isEmpty: Boolean,
    ) = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
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
        }
    }

    @Test
    fun `test that renameFileSync renames the document`(): Unit = mockStatic(Uri::class.java).use {
        mockStatic(DocumentFile::class.java).use {
            mockStatic(DocumentsContract::class.java).use {
                val newName = "renamed"
                val doc = mock<DocumentFile>()
                val uri = stubGetDocumentFileFromUri(doc)
                val uriPath = UriPath("content://foo")
                whenever(Uri.parse(uriPath.value)) doReturn uri

                underTest.renameFileSync(uriPath, newName)
                verify(doc).renameTo(newName)
            }
        }
    }

    @Test
    fun `test that getFileStorageTypeName returns null if file absolute path is null`() = runTest {
        val testFile = mock<File> {
            on { absolutePath } doReturn null
        }

        val result = underTest.getFileStorageTypeName(testFile)

        assertThat(result).isEqualTo(FileStorageType.Unknown)
    }

    @Test
    fun `test that getFileStorageTypeName returns Build model when file is in primary external storage`() =
        runTest {
            val primaryStorage = File("/storage/emulated/0")
            whenever(Environment.getExternalStorageDirectory()).thenReturn(primaryStorage)
            whenever(deviceGateway.getDeviceModel()).thenReturn("SM-8673")
            val testFile = mock<File> {
                on { absolutePath } doReturn "/storage/emulated/0/test.txt"
            }

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

            val testFile = mock<File> {
                on { absolutePath } doReturn "/storage/sdcard/test.txt"
            }

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

            val testFile = mock<File> {
                on { absolutePath } doReturn "/storage/1234-5678/test.txt"
            }

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

            val testFile = mock<File> {
                on { absolutePath } doReturn "/mnt/extSdCard/test.txt"
            }

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

            val testFile = mock<File> {
                on { absolutePath } doReturn "/some/unknown/path.txt"
            }

            val result = underTest.getFileStorageTypeName(testFile)

            assertThat(result).isEqualTo(FileStorageType.Unknown)
        }


    private fun stubGetDocumentFileFromUri(documentFile: DocumentFile): Uri {
        val uri = mock<Uri> {
            on { this.scheme } doReturn "content"
        }
        whenever(DocumentsContract.isTreeUri(uri)) doReturn false
        whenever(DocumentFile.fromSingleUri(context, uri)) doReturn documentFile
        return uri
    }
}