package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfo
import nz.mega.sdk.MegaBackupInfo
import nz.mega.sdk.MegaBackupInfoList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [BackupInfoListMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoListMapperTest {

    private lateinit var underTest: BackupInfoListMapper

    private val backupInfoMapper = mock<BackupInfoMapper>()

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoListMapper(backupInfoMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupInfoMapper)
    }

    @Test
    fun `test that the mapping is correct`() {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }
        val backupInfoList = mutableListOf<BackupInfo>()

        (0 until backupSize).forEach { index ->
            val megaBackupInfo = mock<MegaBackupInfo> { on { id() }.thenReturn(index + 1000L) }
            val backupInfo = mock<BackupInfo> { on { id }.thenReturn(index + 1000L) }
            whenever(megaBackupInfoList.get(index)).thenReturn(megaBackupInfo)
            whenever(backupInfoMapper(megaBackupInfo)).thenReturn(backupInfo)
            backupInfoList.add(backupInfo)
        }
        assertThat(underTest(megaBackupInfoList)).isEqualTo(backupInfoList)
    }

    @ParameterizedTest(name = "backup size: {0}")
    @ValueSource(longs = [0L, -1L])
    fun `test that an empty backup info list is returned`(backupSize: Long) {
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }
        assertThat(underTest(megaBackupInfoList)).isEmpty()
    }

    @Test
    fun `test that an empty backup info list is returned if mega backup info list is null`() {
        assertThat(underTest(null)).isEmpty()
    }
}