package test.mega.privacy.android.app.presentation.advertisements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.advertisements.AdsViewModel
import mega.privacy.android.app.presentation.advertisements.model.AdsLoadState
import mega.privacy.android.domain.entity.advertisements.AdDetail
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsViewModelTest {
    private lateinit var underTest: AdsViewModel

    private val fetchAdDetailUseCase = mock<FetchAdDetailUseCase>()

    private val slotId = "ANDFB"
    private val url = "https://megaad.nz/#z_xyz"
    private val fetchAdDetailRequest = FetchAdDetailRequest(slotId, null)
    private val fetchedAdDetail = AdDetail(slotId, url)

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        reset(
            fetchAdDetailUseCase,
        )
    }

    private fun initViewModel() {
        underTest = AdsViewModel(
            fetchAdDetailUseCase = fetchAdDetailUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that state is updated correctly if fetchAdDetailUseCase succeeds`() {
        runTest {
            val expectedResult = AdsLoadState.Loaded(url)
            whenever(fetchAdDetailUseCase(fetchAdDetailRequest)).thenReturn(fetchedAdDetail)
            initViewModel()
            with(underTest) {
                fetchAdUrl(slotId)
                state.test {
                    assertThat(awaitItem()).isEqualTo(expectedResult)
                }
            }
        }
    }

    @Test
    fun `test that an exception on fetchAdDetailUseCase is not propagated`() {
        withCoroutineExceptions {
            runTest {
                val expectedResult = AdsLoadState.Empty
                fetchAdDetailUseCase.stub {
                    onBlocking { invoke(any()) }.thenAnswer {
                        throw MegaException(
                            1,
                            "Fetch AdDetail threw an exception"
                        )
                    }
                }
                initViewModel()
                underTest.state.test {
                    underTest.fetchAdUrl(slotId)
                    assertThat(awaitItem()).isEqualTo(expectedResult)
                }
            }
        }
    }
}