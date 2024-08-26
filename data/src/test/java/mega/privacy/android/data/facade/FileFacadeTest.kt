package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.file.DocumentFileMapper
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
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileFacadeTest {

    private lateinit var underTest: FileFacade
    private val context: Context = mock()
    private val documentFileMapper: DocumentFileMapper = mock()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileFacade(context, documentFileMapper)
    }

    @Test
    fun `test that get external path by content uri returns the uri string`() = runTest {
        val uriMock = mockStatic(Uri::class.java)
        val environmentMock = mockStatic(android.os.Environment::class.java)
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
        environmentMock.close()
    }

    @Test
    fun `test that buildExternalStorageFile returns correctly`() = runTest {
        val file = mock<File> {
            on { absolutePath } doReturn "/storage/emulated/0"
        }
        val environmentMock = mockStatic(Environment::class.java)
        whenever(Environment.getExternalStorageDirectory()).thenReturn(file)
        val actual = underTest.buildExternalStorageFile("/Mega.txt")

        assertThat(actual.path).isEqualTo("/storage/emulated/0/Mega.txt")
        environmentMock.close()
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
}