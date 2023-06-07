package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileFacadeTest {

    private lateinit var underTest: FileFacade
    private val context: Context = mock()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileFacade(context)
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
        whenever(android.os.Environment.getExternalStorageDirectory()).thenReturn(
            java.io.File("/storage/emulated/0")
        )

        val actual = underTest.getExternalPathByContentUri(contentUri)

        assertThat(expected).isEqualTo(actual)

        uriMock.close()
        environmentMock.close()
    }
}