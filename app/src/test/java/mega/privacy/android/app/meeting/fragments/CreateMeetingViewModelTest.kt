package mega.privacy.android.app.meeting.fragments

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExtendWith(CoroutineMainDispatcherExtension::class, InstantTaskExecutorExtension::class)
class CreateMeetingViewModelTest {
    private lateinit var underTest: CreateMeetingViewModel

    private val getMyFullNameUseCase = mock<GetMyFullNameUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = CreateMeetingViewModel(
            getMyFullNameUseCase = getMyFullNameUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getMyFullNameUseCase)
    }

    @Test
    fun `test that myFullName emits the name returned by the use case`() = runTest {
        getMyFullNameUseCase.stub { onBlocking { invoke() } doReturn "John Doe" }

        underTest.myFullName.test {
            assertThat(expectMostRecentItem()).isEqualTo("John Doe")
        }
    }

    @Test
    fun `test that myFullName stays null when the use case fails`() = runTest {
        getMyFullNameUseCase.stub { onBlocking { invoke() } doThrow RuntimeException() }

        underTest.myFullName.test {
            assertThat(expectMostRecentItem()).isNull()
        }
    }

    @Test
    fun `test that initHintMeetingName formats the hint with the loaded full name`() = runTest {
        getMyFullNameUseCase.stub { onBlocking { invoke() } doReturn "John Doe" }
        val context = mock<Context> {
            on { getString(R.string.type_meeting_name, "John Doe") } doReturn "John Doe’s meeting"
        }

        underTest.myFullName.test {
            expectMostRecentItem()
            assertThat(underTest.initHintMeetingName(context)).isEqualTo("John Doe’s meeting")
        }
    }

    @Test
    fun `test that initMeetingName sets an empty meeting name`() {
        underTest.initMeetingName()

        assertThat(underTest.meetingName.value).isEmpty()
    }

    @Test
    fun `test that updateMeetingName sets the meeting name`() {
        underTest.updateMeetingName("Weekly sync")

        assertThat(underTest.meetingName.value).isEqualTo("Weekly sync")
    }
}
