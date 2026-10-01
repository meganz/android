package mega.privacy.android.domain.usecase.verification

import app.cash.turbine.test
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

internal class DefaultMonitorVerifiedPhoneNumberTest {
    private lateinit var underTest: MonitorVerifiedPhoneNumber

    private val verificationRepository = mock<VerificationRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorVerifiedPhoneNumber(
            verificationRepository = verificationRepository
        )
    }

    @Test
    fun `test that only distinct value are returned`() = runTest {
        val numbers = listOf(
            VerifiedPhoneNumber.NoVerifiedPhoneNumber,
            VerifiedPhoneNumber.PhoneNumber("123"),
            VerifiedPhoneNumber.NoVerifiedPhoneNumber,
            VerifiedPhoneNumber.PhoneNumber("321"),
        )

        verificationRepository.stub {
            on { monitorVerifiedPhoneNumber() }.thenReturn(flow {
                numbers.forEach { number ->
                    repeat(4) { emit(number) }
                }
                awaitCancellation()
            })

            underTest().test {
                repeat(numbers.size) {
                    awaitItem()
                }
            }
        }


    }
}