package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUpdateCameraUploadTimeStampTest {
    private lateinit var underTest: UpdateCameraUploadTimeStamp

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()

    @Before
    fun setUp() {
        underTest =
            DefaultUpdateCameraUploadTimeStamp(
                cameraUploadRepository = cameraUploadRepository,
                isSecondaryFolderEnabled = isSecondaryFolderEnabled
            )
    }

    @Test
    fun `test that new timestamp is set if none exists`() = runTest {
        val timestamp = 100L
        cameraUploadRepository.stub {
            onBlocking { getSyncTimeStamp(any()) }.thenReturn(null)
        }
        isSecondaryFolderEnabled.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        SyncTimeStamp.values()
            .forEach { syncTimeStampType ->
                underTest(timestamp, syncTimeStampType)
                verify(cameraUploadRepository).setSyncTimeStamp(timestamp, syncTimeStampType)
            }
    }

    @Test
    fun `test that new timestamp later than current timestamp replaces existing value`() = runTest {
        val timestamp = 100L
        cameraUploadRepository.stub {
            onBlocking { getSyncTimeStamp(any()) }.thenReturn(timestamp - 1)
        }
        isSecondaryFolderEnabled.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        SyncTimeStamp.values()
            .forEach { syncTimeStampType ->
                underTest(timestamp, syncTimeStampType)
                verify(cameraUploadRepository).setSyncTimeStamp(timestamp, syncTimeStampType)
            }
    }

    @Test
    fun `test that secondary timestamps are not set if secondary folder is not enabled`() =
        runTest {
            val timestamp = 100L
            cameraUploadRepository.stub {
                onBlocking { getSyncTimeStamp(any()) }.thenReturn(timestamp - 1)
            }
            isSecondaryFolderEnabled.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            SyncTimeStamp.values()
                .forEach { syncTimeStampType ->
                    underTest(timestamp, syncTimeStampType)
                }
            verify(cameraUploadRepository).setSyncTimeStamp(timestamp, SyncTimeStamp.PRIMARY_PHOTO)
            verify(cameraUploadRepository).setSyncTimeStamp(timestamp, SyncTimeStamp.PRIMARY_VIDEO)
            verify(cameraUploadRepository, never()).setSyncTimeStamp(
                timestamp,
                SyncTimeStamp.SECONDARY_PHOTO
            )
            verify(cameraUploadRepository, never()).setSyncTimeStamp(
                timestamp,
                SyncTimeStamp.SECONDARY_VIDEO
            )
        }

    @Test
    fun `test that current max time for the type is used if no timestamp is passed`() = runTest {
        isSecondaryFolderEnabled.stub {
            onBlocking { invoke() }.thenReturn(true)
        }
        val timestamp = 100L
        val testParameters = mapOf(
            SyncTimeStamp.PRIMARY_PHOTO to {
                setExpectationsForSpecificMaxValueOnly(
                    timestamp,
                    false,
                    SyncRecordType.TYPE_PHOTO
                )
            },
            SyncTimeStamp.PRIMARY_VIDEO to {
                setExpectationsForSpecificMaxValueOnly(
                    timestamp,
                    false,
                    SyncRecordType.TYPE_VIDEO
                )
            },
            SyncTimeStamp.SECONDARY_PHOTO to {
                setExpectationsForSpecificMaxValueOnly(
                    timestamp,
                    true,
                    SyncRecordType.TYPE_PHOTO
                )
            },
            SyncTimeStamp.SECONDARY_VIDEO to {
                setExpectationsForSpecificMaxValueOnly(
                    timestamp,
                    true,
                    SyncRecordType.TYPE_VIDEO
                )
            },
        )
        testParameters.forEach { (type, setExpectation) ->
            setExpectation()
            underTest(null, type)
            verify(cameraUploadRepository).setSyncTimeStamp(timestamp, type)
        }
    }

    private fun setExpectationsForSpecificMaxValueOnly(
        timestamp: Long,
        isSecondary: Boolean,
        syncRecordType: SyncRecordType,
    ) {
        cameraUploadRepository.stub {
            onBlocking { getSyncTimeStamp(any()) }.thenReturn(timestamp - 1)
            onBlocking { getMaxTimestamp(any(), any()) }.thenReturn(0)
            onBlocking { getMaxTimestamp(isSecondary, syncRecordType) }.thenReturn(timestamp)
        }
    }
}
