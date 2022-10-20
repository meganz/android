package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.RecentActionsMapper
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultRecentActionsRepositoryTest {
    private lateinit var underTest: RecentActionsRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val recentActionsMapper = mock<RecentActionsMapper>()

    @Before
    fun setUp() {
        underTest = DefaultRecentActionsRepository(
            megaApiGateway = megaApiGateway,
            recentActionsMapper = recentActionsMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that get recent actions returns the result of api recentActions`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(mock()) }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val list = listOf<MegaRecentActionBucket>()
        whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }
        whenever(recentActionsMapper(any(), any())).thenReturn(list)
        assertThat(underTest.getRecentActions()).isEqualTo(list)
    }
}