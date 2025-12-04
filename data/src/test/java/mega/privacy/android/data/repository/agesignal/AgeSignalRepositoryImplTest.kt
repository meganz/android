package mega.privacy.android.data.repository.agesignal

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeSignalsErrorCode
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.AdultVerified
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.RequiresMinorRestriction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
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
    private val context: Context = mock()
    private val dispatcher = UnconfinedTestDispatcher()

    private val ageSignalsManager = mock<AgeSignalsManager>()
    private val ageSignalsRequestBuilder = mock<AgeSignalsRequest.Builder>()
    private val ageSignalsRequest = mock<AgeSignalsRequest>()
    private val ageSignalsTask = mock<Task<AgeSignalsResult>>()
    private val ageSignalsResult = mock<AgeSignalsResult>()

    private val factoryMock = mockStatic(AgeSignalsManagerFactory::class.java)
    private val requestBuilderMock = mockStatic(AgeSignalsRequest::class.java)

    @BeforeAll
    fun setUp() {
        factoryMock.`when`<AgeSignalsManager> { AgeSignalsManagerFactory.create(any()) }
            .thenReturn(ageSignalsManager)

        requestBuilderMock.`when`<AgeSignalsRequest.Builder> { AgeSignalsRequest.builder() }
            .thenReturn(ageSignalsRequestBuilder)

        underTest = AgeSignalRepositoryImpl(
            context = context,
            defaultDispatcher = dispatcher,
        )
    }

    @AfterAll
    fun tearDown() {
        factoryMock.close()
        requestBuilderMock.close()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            ageSignalsManager,
            ageSignalsRequestBuilder,
            ageSignalsRequest,
            ageSignalsTask,
            ageSignalsResult,
        )
        // Re-setup default mock behaviors after reset
        whenever(ageSignalsRequestBuilder.build()).thenReturn(ageSignalsRequest)
        whenever(ageSignalsManager.checkAgeSignals(any())).thenReturn(ageSignalsTask)
        whenever(ageSignalsTask.result).thenReturn(ageSignalsResult)
    }

    @Nested
    inner class StatusMappingTests {

        @ParameterizedTest(name = "status {0} maps to {1}")
        @MethodSource("mega.privacy.android.data.repository.agesignal.AgeSignalRepositoryImplTest#provideStatusMappingParameters")
        fun `test that status maps correctly`(
            statusValue: Int?,
            expectedStatus: UserAgeComplianceStatus,
        ) = runTest {
            // Mock returns Int? - the implementation compares with AgeSignalsVerificationStatus constants
            whenever(ageSignalsResult.userStatus()).thenReturn(statusValue)

            val result = underTest.fetchAgeSignal()

            assertThat(result).isEqualTo(expectedStatus)
        }

        @Test
        fun `test that null status defaults to RequiresMinorRestriction`() = runTest {
            // Mock returns Int? - null maps to RequiresMinorRestriction
            whenever(ageSignalsResult.userStatus()).thenReturn(null)

            val result = underTest.fetchAgeSignal()

            assertThat(result).isEqualTo(RequiresMinorRestriction)
        }
    }

    @Nested
    inner class ExceptionHandlingTests {

        @ParameterizedTest(name = "{0} returns RequiresMinorRestriction")
        @MethodSource("mega.privacy.android.data.repository.agesignal.AgeSignalRepositoryImplTest#provideExceptionParameters")
        fun `test that exceptions return RequiresMinorRestriction`(
            exception: Exception,
        ) = runTest {
            whenever(ageSignalsTask.result) doAnswer { throw exception }

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
