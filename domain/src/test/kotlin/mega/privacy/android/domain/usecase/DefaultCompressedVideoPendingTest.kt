package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [DefaultCompressedVideoPending]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCompressedVideoPendingTest {
    private lateinit var underTest: CompressedVideoPending

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getUploadVideoQuality = mock<GetUploadVideoQualityUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultCompressedVideoPending(
            cameraUploadRepository = cameraUploadRepository,
            getUploadVideoQualityUseCase = getUploadVideoQuality,
        )
    }

    @Test
    fun `test that false is returned if upload quality is set to original`() = runTest {
        getUploadVideoQuality.stub {
            onBlocking { invoke() }.thenReturn(VideoQuality.ORIGINAL)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that false is returned if upload quality is set to original but to compress sync records are empty`() =
        runTest {
            getUploadVideoQuality.stub {
                onBlocking { invoke() }.thenReturn(VideoQuality.LOW)
            }
            cameraUploadRepository.stub {
                onBlocking { getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS) }.thenReturn(
                    emptyList()
                )
            }

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that true is returned if upload quality is not original and to compress list is not empty`() =
        runTest {
            val record = SyncRecord(
                id = 0,
                localPath = "path",
                newPath = null,
                originFingerprint = null,
                newFingerprint = null,
                timestamp = 0L,
                fileName = "fileName.mp4",
                longitude = null,
                latitude = null,
                status = 0,
                type = SyncRecordType.TYPE_VIDEO,
                nodeHandle = null,
                isCopyOnly = false,
                isSecondary = false,
            )
            getUploadVideoQuality.stub {
                onBlocking { invoke() }.thenReturn(VideoQuality.LOW)
            }
            cameraUploadRepository.stub {
                onBlocking { getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS) }.thenReturn(
                    listOf(record)
                )
            }

            assertThat(underTest()).isTrue()
        }
}
