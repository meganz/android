package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCompressedVideoPendingTest {
    private lateinit var underTest: CompressedVideoPending

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultCompressedVideoPending(cameraUploadRepository = cameraUploadRepository)
    }

    @Test
    fun `test that false is returned if upload quality is set to original`() = runTest {
        cameraUploadRepository.stub {
            onBlocking { getUploadVideoQuality() }.thenReturn(VideoQuality.ORIGINAL)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that false is returned if upload quality is set to original but to compress sync records are empty`() =
        runTest {
            cameraUploadRepository.stub {
                onBlocking { getUploadVideoQuality() }.thenReturn(VideoQuality.LOW)
                onBlocking { getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS) }.thenReturn(
                    emptyList())
            }

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that true is returned if upload quality is not original and to compress list is not empty`() =
        runTest {
            val record = SyncRecord(
                id = 0,
                localPath = null,
                newPath = null,
                originFingerprint = null,
                newFingerprint = null,
                timestamp = null,
                fileName = null,
                longitude = null,
                latitude = null,
                status = 0,
                type = 0,
                nodeHandle = null,
                isCopyOnly = false,
                isSecondary = false,
            )
            cameraUploadRepository.stub {
                onBlocking { getUploadVideoQuality() }.thenReturn(VideoQuality.LOW)
                onBlocking { getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS) }.thenReturn(
                    listOf(record))
            }

            assertThat(underTest()).isTrue()
        }
}