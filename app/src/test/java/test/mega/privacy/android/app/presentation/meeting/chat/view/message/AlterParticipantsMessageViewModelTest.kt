package test.mega.privacy.android.app.presentation.meeting.chat.view.message

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.view.message.AlterParticipantsMessageViewModel
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AlterParticipantsMessageViewModelTest {
    private lateinit var underTest: AlterParticipantsMessageViewModel
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase = mock()
    private val getMyFullNameUseCase: GetMyFullNameUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = AlterParticipantsMessageViewModel(
            getParticipantFullNameUseCase,
            getMyFullNameUseCase,
            getMyUserHandleUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
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