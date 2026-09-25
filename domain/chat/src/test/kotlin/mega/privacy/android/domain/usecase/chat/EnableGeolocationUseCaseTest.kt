package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnableGeolocationUseCaseTest {

    private lateinit var underTest: EnableGeolocationUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = EnableGeolocationUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that enable geolocation invokes repository`() = runTest {
        whenever(chatRepository.enableGeolocation()).thenReturn(Unit)
        underTest.invoke()
        verify(chatRepository).enableGeolocation()
        verifyNoMoreInteractions(chatRepository)
    }
}