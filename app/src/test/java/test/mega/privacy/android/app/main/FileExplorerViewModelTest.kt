package test.mega.privacy.android.app.main

import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import mega.privacy.android.app.main.FileExplorerViewModel
import mega.privacy.android.app.utils.Constants
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FileExplorerViewModelTest {
    private lateinit var underTest: FileExplorerViewModel

    @Before
    fun setUp() {
        underTest = FileExplorerViewModel(
            ioDispatcher = StandardTestDispatcher(),
            monitorStorageStateEvent = mock()
        )
    }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     */

    @Test
    fun `test that an intent with action send, type plain text and no stream extra is marked as a text import`() {

        val intent = mock<Intent>{
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
        }

        assertThat(underTest.isImportingText(intent)).isTrue()
    }

    @Test
    fun `test that an intent with a stream extra is marked as not a text import`() {

        val bundle = mock<Bundle>{
            on { containsKey(Intent.EXTRA_STREAM) }.thenReturn(true)
        }

        val intent = mock<Intent>{
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
            on { extras }.thenReturn(bundle)
        }

        assertThat(underTest.isImportingText(intent)).isFalse()
    }
}