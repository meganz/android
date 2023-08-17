package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.usecase.account.GetIncomingContactRequestsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.stream.Stream


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetIncomingContactRequestsNotificationListUseCaseTest {

    private lateinit var underTest: GetIncomingContactRequestsNotificationListUseCase

    private lateinit var getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase

    private val contactRequest1 = mock<ContactRequest> {
        on { modificationTime }.thenReturn(34526L)
    }
    private val contactRequest2 = mock<ContactRequest> {
        on { modificationTime }.thenReturn(56789L)
    }
    private val contactRequest3 = mock<ContactRequest> {
        on { modificationTime }.thenReturn(3745631L)
    }

    @BeforeAll
    fun setup() {
        getIncomingContactRequestsUseCase = mock()
        underTest = GetIncomingContactRequestsNotificationListUseCase(
            getIncomingContactRequestsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getIncomingContactRequestsUseCase)
    }


    @ParameterizedTest(
        name = " if get incoming contact requests is {0}"
    )
    @MethodSource("provideParameters")
    fun `test that get incoming contact requests notification list returns correctly`(
        incomingRequests: List<ContactRequest>,
    ) = runTest {
        whenever(getIncomingContactRequestsUseCase()).thenReturn(incomingRequests)

        val maxDays = 14
        val currentDate = Date(System.currentTimeMillis())

        val expectedList = incomingRequests.filter { contactRequest ->
            val ts: Long = contactRequest.modificationTime * 1000
            val crDate = Date(ts)
            val diff = currentDate.time - crDate.time
            val diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
            diffDays < maxDays
        }
        Truth.assertThat(underTest()).isEqualTo(expectedList)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(emptyList<ContactRequest>()),
        Arguments.of(listOf(contactRequest1)),
        Arguments.of(listOf(contactRequest2)),
        Arguments.of(listOf(contactRequest3)),
        Arguments.of(listOf(contactRequest1, contactRequest2)),
        Arguments.of(listOf(contactRequest1, contactRequest3)),
        Arguments.of(listOf(contactRequest3, contactRequest2)),
        Arguments.of(listOf(contactRequest1, contactRequest2, contactRequest3)),
    )
}