package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentFileFacadeTest {
    private lateinit var underTest: DocumentFileWrapper

    private val context = mock<Context>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setup() {
        underTest = DocumentFileFacade(context)
    }

    @BeforeEach
    fun cleanUp() {
        reset(context)
    }

    @Test
    fun `test that get from uri with a file uri returns the correct document`() = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                val testUri = "file:///example"
                val file = File(temporaryFolder, "file.txt")
                file.createNewFile()
                val uri = mock<Uri> {
                    on { this.scheme } doReturn "file"
                    on { this.path } doReturn file.path
                }
                val expected = mock<DocumentFile>()

                whenever(Uri.parse(testUri)).thenReturn(uri)
                whenever(DocumentFile.fromFile(file)) doReturn expected

                val actual = underTest.fromUri(uri)

                assertThat(actual).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that get from uri with a document folder uri returns the correct document`() =
        runTest {
            mockStatic(Uri::class.java).use {
                mockStatic(DocumentFile::class.java).use {
                    mockStatic(DocumentsContract::class.java).use {
                        val testUri = "content:///example"
                        val uri = mock<Uri> {
                            on { this.scheme } doReturn "content"
                            on { this.path } doReturn testUri
                        }
                        val expected = mock<DocumentFile>()

                        whenever(Uri.parse(testUri)).thenReturn(uri)
                        whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                        whenever(DocumentFile.fromTreeUri(context, uri)) doReturn expected

                        val actual = underTest.fromUri(uri)

                        assertThat(actual).isEqualTo(expected)
                    }
                }
            }
        }

    @Test
    fun `test that get from uri with a document file uri returns the correct document`() = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val testUri = "content:///example"
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                        on { this.path } doReturn testUri
                    }
                    val expected = mock<DocumentFile>()

                    whenever(Uri.parse(testUri)).thenReturn(uri)
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn expected

                    val actual = underTest.fromUri(uri)

                    assertThat(actual).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "file:///storage/emulated/0/Movies/1 transfers/empty",
            "file:///storage/emulated/0/test/text.txt",
        ]
    )
    fun `test that get document file from file uri returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val file =
                        File(temporaryFolder, stringUri.substringAfterLast(File.separator))
                    file.createNewFile()
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "file"
                        on { this.path } doReturn file.path
                    }
                    val documentFile = mock<DocumentFile>()
                    val expected = documentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentFile.fromFile(file)) doReturn documentFile

                    assertThat(underTest.getDocumentFile(stringUri)).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content://com.android.externalstorage.documents/tree/primary%3AMusic",
            "content://com.android.externalstorage.documents/tree/primary%3AMovies//1 transfers/empty",
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments//test/text.txt",
        ]
    )
    fun `test that get document file from tree uri returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                        on { this.path } doReturn stringUri
                    }
                    val folderTree = stringUri.removePrefix("content://").let {
                        val folderTree = it.substringAfterLast("//")

                        if (it == folderTree) {
                            emptyList()
                        } else {
                            folderTree.split(File.separator)
                        }
                    }
                    val startDocumentFile = mock<DocumentFile>()
                    val folderTreeDocumentFiles = buildList {
                        folderTree.forEach { folder ->
                            add(mock<DocumentFile> { on { name } doReturn folder })
                        }
                    }
                    val expected = if (folderTree.isEmpty()) {
                        startDocumentFile
                    } else {
                        folderTreeDocumentFiles.last()
                    }
                    var documentFile = startDocumentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                    whenever(DocumentFile.fromTreeUri(context, uri)) doReturn startDocumentFile
                    folderTreeDocumentFiles.forEach { newDocumentFile ->
                        newDocumentFile.name?.let {
                            whenever(documentFile.findFile(it)) doReturn newDocumentFile
                        }
                        documentFile = newDocumentFile
                    }

                    assertThat(underTest.getDocumentFile(stringUri)).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content://com.android.externalstorage.documents/Movies/1 transfers/empty",
            "content://com.android.externalstorage.documents/Documents/test/text.txt",
        ]
    )
    fun `test that get document file from single uri returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                        on { this.path } doReturn stringUri
                    }
                    val documentFile = mock<DocumentFile>()
                    val expected = documentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn documentFile

                    assertThat(underTest.getDocumentFile(stringUri)).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "file:///storage/emulated/0/Movies/1 transfers/empty",
            "file:///storage/emulated/0/test",
        ]
    )
    fun `test that get document file from file uri and file name returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val fileName = "test.txt"
                    val file =
                        File(temporaryFolder, stringUri.substringAfterLast(File.separator))
                    file.createNewFile()
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "file"
                        on { this.path } doReturn file.path
                    }
                    val documentFile = mock<DocumentFile>()
                    val expected = documentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentFile.fromFile(file)) doReturn documentFile

                    assertThat(underTest.getDocumentFile(stringUri, fileName)).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content://com.android.externalstorage.documents/tree/primary%3AMusic",
            "content://com.android.externalstorage.documents/tree/primary%3AMovies//1 transfers/empty",
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments//test",
        ]
    )
    fun `test that get document file from tree uri and file name returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val fileName = "test.txt"
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                        on { this.path } doReturn stringUri
                    }
                    val folderTree = stringUri.removePrefix("content://").let {
                        val folderTree = it.substringAfterLast("//")

                        if (it == folderTree) {
                            emptyList()
                        } else {
                            folderTree.split(File.separator)
                        }
                    }
                    val startDocumentFile = mock<DocumentFile>()
                    val treeDocumentFiles = buildList {
                        folderTree.forEach { folder ->
                            add(mock<DocumentFile> { on { name } doReturn folder })
                        }
                    }
                    val expected = if (folderTree.isEmpty()) {
                        startDocumentFile
                    } else {
                        mock<DocumentFile> { on { name } doReturn fileName }
                    }
                    var documentFile = startDocumentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn true
                    whenever(DocumentFile.fromTreeUri(context, uri)) doReturn startDocumentFile
                    treeDocumentFiles.forEach { newDocumentFile ->
                        newDocumentFile.name?.let {
                            whenever(documentFile.findFile(it)) doReturn newDocumentFile
                        }
                        documentFile = newDocumentFile
                    }
                    whenever(documentFile.findFile(fileName)) doReturn expected

                    assertThat(underTest.getDocumentFile(stringUri, fileName)).isEqualTo(expected)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content://com.android.externalstorage.documents/Movies/1 transfers/empty",
            "content://com.android.externalstorage.documents/Documents/test",
        ]
    )
    fun `test that get document file from single uri and file name returns correctly`(
        stringUri: String,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockStatic(DocumentFile::class.java).use {
                mockStatic(DocumentsContract::class.java).use {
                    val fileName = "test.txt"
                    val uri = mock<Uri> {
                        on { this.scheme } doReturn "content"
                        on { this.path } doReturn stringUri
                    }
                    val documentFile = mock<DocumentFile>()
                    val expected = documentFile

                    whenever(Uri.parse(stringUri)) doReturn uri
                    whenever(DocumentsContract.isTreeUri(uri)) doReturn false
                    whenever(DocumentFile.fromSingleUri(context, uri)) doReturn documentFile

                    assertThat(underTest.getDocumentFile(stringUri, fileName)).isEqualTo(expected)
                }
            }
        }
    }
}