package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MiscFlagsInitialiserTest {
    private lateinit var underTest: MiscFlagsInitialiser

    private val isUserLoggedInUseCase = mock<IsUserLoggedInUseCase>()
    private val getMiscFlagsUseCase = mock<GetMiscFlagsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MiscFlagsInitialiser(
            isUserLoggedInUseCase = isUserLoggedInUseCase,
            getMiscFlagsUseCase = getMiscFlagsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isUserLoggedInUseCase, getMiscFlagsUseCase)
    }

    @Test
    fun `test that invoke calls getMiscFlagsUseCase when user is not logged in`() = runTest {
        isUserLoggedInUseCase.stub { onBlocking { invoke() }.thenReturn(false) }

        underTest()

        verify(getMiscFlagsUseCase).invoke()
    }

    @Test
    fun `test that invoke does not call getMiscFlagsUseCase when user is logged in`() = runTest {
        isUserLoggedInUseCase.stub { onBlocking { invoke() }.thenReturn(true) }

        underTest()

        verifyNoInteractions(getMiscFlagsUseCase)
    }
}
