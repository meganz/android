package test.mega.privacy.android.app.main

import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import mega.privacy.android.app.main.FileExplorerViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.permisison.HasAudioPermissionUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {
    private lateinit var underTest: FileExplorerViewModel
    private val getCopyLatestTargetPathUseCase = mock<GetCopyLatestTargetPathUseCase>()
    private val getMoveLatestTargetPathUseCase = mock<GetMoveLatestTargetPathUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()
    private val hasAudioPermissionUseCase = mock<HasAudioPermissionUseCase>()

    @Before
    fun setUp() {
        underTest = FileExplorerViewModel(
            ioDispatcher = StandardTestDispatcher(),
            monitorStorageStateEventUseCase = mock(),
            getCopyLatestTargetPathUseCase = getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase = getMoveLatestTargetPathUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            hasAudioPermissionUseCase = hasAudioPermissionUseCase,
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

        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
        }

        assertThat(underTest.isImportingText(intent)).isTrue()
    }

    @Test
    fun `test that an intent with a stream extra is marked as not a text import`() {

        val bundle = mock<Bundle> {
            on { containsKey(Intent.EXTRA_STREAM) }.thenReturn(true)
        }

        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
            on { extras }.thenReturn(bundle)
        }

        assertThat(underTest.isImportingText(intent)).isFalse()
    }
}