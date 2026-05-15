package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.home.ResetHomeWidgetConfigurationsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ResetHomeWidgetConfigurationsLogoutTaskTest {
    private lateinit var underTest: ResetHomeWidgetConfigurationsLogoutTask

    private val resetHomeWidgetConfigurationsUseCase = mock<ResetHomeWidgetConfigurationsUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = ResetHomeWidgetConfigurationsLogoutTask(
            resetHomeWidgetConfigurationsUseCase = resetHomeWidgetConfigurationsUseCase,
        )
    }

    @Test
    fun `test that home widget configurations are reset on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(resetHomeWidgetConfigurationsUseCase).invoke()
    }
}
