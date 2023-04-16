package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

/**
 * Test class for [SDCardFacade]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SDCardFacadeTest {

    private lateinit var underTest: SDCardGateway

    private val context = mock<Context>()
    private val documentFileWrapper = mock<DocumentFileWrapper>()

    @BeforeAll
    fun setUp() {
        underTest = SDCardFacade(
            context = context,
            documentFileWrapper = documentFileWrapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(context, documentFileWrapper)
    }

    @ParameterizedTest(name = "can write document file: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the directory name is retrieved`(canWrite: Boolean) = runTest {
        mockStatic(Uri::class.java).use { mockedUri ->
            mockedUri.`when`<Uri> { Uri.parse(any()) }.thenReturn(mock())

            val expectedName = "Test Name"
            val documentFile = mock<DocumentFile> {
                on { name }.thenReturn(expectedName)
                on { canWrite() }.thenReturn(canWrite)
            }
            val directoryName = if (documentFile.canWrite()) expectedName else ""

            whenever(documentFileWrapper.fromTreeUri(any())).thenReturn(documentFile)
            assertThat(underTest.getDirectoryName("test/local/path")).isEqualTo(directoryName)
        }
    }

    @Test
    fun `test that the directory name is empty if the document file does not exist`() = runTest {
        mockStatic(Uri::class.java).use { mockedUri ->
            mockedUri.`when`<Uri> { Uri.parse(any()) }.thenReturn(mock())
            whenever(documentFileWrapper.fromTreeUri(any())).thenReturn(null)
            assertThat(underTest.getDirectoryName("test/local/path")).isEmpty()
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideFolderExistsTestParameters")
    fun `test that the folder's existence is verified`(
        externalDir: Array<File?>,
        localPath: String,
        folderExists: Boolean,
    ) = runTest {
        whenever(context.getExternalFilesDirs(null)).thenReturn(externalDir)
        assertThat(underTest.doesFolderExists(localPath)).isEqualTo(folderExists)
    }

    private fun provideFolderExistsTestParameters() =
        Stream.of(
            Arguments.of(
                arrayOf<File?>(mock(), mock {
                    on { absolutePath }.thenReturn("/storage/sdcard0/DCIM/")
                }),
                "/storage/sdcard0/DCIM/Camera/",
                true
            ),
            Arguments.of(
                arrayOf<File>(mock(), mock {
                    on { absolutePath }.thenReturn("/storage/sdcard0/DCIM/")
                }),
                "/storage2/sdcard0/DCIM/Camera",
                false
            ),
            Arguments.of(emptyArray<File?>(), "/storage/sdcard0/DCIM/Camera/", false),
            Arguments.of(arrayOf<File?>(mock(), null), "/storage/sdcard0/DCIM/Camera/", false)
        )

    @ParameterizedTest(name = "local path: {0}")
    @MethodSource("provideRootSDCardPathTestParameters")
    fun `test that the root SD card path is retrieved`(path: String, maxIndex: Int) = runTest {
        val expectedRootSDCardPath = path.substring(0, maxIndex)
        val rootSDCardPath = underTest.getRootSDCardPath(path)

        assertThat(expectedRootSDCardPath).isEqualTo(rootSDCardPath)
    }

    private fun provideRootSDCardPathTestParameters() =
        Stream.of(
            Arguments.of("/storage/sdcard0/DCIM/DCIM2/DCIM3", 17),
            Arguments.of("/storage/sdcard0/DCIM", 17),
            Arguments.of("/storage/sdcard0", 16),
            Arguments.of("/storage", 8),
            Arguments.of("", 0)
        )
}