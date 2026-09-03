package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.InitialiseLoggingUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingInitialiserTest {

    private val initialiseLoggingUseCase = mock<InitialiseLoggingUseCase>()

    @Test
    fun `test that invoke does not run logging initialisation synchronously`() = runTest {
        val underTest = LoggingInitialiser(
            initialiseLoggingUseCase = initialiseLoggingUseCase,
            appScope = this,
        )

        underTest()

        verifyNoInteractions(initialiseLoggingUseCase)
    }

    @Test
    fun `test that invoke launches logging initialisation in the application scope`() = runTest {
        val underTest = LoggingInitialiser(
            initialiseLoggingUseCase = initialiseLoggingUseCase,
            appScope = this,
        )

        underTest()
        advanceUntilIdle()

        verify(initialiseLoggingUseCase).invoke()
    }
}
