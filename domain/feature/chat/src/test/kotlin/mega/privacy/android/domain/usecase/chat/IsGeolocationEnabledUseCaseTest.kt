package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsGeolocationEnabledUseCaseTest {

    private lateinit var underTest: IsGeolocationEnabledUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = IsGeolocationEnabledUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = " if repository returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is geolocation enabled returns correctly`(
        isGeolocationEnabled: Boolean,
    ) = runTest {
        whenever(chatRepository.isGeolocationEnabled()).thenReturn(isGeolocationEnabled)
        Truth.assertThat(underTest.invoke()).isEqualTo(isGeolocationEnabled)
        verify(chatRepository).isGeolocationEnabled()
        verifyNoMoreInteractions(chatRepository)
    }
}