package mega.privacy.android.domain.usecase.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.billing.GetAppSubscriptionOptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionOptionsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetAppSubscriptionOptionsUseCaseTest {
    private lateinit var underTest: GetAppSubscriptionOptionsUseCase
    private val accountRepository = mock<AccountRepository>()
    private val getSubscriptionOptionsUseCase = GetSubscriptionOptionsUseCase(accountRepository)

    private val subscriptionPlanProIMonthly = SubscriptionOption(
        handle = 1560943707714440503,
        accountType = AccountType.PRO_I,
        months = 1,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyPoint.SystemCurrencyPoint(999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProIYearly = SubscriptionOption(
        handle = 7472683699866478542,
        accountType = AccountType.PRO_I,
        months = 12,
        storage = 2048,
        transfer = 24576,
        amount = CurrencyPoint.SystemCurrencyPoint(9999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProIIMonthly = SubscriptionOption(
        handle = 7974113413762509455,
        accountType = AccountType.PRO_II,
        months = 1,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyPoint.SystemCurrencyPoint(1999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProIIYearly = SubscriptionOption(
        handle = 370834413380951543,
        accountType = AccountType.PRO_II,
        months = 12,
        storage = 8192,
        transfer = 98304,
        amount = CurrencyPoint.SystemCurrencyPoint(19999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProIIIMonthly = SubscriptionOption(
        handle = -2499193043825823892,
        accountType = AccountType.PRO_III,
        months = 1,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyPoint.SystemCurrencyPoint(2999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProIIIYearly = SubscriptionOption(
        handle = 7225413476571973499,
        accountType = AccountType.PRO_III,
        months = 12,
        storage = 16384,
        transfer = 196608,
        amount = CurrencyPoint.SystemCurrencyPoint(29999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProLiteMonthly = SubscriptionOption(
        handle = -4226692769210777158,
        accountType = AccountType.PRO_LITE,
        months = 1,
        storage = 400,
        transfer = 1024,
        amount = CurrencyPoint.SystemCurrencyPoint(499.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProLiteYearly = SubscriptionOption(
        handle = -5517769810977460898,
        accountType = AccountType.PRO_LITE,
        months = 12,
        storage = 400,
        transfer = 12288,
        amount = CurrencyPoint.SystemCurrencyPoint(4999.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanBusiness = SubscriptionOption(
        handle = 7472683699866478542,
        accountType = AccountType.BUSINESS,
        months = 1,
        storage = -1,
        transfer = -1,
        amount = CurrencyPoint.SystemCurrencyPoint(0.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanProFlexi = SubscriptionOption(
        handle = 7472683699866478511,
        accountType = AccountType.PRO_FLEXI,
        months = 1,
        storage = -1,
        transfer = -1,
        amount = CurrencyPoint.SystemCurrencyPoint(1.toLong()),
        currency = Currency("EUR"),
    )

    private val subscriptionPlanUnknown = SubscriptionOption(
        handle = 7472683699866478511,
        accountType = AccountType.UNKNOWN,
        months = 1,
        storage = -1,
        transfer = -1,
        amount = CurrencyPoint.SystemCurrencyPoint(1.toLong()),
        currency = Currency("EUR"),
    )

    private val fullListOfSubscriptionOptions = listOf(
        subscriptionPlanProLiteMonthly,
        subscriptionPlanProLiteYearly,
        subscriptionPlanProIMonthly,
        subscriptionPlanProIYearly,
        subscriptionPlanProIIMonthly,
        subscriptionPlanProIIYearly,
        subscriptionPlanProIIIMonthly,
        subscriptionPlanProIIIYearly,
        subscriptionPlanBusiness,
        subscriptionPlanProFlexi,
        subscriptionPlanUnknown
    )

    private val expectedResult = listOf(
        subscriptionPlanProLiteMonthly,
        subscriptionPlanProIMonthly,
        subscriptionPlanProIIMonthly,
        subscriptionPlanProIIIMonthly
    )

    @Before
    fun setUp() {
        underTest = GetAppSubscriptionOptionsUseCase(
            getSubscriptionOptionsUseCase = getSubscriptionOptionsUseCase
        )
    }

    @Test
    fun `test that subscription options are filtered correctly`() {
        runTest {

            whenever(accountRepository.getSubscriptionOptions()).thenReturn(
                fullListOfSubscriptionOptions
            )

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(expectedResult)
        }
    }
}