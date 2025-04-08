package mega.privacy.android.app.presentation.login.onboarding.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.SetLogoutInProgressFlagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.booleanArrayOf

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TourViewModelTest {

    private lateinit var underTest: TourViewModel

    private val setLogoutInProgressFlagUseCase: SetLogoutInProgressFlagUseCase = mock()
    private val isUrlMatchesRegexUseCase: IsUrlMatchesRegexUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = TourViewModel(
            setLogoutInProgressFlagUseCase = setLogoutInProgressFlagUseCase,
            isUrlMatchesRegexUseCase = isUrlMatchesRegexUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            setLogoutInProgressFlagUseCase,
            isUrlMatchesRegexUseCase,
            getFeatureFlagValueUseCase
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that ui state is updated based on value from RegistrationRevamp feature flag on init`(
        value: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
        setUp()
        underTest.uiState.test {
            assertThat(awaitItem().isNewRegistrationUiEnabled).isEqualTo(value)
        }
    }

    @Test
    fun `test that the logout progress flag is cleared`() = runTest {
        underTest.clearLogoutProgressFlag()

        verify(setLogoutInProgressFlagUseCase).invoke(false)
    }

    @Test
    fun `test that the meeting link is updated and the error text is reset when the meeting link changes`() =
        runTest {
            val meetingLink = "https://mega.co.nz/meetingLink"
            underTest.onMeetingLinkChange(meetingLink)

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.meetingLink).isEqualTo(meetingLink)
                assertThat(item.errorTextId).isNull()
            }
        }

    @Test
    fun `test that the correct error message is returned confirming a blank meeting link`() =
        runTest {
            underTest.onConfirmMeetingLinkClick()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().errorTextId).isEqualTo(R.string.invalid_meeting_link_empty)
            }
        }

    @Test
    fun `test that the correct error message is returned when the meeting link does not match the specified regex pattern`() =
        runTest {
            val meetingLink = "https://mega.co.nz/meetingLink"
            whenever(
                isUrlMatchesRegexUseCase(
                    url = meetingLink,
                    patterns = Constants.CHAT_LINK_REGEXS
                )
            ) doReturn false
            underTest.onMeetingLinkChange(meetingLink)

            underTest.onConfirmMeetingLinkClick()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().errorTextId).isEqualTo(R.string.invalid_meeting_link_args)
            }
        }

    @Test
    fun `test that the meeting link opens successfully when the meeting link matches the specified regex pattern`() =
        runTest {
            val meetingLink = "https://mega.co.nz/meetingLink"
            whenever(
                isUrlMatchesRegexUseCase(
                    url = meetingLink,
                    patterns = Constants.CHAT_LINK_REGEXS
                )
            ) doReturn true
            underTest.onMeetingLinkChange(meetingLink)

            underTest.onConfirmMeetingLinkClick()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldOpenLink).isTrue()
            }
        }
}
