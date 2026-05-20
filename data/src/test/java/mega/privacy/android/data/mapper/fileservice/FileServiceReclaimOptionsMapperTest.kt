package mega.privacy.android.data.mapper.fileservice

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import nz.mega.sdk.MegaFileServiceReclaimOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileServiceReclaimOptionsMapperTest {

    private val underTest = FileServiceReclaimOptionsMapper()

    @Test
    fun `test that all fields are mapped correctly`() {
        val sdkOptions = mock<MegaFileServiceReclaimOptions> {
            on { ageThreshold }.thenReturn(30)
            on { delay }.thenReturn(60L)
            on { period }.thenReturn(3600L)
            on { reclaimThreshold }.thenReturn(1024L)
            on { reclaimTarget }.thenReturn(2048L)
        }

        val expected = FileServiceReclaimOptions(
            reclaimAgeThreshold = 30.minutes,
            reclaimDelay = 60.seconds,
            reclaimPeriod = 3600.seconds,
            reclaimSizeThreshold = 1024L,
            reclaimTarget = 2048L,
        )

        val result = underTest(sdkOptions)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that zero values are mapped correctly`() {
        val sdkOptions = mock<MegaFileServiceReclaimOptions> {
            on { ageThreshold }.thenReturn(0)
            on { delay }.thenReturn(0L)
            on { period }.thenReturn(0L)
            on { reclaimThreshold }.thenReturn(FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_NONE)
            on { reclaimTarget }.thenReturn(0L)
        }

        val result = underTest(sdkOptions)

        assertThat(result.reclaimAgeThreshold).isEqualTo(0.minutes)
        assertThat(result.reclaimDelay).isEqualTo(0.seconds)
        assertThat(result.reclaimPeriod).isEqualTo(0.seconds)
        assertThat(result.reclaimSizeThreshold).isEqualTo(FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_NONE)
        assertThat(result.reclaimTarget).isEqualTo(0L)
    }

    @Test
    fun `test that disabled threshold is mapped correctly`() {
        val sdkOptions = mock<MegaFileServiceReclaimOptions> {
            on { ageThreshold }.thenReturn(10)
            on { delay }.thenReturn(5L)
            on { period }.thenReturn(100L)
            on { reclaimThreshold }.thenReturn(FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_DISABLED)
            on { reclaimTarget }.thenReturn(0L)
        }

        val result = underTest(sdkOptions)

        assertThat(result.reclaimSizeThreshold).isEqualTo(FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_DISABLED)
    }
}
