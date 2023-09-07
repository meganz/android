package mega.privacy.android.domain.usecase.advertisements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.advertisements.AdDetail
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.repository.AdsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchAdDetailUseCaseTest {
    private lateinit var underTest: FetchAdDetailUseCase
    private val adsRepository = mock<AdsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = FetchAdDetailUseCase(
            adsRepository = adsRepository,
        )
    }

    private val slotId = "ANDFB"
    private val url = "https://megaad.nz/#z_xyz"
    private val adDetailList = listOf(AdDetail(slotId, url))
    private val fetchAdDetailRequest = FetchAdDetailRequest(slotId, null)
    private val expectedResult = AdDetail(slotId, url)

    @Test
    fun `test that ad details are fetched correctly`() {
        runTest {
            whenever(adsRepository.fetchAdDetails(listOf(slotId), null)).thenReturn(
                adDetailList
            )
            val actual = underTest.invoke(fetchAdDetailRequest)
            assertThat(actual).isEqualTo(expectedResult)
        }
    }
}