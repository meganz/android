package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectCallAudioDeviceUseCaseTest {

    private lateinit var underTest: SelectCallAudioDeviceUseCase

    private val callAudioRouteRepository = mock<CallAudioRouteRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SelectCallAudioDeviceUseCase(
            callAudioRouteRepository = callAudioRouteRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(callAudioRouteRepository)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AudioDevice::class, names = ["None"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that invoke selects the device in the repository`(device: AudioDevice) = runTest {
        underTest(device)

        verify(callAudioRouteRepository).select(device)
    }

    @Test
    fun `test that invoke throws when the repository fails`() = runTest {
        whenever(callAudioRouteRepository.select(AudioDevice.SpeakerPhone))
            .thenThrow(RuntimeException())

        assertThrows<RuntimeException> { underTest(AudioDevice.SpeakerPhone) }
    }
}
