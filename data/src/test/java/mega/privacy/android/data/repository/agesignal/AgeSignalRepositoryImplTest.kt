package mega.privacy.android.data.repository.agesignal

import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.model.AgeSignalsErrorCode
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AgeSignalsGateway
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.AdultVerified
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.RequiresMinorRestriction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [AgeSignalRepositoryImpl]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AgeSignalRepositoryImplTest {
    private lateinit var underTest: AgeSignalRepositoryImpl
    private val dispatcher = UnconfinedTestDispatcher()
    private val ageSignalsGateway: AgeSignalsGateway = mock()

    @BeforeAll
    fun setUp() {
        underTest = AgeSignalRepositoryImpl(
            ageSignalsGateway = ageSignalsGateway,
            defaultDispatcher = dispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(ageSignalsGateway)
    }

    @Nested
    inner class StatusMappingTests {

        @ParameterizedTest(name = "status {0} maps to {1}")
        @MethodSource("mega.privacy.android.data.repository.agesignal.AgeSignalRepositoryImplTest#provideStatusMappingParameters")
        fun `test that status maps correctly`(
            statusValue: Int?,
            expectedStatus: UserAgeComplianceStatus,
        ) = runTest {
            // Gateway now returns userStatus directly as suspend function
            whenever(ageSignalsGateway.checkAgeSignals()).thenReturn(statusValue)

            val actualResult = underTest.fetchAgeSignal()

            assertThat(actualResult).isEqualTo(expectedStatus)
        }

        @Test
        fun `test that null status defaults to RequiresMinorRestriction`() = runTest {
            // Gateway now returns userStatus directly as suspend function
            whenever(ageSignalsGateway.checkAgeSignals()).thenReturn(null)

            val actualResult = underTest.fetchAgeSignal()

            assertThat(actualResult).isEqualTo(RequiresMinorRestriction)
        }
    }

    @Nested
    inner class ExceptionHandlingTests {

        @ParameterizedTest(name = "{0} returns RequiresMinorRestriction")
        @MethodSource("mega.privacy.android.data.repository.agesignal.AgeSignalRepositoryImplTest#provideExceptionParameters")
        fun `test that exceptions return RequiresMinorRestriction`(
            exception: Exception,
        ) = runTest {
            whenever(ageSignalsGateway.checkAgeSignals()).doAnswer { throw exception }

            val result = underTest.fetchAgeSignal()

            assertThat(result).isEqualTo(RequiresMinorRestriction)
        }
    }

    companion object {
        @JvmStatic
        fun provideStatusMappingParameters(): Stream<Arguments> {
            return Stream.of(
                // VERIFIED maps to AdultVerified
                Arguments.of(AgeSignalsVerificationStatus.VERIFIED, AdultVerified),
                // All non-VERIFIED statuses map to RequiresMinorRestriction
                Arguments.of(AgeSignalsVerificationStatus.SUPERVISED, RequiresMinorRestriction),
                Arguments.of(
                    AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING,
                    RequiresMinorRestriction
                ),
                Arguments.of(
                    AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED,
                    RequiresMinorRestriction
                ),
                Arguments.of(AgeSignalsVerificationStatus.UNKNOWN, RequiresMinorRestriction),
                // Test invalid/unexpected integer value to ensure else branch handles it
                Arguments.of(99, RequiresMinorRestriction),
            )
        }

        @JvmStatic
        fun provideExceptionParameters(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(AgeSignalsException(AgeSignalsErrorCode.INTERNAL_ERROR)),
                Arguments.of(AgeSignalsException(AgeSignalsErrorCode.NETWORK_ERROR)),
                Arguments.of(RuntimeException("Network error")),
                Arguments.of(IllegalStateException("Unexpected state")),
            )
        }
    }
}
