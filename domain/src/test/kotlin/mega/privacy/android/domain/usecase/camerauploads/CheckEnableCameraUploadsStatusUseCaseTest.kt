package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [CheckEnableCameraUploadsStatusUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckEnableCameraUploadsStatusUseCaseTest {

    private lateinit var underTest: CheckEnableCameraUploadsStatusUseCase

    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()
    private val isBusinessAccountActive = mock<IsBusinessAccountActive>()

    @BeforeAll
    fun setUp() {
        underTest = CheckEnableCameraUploadsStatusUseCase(
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            isBusinessAccountActive = isBusinessAccountActive,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getAccountDetailsUseCase, isBusinessAccountActive)
    }

    @Test
    fun `test that CAN_ENABLE_CAMERA_UPLOADS is returned when the user is on a normal account`() =
        runTest {
            val userAccount = mock<UserAccount> {
                on { isBusinessAccount }.thenReturn(false)
                on { accountTypeIdentifier }.thenReturn(AccountType.FREE)
            }

            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(userAccount)

            assertThat(underTest()).isEqualTo(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
        }

    @ParameterizedTest(name = "account type: {0}")
    @EnumSource(
        value = AccountType::class,
        names = ["BUSINESS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that CAN_ENABLE_CAMERA_UPLOADS is returned when the user is on any type of business account but the account type is not business`(
        accountType: AccountType,
    ) = runTest {
        val userAccount = mock<UserAccount> {
            on { isBusinessAccount }.thenReturn(true)
            on { accountTypeIdentifier }.thenReturn(accountType)
        }

        whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(userAccount)

        assertThat(underTest()).isEqualTo(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
    }

    @ParameterizedTest(name = "when is business account active is {0} and is master business account is {1}, then the camera uploads status is {2}")
    @MethodSource("provideBusinessAccountParams")
    fun `test that the correct camera uploads status is returned when the user is on any type of business account and the account type is business`(
        isBusinessAccountActive: Boolean,
        isMasterBusinessAccount: Boolean,
        expectedCameraUploadsStatus: EnableCameraUploadsStatus,
    ) = runTest {
        val userAccount = mock<UserAccount> {
            on { isBusinessAccount }.thenReturn(true)
            on { this.isMasterBusinessAccount }.thenReturn(isMasterBusinessAccount)
            on { accountTypeIdentifier }.thenReturn(AccountType.BUSINESS)
        }

        whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(userAccount)
        whenever(isBusinessAccountActive()).thenReturn(isBusinessAccountActive)

        assertThat(underTest()).isEqualTo(expectedCameraUploadsStatus)
    }

    private fun provideBusinessAccountParams() = Stream.of(
        Arguments.of(true, true, EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS),
        Arguments.of(
            true,
            false,
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT,
        ),
        Arguments.of(
            false,
            true,
            EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT,
        ),
        Arguments.of(
            false,
            false,
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
        ),
    )
}