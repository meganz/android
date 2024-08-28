package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ManagementMessageViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManagementMessageViewModelTest {
    private lateinit var underTest: ManagementMessageViewModel
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase = mock()
    private val getMyFullNameUseCase: GetMyFullNameUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = ManagementMessageViewModel(
            getParticipantFullNameUseCase,
            getMyFullNameUseCase,
            getMyUserHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getParticipantFullNameUseCase,
            getMyFullNameUseCase,
            getMyUserHandleUseCase,
        )
    }

    @Test
    fun `test that full name of participant is returned correctly`() = runTest {
        whenever(getParticipantFullNameUseCase(1234567890L)).thenReturn("Participant")
        Truth.assertThat(underTest.getParticipantFullName(1234567890L)).isEqualTo("Participant")
    }

    @Test
    fun `test that my full name is returned correctly`() = runTest {
        whenever(getMyFullNameUseCase()).thenReturn("My Name")
        Truth.assertThat(underTest.getMyFullName()).isEqualTo("My Name")
    }

    @Test
    fun `test that my user handle is returned correctly`() = runTest {
        whenever(getMyUserHandleUseCase()).thenReturn(1234567890L)
        Truth.assertThat(underTest.getMyUserHandle()).isEqualTo(1234567890L)
    }
}