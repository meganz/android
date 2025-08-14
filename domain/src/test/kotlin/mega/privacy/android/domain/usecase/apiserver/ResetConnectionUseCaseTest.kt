package mega.privacy.android.domain.usecase.apiserver

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetConnectionUseCaseTest {
    lateinit var underTest: ResetConnectionUseCase

    private val apiServerRepository = mock<ApiServerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ResetConnectionUseCase(apiServerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(apiServerRepository)
    }

    @Test
    fun `test that api server is reconnected`() = runTest {
        underTest()
        verify(apiServerRepository).reconnect()
        verify(apiServerRepository).reconnectFolderApi()
    }

    @Test
    fun `test that api server is reconnected with pinning disabled`() = runTest {
        underTest(disablePinning = true)
        verify(apiServerRepository).setPublicKeyPinning(false)
        verify(apiServerRepository).reconnect()
        verify(apiServerRepository).reconnectFolderApi()
    }

}