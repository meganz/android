package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncRecordEntityMapperTest {

    private lateinit var underTest: SyncRecordEntityMapper

    private val encryptData: EncryptData = mock()
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = SyncRecordEntityMapper(encryptData, syncRecordTypeIntMapper)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val model = SyncRecord(
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

        val expected = SyncRecordEntity(
            id = null,
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
        whenever(encryptData(model.localPath)).thenReturn(model.localPath)
        whenever(encryptData(model.newPath)).thenReturn(model.newPath)
        whenever(encryptData(model.originFingerprint)).thenReturn(model.originFingerprint)
        whenever(encryptData(model.newFingerprint)).thenReturn(model.newFingerprint)
        whenever(encryptData(model.timestamp.toString())).thenReturn(model.timestamp.toString())
        whenever(encryptData(model.fileName)).thenReturn(model.fileName)
        whenever(encryptData(model.longitude.toString())).thenReturn(model.longitude.toString())
        whenever(encryptData(model.latitude.toString())).thenReturn(model.latitude.toString())
        whenever(encryptData(model.nodeHandle.toString())).thenReturn(model.nodeHandle.toString())
        whenever(encryptData(model.isCopyOnly.toString())).thenReturn(model.isCopyOnly.toString())
        whenever(encryptData(model.isSecondary.toString())).thenReturn(model.isSecondary.toString())
        whenever(syncRecordTypeIntMapper(SyncRecordType.TYPE_VIDEO)).thenReturn(2)
        val actual = underTest(model)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
