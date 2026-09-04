package mega.privacy.android.feature.sync.domain.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.concurrent.CancellationException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RescanSyncUseCaseTest {

    private lateinit var underTest: RescanSyncUseCase

    private val syncRepository = mock<SyncRepository>()

    @BeforeAll
    fun setup() {
        underTest = RescanSyncUseCase(syncRepository = syncRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository)
    }

    @Test
    fun `test that invoke rescans the given sync with re-fingerprinting by default`() = runTest {
        underTest(SYNC_ID)

        verify(syncRepository).rescanSync(SYNC_ID, reFingerprint = true)
        verifyNoMoreInteractions(syncRepository)
    }

    @Test
    fun `test that invoke passes re-fingerprinting through when it is turned off`() = runTest {
        underTest(SYNC_ID, reFingerprint = false)

        verify(syncRepository).rescanSync(SYNC_ID, reFingerprint = false)
    }

    @Test
    fun `test that invoke propagates the throwable when the repository fails`() {
        val error = RuntimeException("The SDK rejected the rescan")
        syncRepository.stub {
            onBlocking { rescanSync(SYNC_ID, reFingerprint = true) } doThrow error
        }

        val actual = assertThrows(RuntimeException::class.java) {
            runBlocking { underTest(SYNC_ID) }
        }

        assertThat(actual).isSameInstanceAs(error)
    }

    @Test
    fun `test that invoke lets cancellation through rather than swallowing it`() {
        syncRepository.stub {
            onBlocking { rescanSync(SYNC_ID, reFingerprint = true) } doThrow
                    CancellationException("Scope closed")
        }

        assertThrows(CancellationException::class.java) {
            runBlocking { underTest(SYNC_ID) }
        }
    }

    private companion object {
        const val SYNC_ID = 4_923L
    }
}
