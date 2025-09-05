package mega.privacy.android.feature.sync.ui.renamebackup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.exception.BackupAlreadyExistsException
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupViewModel
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/**
 * Test Class for [RenameAndCreateBackupViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class RenameAndCreateBackupViewModelTest {

    private lateinit var underTest: RenameAndCreateBackupViewModel

    private val syncFolderPairUseCase = mock<SyncFolderPairUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = RenameAndCreateBackupViewModel(syncFolderPairUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncFolderPairUseCase)
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.errorMessage).isNull()
            assertThat(initialState.successEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that the error message is cleared`() = runTest {
        underTest.clearErrorMessage()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `test that rename and create the backup fails when the new backup name is empty`() =
        runTest {
            underTest.renameAndCreateBackup(
                newBackupName = "",
                localPath = "Local Path",
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(sharedR.string.sync_rename_and_create_backup_dialog_error_message_empty_backup_name)
            }
        }

    @ParameterizedTest(name = "new backup name: {0}")
    @ValueSource(
        strings = [
            "Backup\"", "Backup*", "Backup/", "Backup:", "Backup<", "Backup>", "Backup?",
            "Backup\\", "Backup|",
        ]
    )
    fun `test that rename and create the backup fails when the new backup name contains invalid characters`(
        newBackupName: String,
    ) = runTest {
        underTest.renameAndCreateBackup(
            newBackupName = newBackupName,
            localPath = "Local Path",
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isEqualTo(sharedR.string.general_invalid_characters_defined)
        }
    }

    @Test
    fun `test that rename and create the backup fails if the backup name already exists`() =
        runTest {
            whenever(
                syncFolderPairUseCase(
                    syncType = SyncType.TYPE_BACKUP,
                    name = "Backup name",
                    localPath = "Local Path",
                    remotePath = RemoteFolder(NodeId(-1L), ""),
                )
            ).thenThrow(BackupAlreadyExistsException())
            underTest.renameAndCreateBackup(
                newBackupName = "Backup name",
                localPath = "Local Path",
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(sharedR.string.sync_rename_and_create_backup_dialog_error_message_name_already_exists)
                assertThat(state.successEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that rename and create the backup is successful`() = runTest {
        whenever(
            syncFolderPairUseCase(
                syncType = SyncType.TYPE_BACKUP,
                name = "Backup name",
                localPath = "Local Path",
                remotePath = RemoteFolder(NodeId(-1L), ""),
            )
        ).thenReturn(true)
        underTest.renameAndCreateBackup(
            newBackupName = "Backup name",
            localPath = "Local Path",
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.successEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that the success event has been consumed`() = runTest {
        underTest.resetSuccessfulEvent()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.successEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that multiple invocations of renameAndCreateBackup do not launch new jobs until previous job finishes`() =
        runTest {
            // Mock the use case to delay execution to simulate a long-running operation
            whenever(
                syncFolderPairUseCase(
                    syncType = SyncType.TYPE_BACKUP,
                    name = "Backup name",
                    localPath = "Local Path",
                    remotePath = RemoteFolder(NodeId(-1L), ""),
                )
            ).doSuspendableAnswer {
                delay(100) // Simulate delay
                true
            }

            // Launch first job
            underTest.renameAndCreateBackup(
                newBackupName = "Backup name",
                localPath = "Local Path",
            )

            // Immediately launch second job (should be ignored)
            underTest.renameAndCreateBackup(
                newBackupName = "Backup name 2",
                localPath = "Local Path 2",
            )

            // Launch third job (should also be ignored)
            underTest.renameAndCreateBackup(
                newBackupName = "Backup name 3",
                localPath = "Local Path 3",
            )
            advanceUntilIdle()

            // Wait for the first job to complete
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isNull()
                assertThat(state.successEvent).isEqualTo(triggered)
            }

            // Verify only one invocation occurred
            verify(syncFolderPairUseCase).invoke(
                syncType = SyncType.TYPE_BACKUP,
                name = "Backup name",
                localPath = "Local Path",
                remotePath = RemoteFolder(NodeId(-1L), "")
            )
            verifyNoMoreInteractions(syncFolderPairUseCase)
        }
}
