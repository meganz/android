package mega.privacy.android.app.presentation.overdisk

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverDiskQuotaPaywallViewModelTest {

    private lateinit var underTest: OverDiskQuotaPaywallViewModel

    private val isDatabaseEntryStale: IsDatabaseEntryStale = mock()
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase = mock()
    private val getPricing: GetPricing = mock()
    private val getUserDataUseCase: GetUserDataUseCase = mock()
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase = mock()

    @BeforeEach
    fun setup() {
        initializeViewModel()
    }

    @Test
    fun `test that pricing state is updated with value from the get pricing use case when the use case successfully executed`() =
        runTest {
            // Given
            val pricing = Pricing(
                products = listOf(
                    Product(
                        handle = Random.nextLong(),
                        level = Random.nextInt(),
                        months = Random.nextInt(),
                        storage = Random.nextInt(),
                        transfer = Random.nextInt(),
                        amount = Random.nextInt(),
                        currency = null,
                        isBusiness = Random.nextBoolean()
                    )
                )
            )
            whenever(getPricing(false)).thenReturn(pricing)

            // When
            initializeViewModel()

            // Then
            underTest.pricing.test {
                assertThat(expectMostRecentItem()).isEqualTo(pricing)
            }
        }

    @Test
    fun `test that pricing state is updated with pricing with empty list when the failed to execute the get pricing use case`() =
        runTest {
            // Given
            whenever(getPricing(false)).thenThrow(RuntimeException())

            // When
            initializeViewModel()

            // Then
            underTest.pricing.test {
                val expected = Pricing(emptyList())
                assertThat(expectMostRecentItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that get specific account detail use case is executed when request storage detail if needed and it's not requested recently`() =
        runTest {
            // Given
            whenever(isDatabaseEntryStale()).thenReturn(true)

            // When
            underTest.requestStorageDetailIfNeeded()

            // Then
            verify(getSpecificAccountDetailUseCase).invoke(
                storage = true,
                transfer = false,
                pro = false
            )
        }

    @Test
    fun `test that get specific account detail use case is not executed when request storage detail if needed and it's requested recently`() =
        runTest {
            // Given
            whenever(isDatabaseEntryStale()).thenReturn(false)

            // When
            underTest.requestStorageDetailIfNeeded()

            // Then
            verify(getSpecificAccountDetailUseCase, never()).invoke(
                storage = true,
                transfer = false,
                pro = false
            )
        }

    @Test
    fun `test that view model receives events emitted by the update user data use case`() =
        runTest {
            // Given
            val updateUserDataEvent = MutableSharedFlow<Unit>()
            whenever(monitorUpdateUserDataUseCase()).thenReturn(updateUserDataEvent)

            initializeViewModel()

            underTest.monitorUpdateUserData.test {
                // When
                updateUserDataEvent.emit(Unit)
                updateUserDataEvent.emit(Unit)

                // Then
                assertThat(awaitItem()).isEqualTo(Unit)
                assertThat(awaitItem()).isEqualTo(Unit)
            }
        }

    @Test
    fun `test that get user data use case is executed when get the user data from view model`() =
        runTest {
            // When
            underTest.getUserData()

            // Then
            verify(getUserDataUseCase).invoke()
        }

    @AfterEach
    fun resetMocks() {
        reset(
            isDatabaseEntryStale,
            getSpecificAccountDetailUseCase,
            getPricing,
            getUserDataUseCase,
            monitorUpdateUserDataUseCase
        )
    }

    private fun initializeViewModel() {
        underTest = OverDiskQuotaPaywallViewModel(
            isDatabaseEntryStale,
            getSpecificAccountDetailUseCase,
            getPricing,
            getUserDataUseCase,
            monitorUpdateUserDataUseCase
        )
    }
}
