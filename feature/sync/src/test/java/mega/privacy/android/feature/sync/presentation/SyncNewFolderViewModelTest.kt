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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.exception.BackupAlreadyExistsException
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.backup.MyBackupsFolderExistsUseCase
import mega.privacy.android.feature.sync.domain.usecase.backup.SetMyBackupsFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNewFolderViewModelTest {

    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase = mock()
    private val syncFolderPairUseCase: SyncFolderPairUseCase = mock()
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase = mock()
    private val getLocalDCIMFolderPathUseCase: GetLocalDCIMFolderPathUseCase = mock()
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val getDeviceNameUseCase: GetDeviceNameUseCase = mock()
    private val getFolderPairsUseCase: GetFolderPairsUseCase = mock()
    private val myBackupsFolderExistsUseCase: MyBackupsFolderExistsUseCase = mock()
    private val setMyBackupsFolderUseCase: SetMyBackupsFolderUseCase = mock()
    private lateinit var underTest: SyncNewFolderViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase,
            isStorageOverQuotaUseCase,
            getLocalDCIMFolderPathUseCase,
            clearSelectedMegaFolderUseCase,
            getDeviceIdUseCase,
            getDeviceNameUseCase,
            getFolderPairsUseCase,
            myBackupsFolderExistsUseCase,
            setMyBackupsFolderUseCase,
        )
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that local folder selected action results in updated state`(syncType: SyncType) =
        runTest {
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
            initViewModel(syncType = syncType)
            val localFolderUri: Uri = mock()
            val localFolderStoragePath = "/storage/emulated/0/Sync/someFolder"
            val localDCIMFolderPath = "/storage/emulated/0/DCIM"
            val expectedState = SyncNewFolderState(
                syncType = syncType,
                deviceName = "Device Name",
                selectedLocalFolder = localFolderStoragePath
            )
            whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
                localDCIMFolderPath
            )
            whenever(getFolderPairsUseCase.invoke()).thenReturn(emptyList())
            whenever(localFolderUri.path).thenReturn(localFolderStoragePath)
            whenever(localFolderUri.scheme).thenReturn("file")

            underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

            assertThat(expectedState.selectedLocalFolder).isEqualTo(underTest.state.value.selectedLocalFolder)
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that snackbar with warning message is displayed if try to select DCIM as local device folder`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)
        val localFolderUri: Uri = mock()
        val localDCIMFolderPath = "/storage/emulated/0/DCIM"
        whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
            localDCIMFolderPath
        )
        whenever(localFolderUri.path).thenReturn(localDCIMFolderPath)
        whenever(localFolderUri.scheme).thenReturn("file")

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

        with(underTest) {
            state.test {
                val result = (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                assertThat(result).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
            }
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that snackbar with warning message is displayed if try to select an already synced local device folder`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)
        val localFolderUri: Uri = mock()
        val localDCIMFolderPath = "/storage/emulated/0/DCIM"
        val localFolderPath = "/storage/emulated/0/Folder"
        whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
            localDCIMFolderPath
        )
        whenever(getFolderPairsUseCase.invoke()).thenReturn(
            listOf(
                FolderPair(
                    id = 1234L,
                    syncType = SyncType.TYPE_TWOWAY,
                    pairName = "Pair Name",
                    localFolderPath = localFolderPath,
                    remoteFolder = RemoteFolder(NodeId(5678L), "Remote Folder"),
                    syncStatus = SyncStatus.SYNCED,
                )
            )
        )
        whenever(localFolderUri.path).thenReturn(localFolderPath)
        whenever(localFolderUri.scheme).thenReturn("file")

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

        with(underTest) {
            state.test {
                val result = (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                assertThat(result).isEqualTo(sharedR.string.sync_local_device_folder_currently_synced_message)
            }
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that snackbar with warning message is displayed if try to select an already backed up local device folder`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)
        val localFolderUri: Uri = mock()
        val localDCIMFolderPath = "/storage/emulated/0/DCIM"
        val localFolderPath = "/storage/emulated/0/Folder"
        whenever(getLocalDCIMFolderPathUseCase.invoke()).thenReturn(
            localDCIMFolderPath
        )
        whenever(getFolderPairsUseCase.invoke()).thenReturn(
            listOf(
                FolderPair(
                    id = 1234L,
                    syncType = SyncType.TYPE_BACKUP,
                    pairName = "Pair Name",
                    localFolderPath = localFolderPath,
                    remoteFolder = RemoteFolder(NodeId(5678L), "Remote Folder"),
                    syncStatus = SyncStatus.SYNCED,
                )
            )
        )
        whenever(localFolderUri.path).thenReturn(localFolderPath)
        whenever(localFolderUri.scheme).thenReturn("file")

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

        with(underTest) {
            state.test {
                val result = (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                assertThat(result).isEqualTo(sharedR.string.sync_local_device_folder_currently_backed_up_message)
            }
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that when mega folder is updated state is also updated`(syncType: SyncType) =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(123L), "someFolder")
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            initViewModel(syncType = syncType)
            val expectedState = SyncNewFolderState(
                syncType = syncType,
                selectedMegaFolder = remoteFolder,
            )

            assertThat(expectedState).isEqualTo(underTest.state.value)
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click creates new folder pair and navigates to next screen`(syncType: SyncType) =
        runTest {
            val remoteFolder = if (syncType == SyncType.TYPE_TWOWAY) {
                RemoteFolder(NodeId(123L), "someFolder")
            } else {
                RemoteFolder(NodeId(-1L), "")
            }
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(myBackupsFolderExistsUseCase()).thenReturn(true)
            whenever(
                syncFolderPairUseCase.invoke(
                    syncType = syncType,
                    name = if (syncType == SyncType.TYPE_TWOWAY) remoteFolder.name else null,
                    localPath = "",
                    remotePath = remoteFolder,
                )
            ).thenReturn(true)
            val state = SyncNewFolderState(
                syncType = syncType,
                deviceName = "Device Name",
                selectedMegaFolder = remoteFolder,
            )
            initViewModel(syncType = syncType)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            verify(syncFolderPairUseCase).invoke(
                syncType = syncType,
                name = if (syncType == SyncType.TYPE_TWOWAY) remoteFolder.name else null,
                localPath = state.selectedLocalFolder,
                remotePath = remoteFolder,
            )
            assertThat(underTest.state.value.openSyncListScreen).isEqualTo(triggered)
        }

    @Test
    fun `test that next click creates backups node, new folder pair and navigates to next screen`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(-1L), "")
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(myBackupsFolderExistsUseCase()).thenReturn(false)
            whenever(setMyBackupsFolderUseCase("Backups")).thenReturn(NodeId(9999L))
            whenever(
                syncFolderPairUseCase.invoke(
                    syncType = SyncType.TYPE_BACKUP,
                    name = null,
                    localPath = "",
                    remotePath = remoteFolder,
                )
            ).thenReturn(true)
            val state = SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
                selectedMegaFolder = remoteFolder,
            )
            initViewModel(syncType = SyncType.TYPE_BACKUP)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            verify(syncFolderPairUseCase).invoke(
                syncType = SyncType.TYPE_BACKUP,
                name = null,
                localPath = state.selectedLocalFolder,
                remotePath = remoteFolder,
            )
            assertThat(underTest.state.value.openSyncListScreen).isEqualTo(triggered)
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click shows error when storage is over quota`(syncType: SyncType) =
        runTest {
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                awaitCancellation()
            })
            whenever(isStorageOverQuotaUseCase()).thenReturn(true)
            initViewModel(syncType = syncType)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            assertThat(underTest.state.value.showStorageOverQuota).isEqualTo(true)
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that storage over quota shown resets showStorageOverQuota event`(syncType: SyncType) {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        initViewModel(syncType = syncType)

        underTest.handleAction(SyncNewFolderAction.StorageOverquotaShown)

        assertThat(underTest.state.value.showStorageOverQuota).isEqualTo(false)
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that sync list screen opened resets openSyncListScreen event`(syncType: SyncType) {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        initViewModel(syncType = syncType)

        underTest.handleAction(SyncNewFolderAction.SyncListScreenOpened)

        assertThat(underTest.state.value.openSyncListScreen).isEqualTo(consumed)
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that clear selected mega folder use case is called when view model is initiated`(
        syncType: SyncType,
    ) {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })

        initViewModel(syncType = syncType)

        verify(clearSelectedMegaFolderUseCase).invoke()
    }

    @Test
    fun `test that selected mega folder is set when view model is initiated with remote folder parameters`(
    ) = runTest {

        val remoteFolder = RemoteFolder(NodeId(123L), "someFolder")
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })

        initViewModel(
            syncType = SyncType.TYPE_TWOWAY,
            remoteFolderHandle = remoteFolder.id,
            remoteFolderName = remoteFolder.name,
        )

        with(underTest) {
            state.test {
                val result = awaitItem().selectedMegaFolder as RemoteFolder
                assertThat(result).isEqualTo(remoteFolder)
            }
        }
    }

    @Test
    fun `test that selected mega folder is not set when view model is not initiated with remote folder parameters`(
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            awaitCancellation()
        })

        initViewModel(
            syncType = SyncType.TYPE_TWOWAY
        )

        with(underTest) {
            state.test {
                val result = awaitItem().selectedMegaFolder
                assertThat(result).isEqualTo(null)
            }
        }
    }

    @Test
    fun `test that next click will display the rename and create backup dialog if backup name already exists`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(-1L), "")
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(myBackupsFolderExistsUseCase()).thenReturn(false)
            whenever(setMyBackupsFolderUseCase("Backups")).thenReturn(NodeId(9999L))
            whenever(
                syncFolderPairUseCase.invoke(
                    syncType = SyncType.TYPE_BACKUP,
                    name = null,
                    localPath = "",
                    remotePath = remoteFolder,
                )
            ).thenThrow(BackupAlreadyExistsException())
            val state = SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
                selectedMegaFolder = remoteFolder,
            )
            initViewModel(syncType = SyncType.TYPE_BACKUP)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            verify(syncFolderPairUseCase).invoke(
                syncType = SyncType.TYPE_BACKUP,
                name = null,
                localPath = state.selectedLocalFolder,
                remotePath = remoteFolder,
            )
            assertThat(underTest.state.value.showRenameAndCreateBackupDialog).isNotEqualTo(null)
            assertThat(underTest.state.value.openSyncListScreen).isEqualTo(consumed)
        }

    private fun initViewModel(
        syncType: SyncType,
        remoteFolderHandle: NodeId? = null,
        remoteFolderName: String? = null,
    ) {
        underTest = SyncNewFolderViewModel(
            syncType = syncType,
            remoteFolderHandle = remoteFolderHandle?.longValue,
            remoteFolderName = remoteFolderName,
            monitorSelectedMegaFolderUseCase = monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase = syncFolderPairUseCase,
            isStorageOverQuotaUseCase = isStorageOverQuotaUseCase,
            getLocalDCIMFolderPathUseCase = getLocalDCIMFolderPathUseCase,
            clearSelectedMegaFolderUseCase = clearSelectedMegaFolderUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            getDeviceNameUseCase = getDeviceNameUseCase,
            getFolderPairsUseCase = getFolderPairsUseCase,
            myBackupsFolderExistsUseCase = myBackupsFolderExistsUseCase,
            setMyBackupsFolderUseCase = setMyBackupsFolderUseCase,
        )
    }

    private fun syncTypeParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP),
    )
}
