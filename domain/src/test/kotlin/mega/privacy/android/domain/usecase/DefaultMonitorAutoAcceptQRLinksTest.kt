package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorAutoAcceptQRLinksTest {
    private lateinit var underTest: MonitorAutoAcceptQRLinks
    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>{ on { invoke() }.thenReturn(emptyFlow())}

    @Before
    fun setUp() {
        underTest = DefaultMonitorAutoAcceptQRLinks(
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            monitorUserUpdates = monitorUserUpdates
        )
    }

    @Test
    fun `test that value from fetch use case is returned`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenReturn(true)

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that contact link verification updates causes a new value to be returned`() =
        runTest {
            whenever(fetchAutoAcceptQRLinks()).thenReturn(true, false)
            whenever(monitorUserUpdates()).thenReturn(flowOf(UserChanges.ContactLinkVerification))

            underTest().test {
                assertThat(awaitItem()).isTrue()
                assertThat(awaitItem()).isFalse()
                awaitComplete()
            }
        }


    @Test
    fun `test that non contact link verification updates do not cause a new value to be returned`() =
        runTest {
            whenever(fetchAutoAcceptQRLinks()).thenReturn(true, false)
            whenever(monitorUserUpdates()).thenReturn(
                UserChanges.values().filterNot {
                    it == UserChanges.ContactLinkVerification
                }.asFlow()
            )

            underTest().test {
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }

}