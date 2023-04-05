package mega.privacy.android.data.repository.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.account.business.BusinessAccountStatusMapper
import mega.privacy.android.domain.repository.BusinessRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class BusinessRepositoryImplTest {
    private lateinit var underTest: BusinessRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = BusinessRepositoryImpl(
            megaApiGateway = megaApiGateway,
            businessAccountStatusMapper = BusinessAccountStatusMapper(),
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that is business account active returns true if api returns true`() =
        runTest {
            whenever(megaApiGateway.isBusinessAccountActive()).thenReturn(true)
            assertThat(underTest.isBusinessAccountActive()).isTrue()
        }

    @Test
    fun `test that is business account active returns false if api returns false`() = runTest {
        whenever(megaApiGateway.isBusinessAccountActive()).thenReturn(false)
        assertThat(underTest.isBusinessAccountActive()).isFalse()
    }
}