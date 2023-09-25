package mega.privacy.android.data.repository.psa

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.PermanentCache
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.psa.PsaPreferenceGateway
import mega.privacy.android.data.mapper.psa.PsaMapper
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.repository.psa.PsaRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PsaRepositoryImplTest {
    private lateinit var underTest: PsaRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val cache = PermanentCache<Psa>()

    private val psaPreferenceGateway = mock<PsaPreferenceGateway>()

    private val psaMapper = mock<PsaMapper>()

    @BeforeEach
    internal fun setUp() {
        underTest = PsaRepositoryImpl(
            megaApiGateway = megaApiGateway,
            psaCache = cache,
            psaPreferenceGateway = psaPreferenceGateway,
            psaMapper = psaMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    internal fun `test that cache value is returned if calling fetch psa and refreshCache is false`() =
        runTest {
            val expected = mock<Psa>()
            cache.clear()
            cache.set(expected)
            val actual = underTest.fetchPsa(false)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    internal fun `test that cache value is updated before returning if calling fetch psa and refreshCache is true`() =
        runTest {
            val expected = mock<Psa>()
            cache.clear()

            psaMapper.stub {
                onBlocking { invoke(any()) }.thenReturn(expected)
            }

            megaApiGateway.stub {
                val megaError = mock<MegaError> {
                    on { errorCode }.thenReturn(MegaError.API_OK)
                }
                onBlocking { getPsa(any()) }.thenAnswer {
                    (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                        api = mock(),
                        request = mock(),
                        e = megaError
                    )
                }
            }

            val actual = underTest.fetchPsa(true)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    internal fun `test that get last fetched time returns value from gateway`() = runTest {
        val expected = 55L
        psaPreferenceGateway.stub {
            onBlocking { getLastRequestedTime() }.thenReturn(expected)
        }

        assertThat(underTest.getLastPsaFetchedTime()).isEqualTo(expected)
    }

    @Test
    internal fun `test that setting the last fetched time sets it on the gateway`() = runTest {
        val newTime = 14L

        underTest.setLastFetchedTime(newTime)

        verify(psaPreferenceGateway).setLastRequestedTime(newTime)
    }

    @Test
    internal fun `test that calling dismiss calls the correct method on the api gateway`() =
        runTest {
            val psaId = 66
            underTest.dismissPsa(psaId)

            verify(megaApiGateway).setPsaHandled(psaId)
        }

}