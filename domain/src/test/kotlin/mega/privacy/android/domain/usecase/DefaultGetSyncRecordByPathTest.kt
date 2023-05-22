package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetSyncRecordByPathTest {
    private lateinit var underTest: GetSyncRecordByPath

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetSyncRecordByPath(cameraUploadRepository = cameraUploadRepository)
    }

    @Test
    fun `test that local path is returned if new path is null`() = runTest {
        val expected = SyncRecord(
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
            type = SyncRecordType.TYPE_PHOTO,
            nodeHandle = null,
            isCopyOnly = false,
            isSecondary = false,
        )
        cameraUploadRepository.stub {
            onBlocking { getSyncRecordByNewPath(any()) }.thenReturn(null)
            onBlocking { getSyncRecordByLocalPath(any(), any()) }.thenReturn(expected)
        }

        assertThat(underTest("", false)).isEqualTo(expected)
    }
}
