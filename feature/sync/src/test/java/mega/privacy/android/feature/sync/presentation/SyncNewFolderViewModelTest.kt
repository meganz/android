package mega.privacy.android.feature.sync.presentation

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.exception.BackupAlreadyExistsException
import mega.privacy.android.feature.sync.domain.usecase.backup.MyBackupsFolderExistsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.PreSeedSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.backup.SetMyBackupsFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncRemoteFolderValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncValidityResult
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderState
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
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
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val getDeviceNameUseCase: GetDeviceNameUseCase = mock()
    private val myBackupsFolderExistsUseCase: MyBackupsFolderExistsUseCase = mock()
    private val setMyBackupsFolderUseCase: SetMyBackupsFolderUseCase = mock()
    private val syncUriValidityMapper: SyncUriValidityMapper = mock()
    private val syncRemoteFolderValidityMapper: SyncRemoteFolderValidityMapper = mock()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val preSeedSyncExclusionsUseCase: PreSeedSyncExclusionsUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private lateinit var underTest: SyncNewFolderViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase,
            isStorageOverQuotaUseCase,
            clearSelectedMegaFolderUseCase,
            getDeviceIdUseCase,
            getDeviceNameUseCase,
            myBackupsFolderExistsUseCase,
            setMyBackupsFolderUseCase,
            syncUriValidityMapper,
            syncRemoteFolderValidityMapper,
            monitorAccountDetailUseCase,
            preSeedSyncExclusionsUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that local folder selected action results in updated state`(syncType: SyncType) =
        runTest {
            val folderName = "Photos"
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
            val localFolderUri = "content://storage/emulated/0/Photos"
            initViewModel(syncType = syncType)
            val documentFile: DocumentFile = mock()
            val uri: Uri = mock()
            whenever(uri.toString()).thenReturn(localFolderUri)
            whenever(documentFile.uri).thenReturn(uri)
            val expectedState = SyncNewFolderState(
                syncType = syncType,
                deviceName = "Device Name",
                selectedLocalFolder = localFolderUri,
                selectedFolderName = folderName
            )
            whenever(syncUriValidityMapper(localFolderUri)).thenReturn(
                SyncValidityResult.ValidFolderSelected(
                    localFolderUri = UriPath(localFolderUri),
                    folderName = folderName
                )
            )

            underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(documentFile))
            underTest.state.test {
                val result = awaitItem()
                assertThat(result.selectedLocalFolder).isEqualTo(expectedState.selectedLocalFolder)
                assertThat(result.selectedFolderName).isEqualTo(expectedState.selectedFolderName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that snackbar with warning message is displayed if try to select DCIM as local device folder`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        val localFolderUri = "content://storage/emulated/0/Photos"
        initViewModel(syncType = syncType)
        val documentFile: DocumentFile = mock()
        val uri: Uri = mock()
        whenever(uri.toString()).thenReturn(localFolderUri)
        whenever(documentFile.uri).thenReturn(uri)
        whenever(syncUriValidityMapper(localFolderUri)).thenReturn(
            SyncValidityResult.ShowSnackbar(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
        )

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(documentFile))

        with(underTest) {
            state.test {
                val result = (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                assertThat(result).isEqualTo(
                    LocalizedText.StringRes(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
                )
            }
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that snackbar with warning message is displayed if try to select an already synced or backed up local device folder`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        val localFolderUri = "content://storage/emulated/0/Photos"
        initViewModel(syncType = syncType)
        val documentFile: DocumentFile = mock()
        val uri: Uri = mock()
        whenever(uri.toString()).thenReturn(localFolderUri)
        whenever(documentFile.uri).thenReturn(uri)
        val snackbarMessage = when (syncType) {
            SyncType.TYPE_BACKUP -> sharedR.string.sync_local_device_folder_currently_backed_up_message
            else -> sharedR.string.sync_local_device_folder_currently_synced_message
        }
        whenever(syncUriValidityMapper(localFolderUri)).thenReturn(
            SyncValidityResult.ShowSnackbar(snackbarMessage)
        )
        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(documentFile))

        with(underTest) {
            state.test {
                val result = (awaitItem().showSnackbar as StateEventWithContentTriggered).content
                assertThat(result).isEqualTo(LocalizedText.StringRes(snackbarMessage))
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
            // Mock remote folder validation for non-BACKUP sync types
            if (syncType == SyncType.TYPE_TWOWAY) {
                whenever(
                    syncRemoteFolderValidityMapper(
                        nodeId = remoteFolder.id,
                        remoteFolderDisplayName = remoteFolder.name,
                    )
                ).thenReturn(
                    SyncValidityResult.ValidFolderSelected(
                        localFolderUri = UriPath(""),
                        folderName = "folder"
                    )
                )
            }
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

    @Test
    fun `test that storage over quota status is updated when account details change`() = runTest {
        val accountDetails = mock<AccountDetail>()
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetails))
        whenever(isStorageOverQuotaUseCase()).thenReturn(true)

        initViewModel(syncType = SyncType.TYPE_TWOWAY)

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.isStorageOverQuota).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that storage over quota status is false when account has sufficient storage`() =
        runTest {
            val accountDetails = mock<AccountDetail>()
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetails))
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)

            initViewModel(syncType = SyncType.TYPE_TWOWAY)

            underTest.state.test {
                val result = awaitItem()
                assertThat(result.isStorageOverQuota).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that storage over quota status is checked on ViewModel initialization`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock()))
        whenever(isStorageOverQuotaUseCase()).thenReturn(true)

        initViewModel(syncType = SyncType.TYPE_TWOWAY)
        advanceUntilIdle()

        verify(isStorageOverQuotaUseCase).invoke() // once on account detail change
    }


    @Test
    fun `test that when sync type is TWO_WAY and remote folder validation returns ShowSnackbar, snackbar is displayed`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(123L), "someFolder")
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(
                syncRemoteFolderValidityMapper(
                    nodeId = remoteFolder.id,
                    remoteFolderDisplayName = remoteFolder.name,
                )
            ).thenReturn(
                SyncValidityResult.ShowSnackbar(sharedR.string.sync_local_device_folder_currently_synced_message)
            )

            initViewModel(syncType = SyncType.TYPE_TWOWAY)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            underTest.state.test {
                val result = awaitItem()
                assertThat(result.isLoading).isFalse()
                assertThat((result.showSnackbar as StateEventWithContentTriggered).content)
                    .isEqualTo(LocalizedText.StringRes(sharedR.string.sync_local_device_folder_currently_synced_message))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when sync type is TWO_WAY and remote folder validation returns Invalid, loading is stopped`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(123L), "someFolder")
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(
                syncRemoteFolderValidityMapper(
                    nodeId = remoteFolder.id,
                    remoteFolderDisplayName = remoteFolder.name,
                )
            ).thenReturn(
                SyncValidityResult.Invalid
            )

            initViewModel(syncType = SyncType.TYPE_TWOWAY)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            underTest.state.test {
                val result = awaitItem()
                assertThat(result.isLoading).isFalse()
                assertThat(result.openSyncListScreen).isEqualTo(consumed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when sync type is BACKUP, remote folder validation is skipped`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(-1L), "")
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)
            whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
                emit(remoteFolder)
                awaitCancellation()
            })
            whenever(myBackupsFolderExistsUseCase()).thenReturn(true)
            whenever(
                syncFolderPairUseCase.invoke(
                    syncType = SyncType.TYPE_BACKUP,
                    name = null,
                    localPath = "",
                    remotePath = remoteFolder,
                )
            ).thenReturn(true)

            initViewModel(syncType = SyncType.TYPE_BACKUP)

            underTest.handleAction(SyncNewFolderAction.NextClicked)

            verify(syncRemoteFolderValidityMapper, never()).invoke(
                any(),
                any(),
            )
            assertThat(underTest.state.value.openSyncListScreen).isEqualTo(triggered)
        }


    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click writes the staged exclusions before creating the folder pair`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        whenever(preSeedSyncExclusionsUseCase(any(), any())).thenReturn(Result.success(Unit))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        val inOrder = inOrder(preSeedSyncExclusionsUseCase, syncFolderPairUseCase)
        inOrder.verify(preSeedSyncExclusionsUseCase).invoke(UriPath(LOCAL_ROOT), stagedExclusions)
        inOrder.verify(syncFolderPairUseCase).invoke(
            eq(syncType),
            anyOrNull(),
            eq(LOCAL_ROOT),
            any(),
        )
    }

    @Test
    fun `test that next click writes the staged exclusions before creating the backup when the backups folder does not exist`() =
        runTest {
            stubFolderPairCreation(SyncType.TYPE_BACKUP)
            whenever(myBackupsFolderExistsUseCase()).thenReturn(false)
            whenever(setMyBackupsFolderUseCase(BACKUPS_FOLDER_NAME)).thenReturn(NodeId(9999L))
            whenever(preSeedSyncExclusionsUseCase(any(), any())).thenReturn(Result.success(Unit))
            initViewModel(syncType = SyncType.TYPE_BACKUP)
            selectLocalFolderAndStageExclusions()

            underTest.handleAction(SyncNewFolderAction.NextClicked)
            advanceUntilIdle()

            val inOrder = inOrder(preSeedSyncExclusionsUseCase, syncFolderPairUseCase)
            inOrder.verify(preSeedSyncExclusionsUseCase)
                .invoke(UriPath(LOCAL_ROOT), stagedExclusions)
            inOrder.verify(syncFolderPairUseCase).invoke(
                eq(SyncType.TYPE_BACKUP),
                anyOrNull(),
                eq(LOCAL_ROOT),
                any(),
            )
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not create the folder pair when the staged exclusions fail to write`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        whenever(preSeedSyncExclusionsUseCase(any(), any()))
            .thenReturn(Result.failure(RuntimeException("Write failed")))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        verify(syncFolderPairUseCase, never()).invoke(any(), anyOrNull(), any(), any())
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not open the sync list screen when the staged exclusions fail to write`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        whenever(preSeedSyncExclusionsUseCase(any(), any()))
            .thenReturn(Result.failure(RuntimeException("Write failed")))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.openSyncListScreen).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click shows a snackbar when the staged exclusions fail to write`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        whenever(preSeedSyncExclusionsUseCase(any(), any()))
            .thenReturn(Result.failure(RuntimeException("Write failed")))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.showSnackbar)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((result.showSnackbar as StateEventWithContentTriggered).content)
                .isEqualTo(
                    LocalizedText.StringRes(sharedR.string.general_sync_could_not_create_ignore_file)
                )
            assertThat(result.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that next click does not create the backup when the staged exclusions fail to write and the backups folder does not exist`() =
        runTest {
            stubFolderPairCreation(SyncType.TYPE_BACKUP)
            whenever(myBackupsFolderExistsUseCase()).thenReturn(false)
            whenever(setMyBackupsFolderUseCase(BACKUPS_FOLDER_NAME)).thenReturn(NodeId(9999L))
            whenever(preSeedSyncExclusionsUseCase(any(), any()))
                .thenReturn(Result.failure(RuntimeException("Write failed")))
            initViewModel(syncType = SyncType.TYPE_BACKUP)
            selectLocalFolderAndStageExclusions()

            underTest.handleAction(SyncNewFolderAction.NextClicked)
            advanceUntilIdle()

            verify(syncFolderPairUseCase, never()).invoke(any(), anyOrNull(), any(), any())
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not write exclusions when none are staged`(syncType: SyncType) =
        runTest {
            stubFolderPairCreation(syncType)
            initViewModel(syncType = syncType)

            underTest.handleAction(SyncNewFolderAction.NextClicked)
            advanceUntilIdle()

            verify(preSeedSyncExclusionsUseCase, never()).invoke(any(), any())
            verify(syncFolderPairUseCase).invoke(eq(syncType), anyOrNull(), any(), any())
        }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not write exclusions when the sync exclusions flag is off`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SyncExclusions)).thenReturn(false)

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        verify(preSeedSyncExclusionsUseCase, never()).invoke(any(), any())
        verify(syncFolderPairUseCase).invoke(eq(syncType), anyOrNull(), eq(LOCAL_ROOT), any())
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not write exclusions when the sync exclusions flag cannot be read`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SyncExclusions))
            .thenThrow(RuntimeException("Flag unreachable"))

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        verify(preSeedSyncExclusionsUseCase, never()).invoke(any(), any())
        verify(syncFolderPairUseCase).invoke(eq(syncType), anyOrNull(), eq(LOCAL_ROOT), any())
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that exclusions staged updates the state`(syncType: SyncType) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)

        underTest.handleAction(SyncNewFolderAction.ExclusionsStaged(stagedExclusions))
        advanceUntilIdle()

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.stagedExclusions).isEqualTo(stagedExclusions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that local folder selected discards the staged exclusions when a different folder is chosen`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        selectLocalFolder(OTHER_LOCAL_ROOT)

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.stagedExclusions).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that local folder selected keeps the staged exclusions when the same folder is chosen again`(
        syncType: SyncType,
    ) = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()

        selectLocalFolder(LOCAL_ROOT)

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.stagedExclusions).isEqualTo(stagedExclusions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that next click does not write exclusions when a different local folder was chosen after staging`(
        syncType: SyncType,
    ) = runTest {
        stubFolderPairCreation(syncType)
        initViewModel(syncType = syncType)
        selectLocalFolderAndStageExclusions()
        selectLocalFolder(OTHER_LOCAL_ROOT)

        underTest.handleAction(SyncNewFolderAction.NextClicked)
        advanceUntilIdle()

        verify(preSeedSyncExclusionsUseCase, never()).invoke(any(), any())
        verify(syncFolderPairUseCase).invoke(eq(syncType), anyOrNull(), eq(OTHER_LOCAL_ROOT), any())
    }

    private suspend fun stubFolderPairCreation(syncType: SyncType): RemoteFolder {
        val remoteFolder = if (syncType == SyncType.TYPE_BACKUP) {
            RemoteFolder(NodeId(-1L), "")
        } else {
            RemoteFolder(NodeId(123L), "someFolder")
        }
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(remoteFolder)
            awaitCancellation()
        })
        whenever(myBackupsFolderExistsUseCase()).thenReturn(true)
        whenever(
            syncRemoteFolderValidityMapper(
                nodeId = remoteFolder.id,
                remoteFolderDisplayName = remoteFolder.name,
            )
        ).thenReturn(
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(LOCAL_ROOT),
                folderName = "folder",
            )
        )
        whenever(syncFolderPairUseCase(any(), anyOrNull(), any(), any())).thenReturn(true)
        return remoteFolder
    }

    private suspend fun TestScope.selectLocalFolder(localFolderUri: String) {
        val documentFile: DocumentFile = mock()
        val uri: Uri = mock()
        whenever(uri.toString()).thenReturn(localFolderUri)
        whenever(documentFile.uri).thenReturn(uri)
        whenever(syncUriValidityMapper(localFolderUri)).thenReturn(
            SyncValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(localFolderUri),
                folderName = "Photos",
            )
        )
        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(documentFile))
        advanceUntilIdle()
    }

    private suspend fun TestScope.selectLocalFolderAndStageExclusions() {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SyncExclusions)).thenReturn(true)
        selectLocalFolder(LOCAL_ROOT)
        underTest.handleAction(SyncNewFolderAction.ExclusionsStaged(stagedExclusions))
        advanceUntilIdle()
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
            clearSelectedMegaFolderUseCase = clearSelectedMegaFolderUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            getDeviceNameUseCase = getDeviceNameUseCase,
            myBackupsFolderExistsUseCase = myBackupsFolderExistsUseCase,
            setMyBackupsFolderUseCase = setMyBackupsFolderUseCase,
            syncUriValidityMapper = syncUriValidityMapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            syncRemoteFolderValidityMapper = syncRemoteFolderValidityMapper,
            preSeedSyncExclusionsUseCase = preSeedSyncExclusionsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    private fun syncTypeParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP),
    )

    private companion object {
        const val LOCAL_ROOT = "content://com.android.externalstorage.documents/tree/primary%3APhotos"
        const val OTHER_LOCAL_ROOT = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
        const val BACKUPS_FOLDER_NAME = "Backups"

        val stagedExclusions = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES.first())),
        )
    }
}
