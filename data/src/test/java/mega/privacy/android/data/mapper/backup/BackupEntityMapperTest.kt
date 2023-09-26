package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
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
internal class BackupEntityMapperTest {

    private lateinit var underTest: BackupEntityMapper

    private val encryptData: EncryptData = mock()
    private val backupStateIntMapper: BackupStateIntMapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = BackupEntityMapper(encryptData, backupStateIntMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(encryptData, backupStateIntMapper)
    }


    @Test
    fun `test that mapper returns entity correctly when invoked`() = runTest {
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
        val model = Backup(
            id = 1,
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

        val expected = BackupEntity(
            id = 1,
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
        whenever(encryptData(backupId.toString())).thenReturn(backupId.toString())
        whenever(encryptData(backupType.toString())).thenReturn(backupType.toString())
        whenever(encryptData(targetNode.toString())).thenReturn(targetNode.toString())
        whenever(encryptData(localFolder)).thenReturn(localFolder)
        whenever(encryptData(backupName)).thenReturn(backupName)
        whenever(encryptData(state.value.toString())).thenReturn(state.value.toString())
        whenever(encryptData(subState.toString())).thenReturn(subState.toString())
        whenever(encryptData(extraData)).thenReturn(extraData)
        whenever(encryptData(startTimestamp.toString())).thenReturn(startTimestamp.toString())
        whenever(encryptData(lastFinishTimestamp.toString())).thenReturn(lastFinishTimestamp.toString())
        whenever(encryptData(targetFolderPath)).thenReturn(targetFolderPath)
        whenever(encryptData(isExcludeSubFolders.toString())).thenReturn(isExcludeSubFolders.toString())
        whenever(encryptData(isDeleteEmptySubFolders.toString())).thenReturn(isDeleteEmptySubFolders.toString())
        whenever(encryptData(outdated.toString())).thenReturn(outdated.toString())
        whenever(backupStateIntMapper(BackupState.ACTIVE)).thenReturn(BackupState.ACTIVE.value)
        val actual = underTest(model)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
