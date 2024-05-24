package mega.privacy.android.app.presentation.login.onboarding.view

import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.login.SetLogoutInProgressFlagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TourViewModelTest {

    private lateinit var underTest: TourViewModel

    private val setLogoutInProgressFlagUseCase: SetLogoutInProgressFlagUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = TourViewModel(
            setLogoutInProgressFlagUseCase = setLogoutInProgressFlagUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(setLogoutInProgressFlagUseCase)
    }

    @Test
    fun `test that the logout progress flag is cleared`() = runTest {
        underTest.clearLogoutProgressFlag()

        verify(setLogoutInProgressFlagUseCase).invoke(false)
    }
}
