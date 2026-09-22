package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsWifiNotSatisfiedUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsWifiNotSatisfiedUseCaseTest {

    private lateinit var underTest: IsWifiNotSatisfiedUseCase

    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val networkRepository = mock<NetworkRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsWifiNotSatisfiedUseCase(
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            networkRepository = networkRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isCameraUploadsByWifiUseCase, networkRepository)
    }

    @Test
    fun `test that the wi-fi condition is satisfied when both wi-fi and mobile data are configured`() =
        runTest {
            whenever(isCameraUploadsByWifiUseCase()).thenReturn(false)

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that the wi-fi condition is satisfied when only wi-fi is configured and the user is connected to wi-fi`() =
        runTest {
            whenever(isCameraUploadsByWifiUseCase()).thenReturn(true)
            whenever(networkRepository.isOnWifi()).thenReturn(true)

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that the wi-fi condition is unsatisfied when only wi-fi is configured and the user is connected to mobile data`() =
        runTest {
            whenever(isCameraUploadsByWifiUseCase()).thenReturn(true)
            whenever(networkRepository.isOnWifi()).thenReturn(false)

            assertThat(underTest()).isTrue()
        }
}