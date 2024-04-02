package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNumUnreadPromoNotificationsUseCaseTest {
    private lateinit var underTest: GetNumUnreadPromoNotificationsUseCase
    private val getEnabledNotificationsUseCase = mock<GetEnabledNotificationsUseCase>()
    private val getLastReadNotificationIDUseCase = mock<GetLastReadNotificationIdUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = GetNumUnreadPromoNotificationsUseCase(
            getEnabledNotificationsUseCase = getEnabledNotificationsUseCase,
            getLastReadNotificationIdUseCase = getLastReadNotificationIDUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getEnabledNotificationsUseCase,
            getLastReadNotificationIDUseCase
        )
    }


    private fun provideParameters() = Stream.of(
        Arguments.of(emptyList<Int>(), 0, 0),
        Arguments.of(
            listOf(1), 1, 0
        ),
        Arguments.of(
            listOf(1, 2), 1, 1
        ),
        Arguments.of(
            listOf(1, 2, 3), 0, 3
        ),
        Arguments.of(
            listOf(1, 2, 3), 3, 0
        ),
    )

    @ParameterizedTest
    @MethodSource("provideParameters")
    fun `test that calculateUnreadPromoNotifications return the correct number of unread promo notifications`(
        enabledPromoNotificationsIDs: List<Int>,
        lastReadNotificationId: Long,
        expectedUnreadPromoNotificationsCount: Int,
    ) = runTest {
        whenever(getEnabledNotificationsUseCase()).thenReturn(enabledPromoNotificationsIDs)
        whenever(getLastReadNotificationIDUseCase()).thenReturn(lastReadNotificationId)
        Truth.assertThat(underTest.invoke()).isEqualTo(expectedUnreadPromoNotificationsCount)
    }
}