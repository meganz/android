package mega.privacy.android.domain.usecase.notifications

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.NewShareAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUnreadAlertsCountUseCaseTest {
    private lateinit var underTest: MonitorUnreadAlertsCountUseCase

    private val notificationsRepository = mock<NotificationsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorUnreadAlertsCountUseCase(notificationsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository)
    }

    @Test
    fun `test that invoke returns a flow with the amount of not seen user alerts starting with current amount`() =
        runTest {
            val current = 3
            val firstUpdate = 4
            val secondUpdate = 0
            whenever(notificationsRepository.getUserAlerts()) doReturn createUserAlerts(current)
            val userAlertsFlow = MutableStateFlow(createUserAlerts(firstUpdate))
            whenever(notificationsRepository.monitorUserAlerts()) doReturn userAlertsFlow
            underTest.invoke().test {
                assertThat(awaitItem()).isEqualTo(current)
                assertThat(awaitItem()).isEqualTo(firstUpdate)
                userAlertsFlow.emit(createUserAlerts(secondUpdate))
                assertThat(awaitItem()).isEqualTo(secondUpdate)
            }
        }

    private fun createUserAlerts(notSeenAmount: Int, seenAmount: Int = 10) =
        buildList<UserAlert> {
            repeat(notSeenAmount) {
                add(
                    NewShareAlert(
                        id = it.toLong(),
                        seen = false,
                        createdTime = 0L,
                        isOwnChange = false,
                        nodeId = null,
                        contact = mock()
                    )
                )
            }
            repeat(seenAmount) {
                add(
                    NewShareAlert(
                        id = (notSeenAmount + it).toLong(),
                        seen = true,
                        createdTime = 0L,
                        isOwnChange = false,
                        nodeId = null,
                        contact = mock()
                    )
                )
            }
        }
}