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
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.use

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
}