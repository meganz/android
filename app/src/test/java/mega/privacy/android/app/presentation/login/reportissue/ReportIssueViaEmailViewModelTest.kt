package mega.privacy.android.app.presentation.login.reportissue

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.support.SupportEmailTicket
import mega.privacy.android.domain.usecase.support.CreateSupportTicketEmailUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportIssueViaEmailViewModelTest {
    private lateinit var underTest: ReportIssueViaEmailViewModel
    private val createSupportTicketEmailUseCase = mock<CreateSupportTicketEmailUseCase>()

    @BeforeEach
    fun setup() {
        underTest = ReportIssueViaEmailViewModel(createSupportTicketEmailUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(createSupportTicketEmailUseCase)
    }

    @Test
    fun `test initial state`() = runTest {
        val initial = underTest.uiState.value
        Truth.assertThat(initial.description).isEmpty()
        Truth.assertThat(initial.canSubmit).isFalse()
    }

    @Test
    fun `test that description is updated when setDescription is called`() = runTest {
        underTest.setDescription("New description")
        val updated = underTest.uiState.value
        Truth.assertThat(updated.description).isEqualTo("New description")
        Truth.assertThat(updated.canSubmit).isTrue()
    }

    @Test
    fun `test that use case is not invoked when canSubmit is false`() = runTest {
        underTest.submit()
        verify(createSupportTicketEmailUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `test that use case is invoked when canSubmit is true`() = runTest {
        val ticket = mock<SupportEmailTicket>()
        val emailBody = "New description"
        whenever(createSupportTicketEmailUseCase.invoke(emailBody, false)).thenReturn(ticket)

        underTest.setDescription("New description")
        underTest.submit()
        verify(createSupportTicketEmailUseCase).invoke(emailBody, false)
    }
}