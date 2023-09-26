package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.mapper.camerauploads.BackupStateMapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupModelMapperTest {

    private lateinit var underTest: BackupModelMapper

    private val decryptData: DecryptData = mock()
    private val backupStateMapper: BackupStateMapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = BackupModelMapper(decryptData, backupStateMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(decryptData, backupStateMapper)
    }

    @Test
    fun `test that mapper returns model correctly when invoked`() = runTest {
        val backupId = 1234L
        val backupType = 3
        val targetNode = 4567L
        val localFolder = "path/to/local"
        val backupName = "Camera Uploads"
        val state = BackupState.ACTIVE
        val subState = 1
        val extraData = "extra_data"
        val startTimestamp = 1234567L
        val lastFinishTimestamp = 12345678L
        val targetFolderPath = "path/to/target"
        val isExcludeSubFolders = true
        val isDeleteEmptySubFolders = false
        val outdated = false

        val entity = BackupEntity(
            id = null,
            encryptedBackupId = backupId.toString(),
            backupType = backupType,
            encryptedTargetNode = targetNode.toString(),
            encryptedLocalFolder = localFolder,
            encryptedBackupName = backupName,
            state = state.value,
            subState = subState,
            encryptedExtraData = extraData,
            encryptedStartTimestamp = startTimestamp.toString(),
            encryptedLastFinishTimestamp = lastFinishTimestamp.toString(),
            encryptedTargetFolderPath = targetFolderPath,
            encryptedShouldExcludeSubFolders = isExcludeSubFolders.toString(),
            encryptedShouldDeleteEmptySubFolders = isDeleteEmptySubFolders.toString(),
            encryptedIsOutdated = outdated.toString()
        )

        val expected = Backup(
            backupId = backupId,
            backupType = backupType,
            targetNode = targetNode,
            localFolder = localFolder,
            backupName = backupName,
            state = state,
            subState = subState,
            extraData = extraData,
            startTimestamp = startTimestamp,
            lastFinishTimestamp = lastFinishTimestamp,
            isExcludeSubFolders = isExcludeSubFolders,
            isDeleteEmptySubFolders = isDeleteEmptySubFolders,
            outdated = outdated,
            targetFolderPath = targetFolderPath,
        )

        whenever(decryptData(backupId.toString())).thenReturn(backupId.toString())
        whenever(decryptData(backupType.toString())).thenReturn(backupType.toString())
        whenever(decryptData(targetNode.toString())).thenReturn(targetNode.toString())
        whenever(decryptData(localFolder)).thenReturn(localFolder)
        whenever(decryptData(backupName)).thenReturn(backupName)
        whenever(decryptData(state.value.toString())).thenReturn(state.value.toString())
        whenever(decryptData(subState.toString())).thenReturn(subState.toString())
        whenever(decryptData(extraData)).thenReturn(extraData)
        whenever(decryptData(startTimestamp.toString())).thenReturn(startTimestamp.toString())
        whenever(decryptData(lastFinishTimestamp.toString())).thenReturn(lastFinishTimestamp.toString())
        whenever(decryptData(targetFolderPath)).thenReturn(targetFolderPath)
        whenever(decryptData(isExcludeSubFolders.toString())).thenReturn(isExcludeSubFolders.toString())
        whenever(decryptData(isDeleteEmptySubFolders.toString())).thenReturn(isDeleteEmptySubFolders.toString())
        whenever(decryptData(outdated.toString())).thenReturn(outdated.toString())
        whenever(decryptData(BackupState.ACTIVE.value.toString())).thenReturn(BackupState.ACTIVE.value.toString())
        whenever(backupStateMapper(BackupState.ACTIVE.value)).thenReturn(BackupState.ACTIVE)
        val actual = underTest(entity)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
