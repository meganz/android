package mega.privacy.android.data.mapper.transfer.upload

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.pitag.PitagTriggerMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.TransferAppData
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaUploadOptions
import nz.mega.sdk.MegaUploadOptions.INVALID_CUSTOM_MOD_TIME
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaUploadOptionsMapperTest {

    private lateinit var underTest: MegaUploadOptionsMapper

    private val transferAppDataStringMapper = mock<TransferAppDataStringMapper>()
    private val pitagTriggerMapper = mock<PitagTriggerMapper>()
    private val megaUploadOptionsProvider = mock<MegaUploadOptionsProvider>()

    private val sourceTemporary = false
    private val shouldStartFirst = true
    private val pitagTrigger = PitagTrigger.SyncAlgorithm
    private val pitagTriggerChar = MegaApiJava.PITAG_TRIGGER_SYNC_ALGORITHM
    private val options = mock<MegaUploadOptions>()

    @BeforeAll
    fun setup() {
        underTest = MegaUploadOptionsMapper(
            transferAppDataStringMapper = transferAppDataStringMapper,
            pitagTriggerMapper = pitagTriggerMapper,
            megaUploadOptionsProvider = megaUploadOptionsProvider,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferAppDataStringMapper,
            pitagTriggerMapper,
            megaUploadOptionsProvider,
        )
    }

    @Test
    fun `test that maps correctly if nullable params are null`() {
        val expected = options.apply {
            this.mtime = INVALID_CUSTOM_MOD_TIME
            this.isSourceTemporary = sourceTemporary
            this.startFirst = shouldStartFirst
            pitagTrigger = pitagTriggerChar
        }

        whenever(megaUploadOptionsProvider()) doReturn options
        whenever(pitagTriggerMapper(pitagTrigger)) doReturn pitagTriggerChar

        val actual = underTest(
            fileName = null,
            mtime = null,
            appData = null,
            isSourceTemporary = sourceTemporary,
            startFirst = shouldStartFirst,
            pitagTrigger = pitagTrigger,
        )

        assertThat(actual?.fileName).isEqualTo(expected.fileName)
        assertThat(actual?.mtime).isEqualTo(expected.mtime)
        assertThat(actual?.appData).isEqualTo(expected.appData)
        assertThat(actual?.isSourceTemporary).isEqualTo(expected.isSourceTemporary)
        assertThat(actual?.startFirst).isEqualTo(expected.startFirst)
        assertThat(actual?.pitagTrigger).isEqualTo(expected.pitagTrigger)
    }

    @Test
    fun `test that maps correctly if nullable params have value`() {
        val mTime = 1234L
        val fileName = "fileName"
        val appData = listOf(TransferAppData.CameraUpload)
        val appDataString = "CU"
        val expected = options.apply {
            this.fileName = fileName
            this.mtime = mTime
            this.appData = appDataString
            this.isSourceTemporary = sourceTemporary
            this.startFirst = shouldStartFirst
            pitagTrigger = pitagTriggerChar
        }

        whenever(megaUploadOptionsProvider()) doReturn options
        whenever(pitagTriggerMapper(pitagTrigger)) doReturn pitagTriggerChar
        whenever(transferAppDataStringMapper(appData)) doReturn appDataString

        val actual = underTest(
            fileName = fileName,
            mtime = mTime,
            appData = appData,
            isSourceTemporary = sourceTemporary,
            startFirst = shouldStartFirst,
            pitagTrigger = pitagTrigger,
        )

        assertThat(actual?.fileName).isEqualTo(expected.fileName)
        assertThat(actual?.mtime).isEqualTo(expected.mtime)
        assertThat(actual?.appData).isEqualTo(expected.appData)
        assertThat(actual?.isSourceTemporary).isEqualTo(expected.isSourceTemporary)
        assertThat(actual?.startFirst).isEqualTo(expected.startFirst)
        assertThat(actual?.pitagTrigger).isEqualTo(expected.pitagTrigger)
    }
}