package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.repository.DefaultRecentActionsRepository
import mega.privacy.android.app.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaRecentActionBucket
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultRecentActionsRepositoryTest {
    private lateinit var underTest: RecentActionsRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultRecentActionsRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that get recent actions would invoke api recentActions`() = runTest {
        underTest.getRecentActions()
        verify(megaApiGateway).getRecentActions()
    }

    @Test
    fun `test that get recent actions returns the result of api recentActions`() = runTest {
        val expected = listOf<MegaRecentActionBucket>()
        whenever(megaApiGateway.getRecentActions()).thenReturn(expected)
        assertThat(underTest.getRecentActions()).isEqualTo(expected)
    }
}