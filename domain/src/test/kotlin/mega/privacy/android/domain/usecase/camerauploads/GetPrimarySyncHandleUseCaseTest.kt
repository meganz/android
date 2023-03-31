package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [GetPrimarySyncHandleUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPrimarySyncHandleUseCaseTest {

    private lateinit var underTest: GetPrimarySyncHandleUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetPrimarySyncHandleUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "handle from repository: {0}")
    @ValueSource(longs = [100L])
    @NullSource
    fun `test that the correct primary sync handle is returned`(syncHandle: Long?) = runTest {
        whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(syncHandle)
        whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1)

        val retrievedSyncHandle = underTest()
        verify(cameraUploadRepository, times(1)).getPrimarySyncHandle()

        if (syncHandle != null) {
            assertThat(retrievedSyncHandle).isEqualTo(syncHandle)
            verify(cameraUploadRepository, never()).getInvalidHandle()
        } else {
            assertThat(retrievedSyncHandle).isEqualTo(-1)
            verify(cameraUploadRepository, times(1)).getInvalidHandle()
        }
    }
}