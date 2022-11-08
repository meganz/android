package test.mega.privacy.android.app.domain.usecase

import android.provider.MediaStore
import com.google.common.truth.Truth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.GetPendingUploadList
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.ProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.SaveSyncRecordsToDB
import mega.privacy.android.app.sync.BackupState
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProcessMediaForUploadTest {
    private lateinit var underTest: ProcessMediaForUpload

    private val getSyncFileUploadUris = mock<GetSyncFileUploadUris>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val updateTimeStamp = mock<UpdateCameraUploadTimeStamp>()
    private val getPendingUploadList = mock<GetPendingUploadList>()
    private val saveSyncRecordsToDB = mock<SaveSyncRecordsToDB>()
    private val cameraUploadSyncManagerWrapper = mock<CameraUploadSyncManagerWrapper>()

    @Before
    fun setUp() {
        underTest = DefaultProcessMediaForUpload(
            cameraUploadRepository = mock(),
            getSyncFileUploadUris = getSyncFileUploadUris,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            selectionQuery = mock(),
            localPath = mock(),
            localPathSecondary = mock(),
            updateTimeStamp = updateTimeStamp,
            getPendingUploadList = getPendingUploadList,
            saveSyncRecordsToDB = saveSyncRecordsToDB,
            mediaStoreFileTypeMapper = mock(),
            cameraUploadSyncManagerWrapper = cameraUploadSyncManagerWrapper
        )
    }

    @Test
    fun `primary photos are prepared when video upload is disabled`() = runTest {
        whenever(getSyncFileUploadUris.invoke()).thenReturn(listOf(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ))
        whenever(isSecondaryFolderEnabled.invoke()).thenReturn(false)
        val inOrder = inOrder(updateTimeStamp, cameraUploadSyncManagerWrapper)
        underTest(null, null, null)
        inOrder.verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_PHOTO)
        inOrder.verify(cameraUploadSyncManagerWrapper, Times(1))
            .updatePrimaryFolderBackupState(BackupState.ACTIVE)
    }

    @Test
    fun `primary and secondary photos are prepared when secondary folder is enabled`() = runTest {
        whenever(getSyncFileUploadUris.invoke()).thenReturn(listOf(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ))
        whenever(isSecondaryFolderEnabled.invoke()).thenReturn(true)
        underTest(null, null, null)
        val inOrder = inOrder(updateTimeStamp, cameraUploadSyncManagerWrapper)
        inOrder.verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_PHOTO)
        inOrder.verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.SECONDARY_PHOTO)
        inOrder.verify(cameraUploadSyncManagerWrapper, Times(1))
            .updateSecondaryFolderBackupState(BackupState.ACTIVE)
    }

    @Test
    fun `both primary photos and videos are prepared when video upload is enabled`() = runTest {
        whenever(getSyncFileUploadUris.invoke()).thenReturn(listOf(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.INTERNAL_CONTENT_URI,
        ))
        whenever(isSecondaryFolderEnabled.invoke()).thenReturn(false)
        underTest(null, null, null)
        verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_PHOTO)
        verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_VIDEO)
    }

    @Test
    fun `both primary and secondary photos and videos are prepared when secondary folder upload is enabled`() =
        runTest {
            whenever(getSyncFileUploadUris.invoke()).thenReturn(listOf(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.INTERNAL_CONTENT_URI,
            ))
            whenever(isSecondaryFolderEnabled.invoke()).thenReturn(true)
            underTest(null, null, null)
            verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_PHOTO)
            verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.PRIMARY_VIDEO)
            verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.SECONDARY_PHOTO)
            verify(updateTimeStamp, Times(1)).invoke(null, SyncTimeStamp.SECONDARY_VIDEO)
        }

    @Test
    fun `cancellation exception is thrown when the operation is cancelled`() =
        runTest {
            val job = launch {
                whenever(getSyncFileUploadUris.invoke()).thenReturn(listOf(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                ))
                getPendingUploadList.stub {
                    onBlocking { invoke(any(), any(), any()) }.doSuspendableAnswer {
                        delay(2000)
                        return@doSuspendableAnswer emptyList()
                    }
                }
                isSecondaryFolderEnabled.stub {
                    onBlocking { invoke() }.doSuspendableAnswer {
                        delay(3000)
                        return@doSuspendableAnswer true
                    }
                }
                try {
                    underTest(null, null, null)
                } catch (e: Exception) {
                    Truth.assertThat(e).isInstanceOf(CancellationException::class.java)
                }
                verify(saveSyncRecordsToDB, Times(0)).invoke(any(), any(), any(), any())
                verify(saveSyncRecordsToDB, Times(0)).invoke(any(), any(), any(), any())
                verify(cameraUploadSyncManagerWrapper, Times(0)).updatePrimaryFolderBackupState(
                    BackupState.ACTIVE)
                verify(cameraUploadSyncManagerWrapper, Times(0)).updateSecondaryFolderBackupState(
                    BackupState.ACTIVE)
            }
            runCurrent()
            advanceTimeBy(3000)
            job.cancelAndJoin()
        }
}
