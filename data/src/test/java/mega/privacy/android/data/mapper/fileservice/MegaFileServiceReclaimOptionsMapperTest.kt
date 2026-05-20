package mega.privacy.android.data.mapper.fileservice

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import nz.mega.sdk.MegaFileServiceReclaimOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaFileServiceReclaimOptionsMapperTest {

    private val underTest = MegaFileServiceReclaimOptionsMapper()

    @Test
    fun `test that all fields are set correctly`() {
        val megaOptions = mock<MegaFileServiceReclaimOptions>()
        mockStatic(MegaFileServiceReclaimOptions::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaFileServiceReclaimOptions> {
                MegaFileServiceReclaimOptions.create()
            }.thenReturn(megaOptions)

            val options = FileServiceReclaimOptions(
                reclaimAgeThreshold = 30.minutes,
                reclaimDelay = 60.seconds,
                reclaimPeriod = 1.hours,
                reclaimSizeThreshold = 1024L,
                reclaimTarget = 2048L,
            )

            underTest(options)

            verify(megaOptions).ageThreshold = 30
            verify(megaOptions).delay = 60L
            verify(megaOptions).period = 3600L
            verify(megaOptions).reclaimThreshold = 1024L
            verify(megaOptions).reclaimTarget = 2048L
        }
    }

    @Test
    fun `test that zero values are set correctly`() {
        val megaOptions = mock<MegaFileServiceReclaimOptions>()
        mockStatic(MegaFileServiceReclaimOptions::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaFileServiceReclaimOptions> {
                MegaFileServiceReclaimOptions.create()
            }.thenReturn(megaOptions)

            val options = FileServiceReclaimOptions(
                reclaimAgeThreshold = 0.minutes,
                reclaimDelay = 0.seconds,
                reclaimPeriod = 0.seconds,
                reclaimSizeThreshold = FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_NONE,
                reclaimTarget = 0L,
            )

            underTest(options)

            verify(megaOptions).ageThreshold = 0
            verify(megaOptions).delay = 0L
            verify(megaOptions).period = 0L
            verify(megaOptions).reclaimThreshold = FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_NONE
            verify(megaOptions).reclaimTarget = 0L
        }
    }

    @Test
    fun `test that sub-minute age threshold is rounded up to 1 minute`() {
        val megaOptions = mock<MegaFileServiceReclaimOptions>()
        mockStatic(MegaFileServiceReclaimOptions::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaFileServiceReclaimOptions> {
                MegaFileServiceReclaimOptions.create()
            }.thenReturn(megaOptions)

            val options = FileServiceReclaimOptions(
                reclaimAgeThreshold = 30.seconds,
                reclaimDelay = 0.seconds,
                reclaimPeriod = 0.seconds,
                reclaimSizeThreshold = 0L,
                reclaimTarget = 0L,
            )

            underTest(options)

            verify(megaOptions).ageThreshold = 1
        }
    }

    @Test
    fun `test that disabled threshold is set correctly`() {
        val megaOptions = mock<MegaFileServiceReclaimOptions>()
        mockStatic(MegaFileServiceReclaimOptions::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaFileServiceReclaimOptions> {
                MegaFileServiceReclaimOptions.create()
            }.thenReturn(megaOptions)

            val options = FileServiceReclaimOptions(
                reclaimAgeThreshold = 10.minutes,
                reclaimDelay = 5.seconds,
                reclaimPeriod = 100.seconds,
                reclaimSizeThreshold = FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_DISABLED,
                reclaimTarget = 0L,
            )

            underTest(options)

            verify(megaOptions).reclaimThreshold = FileServiceReclaimOptions.RECLAIM_SIZE_THRESHOLD_DISABLED
        }
    }
}
