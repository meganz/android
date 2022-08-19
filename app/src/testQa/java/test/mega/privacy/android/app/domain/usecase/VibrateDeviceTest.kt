package test.mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.usecase.VibrateDevice
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class VibrateDeviceTest {

    lateinit var underTest: VibrateDevice
    private val repository = mock<ShakeDetectorRepository>()

    @Before
    fun setUp() {
        underTest = VibrateDevice(repository::vibrateDevice)
    }

    @Test
    fun `test that operator function gets invoked`() {
        underTest()
        verify(repository).vibrateDevice()
    }
}