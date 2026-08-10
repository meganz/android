package mega.privacy.android.data.repository

import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FirebaseAnalyticsGateway
import mega.privacy.android.domain.entity.analytics.FirebaseAnalyticsEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirebaseAnalyticsRepositoryImplTest {
    private lateinit var underTest: FirebaseAnalyticsRepositoryImpl

    private val firebaseAnalyticsGateway = mock<FirebaseAnalyticsGateway>()

    @BeforeAll
    fun setUp() {
        underTest = FirebaseAnalyticsRepositoryImpl(
            firebaseAnalyticsGateway = firebaseAnalyticsGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(firebaseAnalyticsGateway)
    }

    @Test
    fun `test that logEvent passes event name to gateway`() = runTest {
        underTest.logEvent(FirebaseAnalyticsEvent.CreateNewAccount)

        verify(firebaseAnalyticsGateway).logEvent("create_new_account")
    }
}
