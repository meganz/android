package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.user.UserId
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

/**
 * Test class of [DefaultCheckEnableCameraUploadsStatus]
 */
@ExperimentalCoroutinesApi
class DefaultCheckEnableCameraUploadsStatusTest {
    private lateinit var underTest: DefaultCheckEnableCameraUploadsStatus

    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()
    private val isBusinessAccountActive = mock<IsBusinessAccountActive>()

    private val testUserAccount = UserAccount(
        userId = UserId(1L),
        email = "testemail@gmail.com",
        fullName = "name",
        isBusinessAccount = false,
        isMasterBusinessAccount = false,
        accountTypeIdentifier = AccountType.PRO_I,
        accountTypeString = ""
    )

    @Before
    fun setUp() {
        underTest = DefaultCheckEnableCameraUploadsStatus(
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            isBusinessAccountActive = isBusinessAccountActive,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that CAN_ENABLE_CAMERA_UPLOADS is returned when the user is under a regular account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(testUserAccount)
            whenever(isBusinessAccountActive()).thenReturn(any())

            assertEquals(underTest.invoke(), EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
        }

    @Test
    fun `test that CAN_ENABLE_CAMERA_UPLOADS is returned when the user is under an active master business account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(
                testUserAccount.copy(
                    isBusinessAccount = true,
                    isMasterBusinessAccount = true,
                    accountTypeIdentifier = AccountType.BUSINESS
                )
            )
            whenever(isBusinessAccountActive()).thenReturn(true)

            assertEquals(underTest.invoke(), EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
        }

    @Test
    fun `test that SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT is returned when the user is under an active regular business account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(
                testUserAccount.copy(
                    isBusinessAccount = true,
                    accountTypeIdentifier = AccountType.BUSINESS
                )
            )
            whenever(isBusinessAccountActive()).thenReturn(true)

            assertEquals(
                underTest.invoke(),
                EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
            )
        }

    @Test
    fun `test that SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT is returned when the user is under an inactive regular business account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(
                testUserAccount.copy(
                    isBusinessAccount = true,
                    accountTypeIdentifier = AccountType.BUSINESS
                )
            )
            whenever(isBusinessAccountActive()).thenReturn(false)

            assertEquals(
                underTest.invoke(),
                EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
            )
        }

    @Test
    fun `test that SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT is returned when the user is under an inactive master business account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(
                testUserAccount.copy(
                    isBusinessAccount = true,
                    isMasterBusinessAccount = true,
                    accountTypeIdentifier = AccountType.BUSINESS
                )
            )
            whenever(isBusinessAccountActive()).thenReturn(false)

            assertEquals(
                underTest.invoke(),
                EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
            )
        }

    @Test
    fun `test that CAN_ENABLE_CAMERA_UPLOADS is returned when the user is Pro Flexi account`() =
        runTest {
            whenever(getAccountDetailsUseCase(forceRefresh = true)).thenReturn(
                testUserAccount.copy(
                    isBusinessAccount = true,
                    accountTypeIdentifier = AccountType.PRO_FLEXI
                )
            )
            whenever(isBusinessAccountActive()).thenReturn(true)

            assertEquals(
                underTest.invoke(),
                EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
            )
        }

}