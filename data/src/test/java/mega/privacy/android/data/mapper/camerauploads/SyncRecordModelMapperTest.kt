package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncRecordModelMapperTest {

    private lateinit var underTest: SyncRecordModelMapper

    private val decryptData: DecryptData = mock()
    private val syncRecordTypeMapper: SyncRecordTypeMapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = SyncRecordModelMapper(decryptData, syncRecordTypeMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            decryptData,
            syncRecordTypeMapper,
        )
    }
    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = SyncRecordEntity(
            id = 0,
            originalPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_1.jpg",
            newPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            originalFingerPrint = "adlkfjalsdkfj",
            newFingerprint = "adlkfjalsdkfjsdf",
            timestamp = "1684228012974",
            fileName = "2023-07-25 00.13.20_1.jpg",
            longitude = "1.684228E7",
            latitude = "1.684228E7",
            state = 3,
            type = 2,
            nodeHandle = "11622336899311",
            isCopyOnly = "false",
            isSecondary = "false",
        )
        val expected = SyncRecord(
            localPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_1.jpg",
            newPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            originFingerprint = "adlkfjalsdkfj",
            newFingerprint = "adlkfjalsdkfjsdf",
            timestamp = 1684228012974L,
            fileName = "2023-07-25 00.13.20_1.jpg",
            longitude = 16842280.0,
            latitude = 16842280.0,
            status = 3,
            type = SyncRecordType.TYPE_VIDEO,
            nodeHandle = 11622336899311,
            isCopyOnly = false,
            isSecondary = false,
        )
        whenever(decryptData(entity.originalPath)).thenReturn(entity.originalPath)
        whenever(decryptData(entity.newPath)).thenReturn(entity.newPath)
        whenever(decryptData(entity.originalFingerPrint)).thenReturn(entity.originalFingerPrint)
        whenever(decryptData(entity.newFingerprint)).thenReturn(entity.newFingerprint)
        whenever(decryptData(entity.timestamp.toString())).thenReturn(entity.timestamp.toString())
        whenever(decryptData(entity.fileName)).thenReturn(entity.fileName)
        whenever(decryptData(entity.longitude.toString())).thenReturn(entity.longitude.toString())
        whenever(decryptData(entity.latitude.toString())).thenReturn(entity.latitude.toString())
        whenever(decryptData(entity.nodeHandle.toString())).thenReturn(entity.nodeHandle.toString())
        whenever(decryptData(entity.isCopyOnly.toString())).thenReturn(entity.isCopyOnly.toString())
        whenever(decryptData(entity.isSecondary.toString())).thenReturn(entity.isSecondary.toString())
        whenever(syncRecordTypeMapper(2)).thenReturn(SyncRecordType.TYPE_VIDEO)
        val actual = underTest(entity)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
