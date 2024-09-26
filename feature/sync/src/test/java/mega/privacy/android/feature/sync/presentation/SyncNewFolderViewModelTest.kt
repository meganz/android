package mega.privacy.android.feature.sync.presentation

import mega.privacy.android.shared.resources.R as sharedR
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderState
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNewFolderViewModelTest {

    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase = mock()
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase = mock()
    private val syncFolderPairUseCase: SyncFolderPairUseCase = mock()
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase = mock()
    private val getLocalDCIMFolderPathUseCase: GetLocalDCIMFolderPathUseCase = mock()
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase = mock()
    private lateinit var underTest: SyncNewFolderViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            getExternalPathByContentUriUseCase,
            monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase,
            isStorageOverQuotaUseCase,
            getLocalDCIMFolderPathUseCase,
            clearSelectedMegaFolderUseCase
        )
    }

    @Test
    fun `test that local folder selected action results in updated state`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel()
        val localFolderContentUri =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
        val localFolderUri: Uri = mock()
        val localFolderFolderStoragePath = "/storage/emulated/0/Sync/someFolder"
        val localDCIMFolderPath = "/storage/emulated/0/DCIM"
        val expectedState = SyncNewFolderState(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
            selectedLocalFolder = localFolderFolderStoragePath
        )
        whenever(getExternalPathByContentUriUseCase.invoke(localFolderContentUri)).thenReturn(
            localFolderFolderStoragePath
        )
        whenever(localFolderUri.toString()).thenReturn(localFolderContentUri)
        whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
            localDCIMFolderPath
        )

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

        assertThat(expectedState.selectedLocalFolder).isEqualTo(underTest.state.value.selectedLocalFolder)
    }

    @Test
    fun `test that snackbar with warning message is displayed if try to select DCIM as local device folder`() =
        runTest {
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
            initViewModel()
            val localFolderContentUri =
                "content://com.android.externalstorage.documents/tree/primary%3ADCIM"
            val localFolderUri: Uri = mock()
            val localFolderFolderStoragePath = "/storage/emulated/0/DCIM"
            val localDCIMFolderPath = "/storage/emulated/0/DCIM"
            whenever(getExternalPathByContentUriUseCase.invoke(localFolderContentUri)).thenReturn(
                localFolderFolderStoragePath
            )
            whenever(localFolderUri.toString()).thenReturn(localFolderContentUri)
            whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
                localDCIMFolderPath
            )

            underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

            with(underTest) {
                state.test {
                    val result =
                        (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                    assertThat(result).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
                }
            }
        }

    @Test
    fun `test that when mega folder is updated state is also updated`() = runTest {
        val remoteFolder = RemoteFolder(123L, "someFolder")
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(remoteFolder)
            awaitCancellation()
        })
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(remoteFolder)
            awaitCancellation()
        })
        initViewModel()
        val expectedState = SyncNewFolderState(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
            selectedMegaFolder = remoteFolder,
        )

        assertThat(expectedState).isEqualTo(underTest.state.value)
    }

    @Test
    fun `test that next click creates new folder pair and navigates to next screen`() = runTest {
        val remoteFolder = RemoteFolder(123L, "someFolder")
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(remoteFolder)
            awaitCancellation()
        })
        whenever(
            syncFolderPairUseCase.invoke(
                syncType = SyncType.TYPE_TWOWAY,
                name = remoteFolder.name,
                localPath = "",
                remotePath = remoteFolder,
            )
        ).thenReturn(true)
        val state = SyncNewFolderState(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
            selectedMegaFolder = remoteFolder,
        )
        initViewModel()

        underTest.handleAction(SyncNewFolderAction.NextClicked)

        verify(syncFolderPairUseCase).invoke(
            syncType = SyncType.TYPE_TWOWAY,
            name = remoteFolder.name,
            localPath = state.selectedLocalFolder,
            remotePath = remoteFolder,
        )
        assertThat(underTest.state.value.openSyncListScreen).isEqualTo(triggered)
    }

    @Test
    fun `test that next click shows error when storage is over quota`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        whenever(isStorageOverQuotaUseCase()).thenReturn(true)
        initViewModel()

        underTest.handleAction(SyncNewFolderAction.NextClicked)

        assertThat(underTest.state.value.showStorageOverQuota).isEqualTo(true)
    }

    @Test
    fun `test that storage over quota shown resets showStorageOverQuota event`() {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        initViewModel()

        underTest.handleAction(SyncNewFolderAction.StorageOverquotaShown)

        assertThat(underTest.state.value.showStorageOverQuota).isEqualTo(false)
    }

    @Test
    fun `test that sync list screen opened resets openSyncListScreen event`() {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        initViewModel()

        underTest.handleAction(SyncNewFolderAction.SyncListScreenOpened)

        assertThat(underTest.state.value.openSyncListScreen).isEqualTo(consumed)
    }

    @Test
    fun `test that clear selected mega folder use case is called when view model is initiated`() {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })

        initViewModel()

        verify(clearSelectedMegaFolderUseCase).invoke()
    }

    private fun initViewModel() {
        underTest = SyncNewFolderViewModel(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
            getExternalPathByContentUriUseCase = getExternalPathByContentUriUseCase,
            monitorSelectedMegaFolderUseCase = monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase = syncFolderPairUseCase,
            isStorageOverQuotaUseCase = isStorageOverQuotaUseCase,
            getLocalDCIMFolderPathUseCase = getLocalDCIMFolderPathUseCase,
            clearSelectedMegaFolderUseCase = clearSelectedMegaFolderUseCase,
        )
    }
}
