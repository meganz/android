package mega.privacy.android.data.mapper

import com.android.billingclient.api.ProductDetails
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.OfferPeriod
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MegaSkuMapperTest {
    private lateinit var underTest: MegaSkuMapper

    @Before
    fun setUp() {
        underTest = MegaSkuMapper()
    }

    @Test
    fun `test that mapper returns null when no subscription offer details`() {
        runTest {
            val productDetails = mock<ProductDetails>()
            whenever(productDetails.subscriptionOfferDetails).thenReturn(null)

            val result = underTest(productDetails)

            assertThat(result).isNull()
        }
    }

    @Test
    fun `test that mapper returns null when empty subscription offer details`() {
        runTest {
            val productDetails = mock<ProductDetails>()
            whenever(productDetails.subscriptionOfferDetails).thenReturn(emptyList())

            val result = underTest(productDetails)

            assertThat(result).isNull()
        }
    }

    @Test
    fun `test that mapper creates MegaSku with base price from offer without offerId`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 1000000000L, // $10.00
                baseCurrencyCode = "USD",
                offers = emptyList()
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.sku).isEqualTo("test.product")
            assertThat(result?.priceAmountMicros).isEqualTo(1000000000L)
            assertThat(result?.priceCurrencyCode).isEqualTo("USD")
            assertThat(result?.offers).isEmpty()
        }
    }

    @Test
    fun `test discount percentage calculation with 50 discount`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 2000000000L, // $20.00
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "discount-50",
                        introPriceMicros = 1000000000L, // $10.00 (50% off)
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M", // 1 month
                        regularPriceMicros = 2000000000L, // $20.00
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M" // 1 month
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.offerId).isEqualTo("discount-50")
            assertThat(offer?.discountPercentage).isEqualTo(50) // 50% discount
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(1000000000L) // $10.00 monthly
            )
        }
    }

    @Test
    fun `test discount percentage calculation with 25 discount`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 4000000000L, // $40.00
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "discount-25",
                        introPriceMicros = 3000000000L, // $30.00 (25% off)
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M", // 1 month
                        regularPriceMicros = 4000000000L, // $40.00
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M" // 1 month
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.discountPercentage).isEqualTo(25) // 25% discount
        }
    }

    @Test
    fun `test monthly price calculation for yearly subscription`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 12000000000L, // $120.00 yearly
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "yearly-discount",
                        introPriceMicros = 6000000000L, // $60.00 yearly (50% off)
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1Y", // 1 year
                        regularPriceMicros = 12000000000L, // $120.00 yearly
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1Y" // 1 year
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.discountPercentage).isEqualTo(50) // 50% discount
            // Monthly price should be $60.00 / 12 = $5.00 = 500000000L micros
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(500000000L)
            )
        }
    }

    @Test
    fun `test monthly price calculation for 3-month subscription`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 3000000000L, // $30.00 for 3 months
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "quarterly-discount",
                        introPriceMicros = 1500000000L, // $15.00 for 3 months (50% off)
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P3M", // 3 months
                        regularPriceMicros = 3000000000L, // $30.00 for 3 months
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P3M" // 3 months
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.discountPercentage).isEqualTo(50) // 50% discount
            // Monthly price should be $15.00 / 3 = $5.00 = 500000000L micros
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(500000000L)
            )
        }
    }

    @Test
    fun `test roundToInt logic with 33_33 discount`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 3000000000L, // $30.00
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "round-test",
                        introPriceMicros = 2000000000L, // $20.00 (33.33% off)
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M", // 1 month
                        regularPriceMicros = 3000000000L, // $30.00
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M" // 1 month
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            // Should round 33.33% to 33%
            assertThat(offer?.discountPercentage).isEqualTo(33)
        }
    }

    @Test
    fun `test that offers without multiple pricing phases are filtered out`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 1000000000L,
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOfferWithSinglePhase(
                        offerId = "single-phase",
                        priceMicros = 1000000000L,
                        currencyCode = "USD",
                        billingPeriod = "P1M"
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).isEmpty() // Should be filtered out
        }
    }

    @Test
    fun `test that offers with empty offerId are filtered out`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 1000000000L,
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "", // Empty offerId
                        introPriceMicros = 500000000L,
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M",
                        regularPriceMicros = 1000000000L,
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M"
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).isEmpty() // Should be filtered out
        }
    }

    @Test
    fun `test offer period creation for monthly subscription`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 1000000000L,
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "monthly-offer",
                        introPriceMicros = 500000000L,
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M", // 1 month
                        regularPriceMicros = 1000000000L,
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M"
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Month::class.java)
            assertThat((offer?.offerPeriod as OfferPeriod.Month).value).isEqualTo(1)
        }
    }

    @Test
    fun `test offer period creation for yearly subscription`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 12000000000L,
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "yearly-offer",
                        introPriceMicros = 6000000000L,
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1Y", // 1 year
                        regularPriceMicros = 12000000000L,
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1Y"
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Year::class.java)
            assertThat((offer?.offerPeriod as OfferPeriod.Year).value).isEqualTo(1)
        }
    }

    @Test
    fun `test discount calculation with intro monthly and regular yearly`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 12000000000L, // $120.00 yearly
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "intro-monthly-regular-yearly",
                        introPriceMicros = 500000000L, // $5.00 monthly
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1M", // 1 month intro
                        regularPriceMicros = 12000000000L, // $120.00 yearly regular
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1Y" // 1 year regular
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            // Intro: $5.00/month, Regular: $120.00/year = $10.00/month
            // Discount: (10.00 - 5.00) / 10.00 * 100 = 50%
            assertThat(offer?.discountPercentage).isEqualTo(50)
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(500000000L) // $5.00 monthly
            )
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Month::class.java)
        }
    }

    @Test
    fun `test discount calculation with intro yearly and regular monthly`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 1200000000L, // $12.00 monthly
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "intro-yearly-regular-monthly",
                        introPriceMicros = 6000000000L, // $60.00 yearly intro
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1Y", // 1 year intro
                        regularPriceMicros = 1200000000L, // $12.00 monthly regular
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1M" // 1 month regular
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            // Intro: $60.00/year = $5.00/month, Regular: $12.00/month
            // Discount: (12.00 - 5.00) / 12.00 * 100 = 58%
            assertThat(offer?.discountPercentage).isEqualTo(58)
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(500000000L) // $5.00 monthly (60/12)
            )
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Year::class.java)
        }
    }

    @Test
    fun `test discount calculation with intro 3-month and regular yearly`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 12000000000L, // $120.00 yearly
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "intro-3month-regular-yearly",
                        introPriceMicros = 1500000000L, // $15.00 for 3 months
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P3M", // 3 months intro
                        regularPriceMicros = 12000000000L, // $120.00 yearly regular
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P1Y" // 1 year regular
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            // Intro: $15.00/3months = $5.00/month, Regular: $120.00/year = $10.00/month
            // Discount: (10.00 - 5.00) / 10.00 * 100 = 50%
            assertThat(offer?.discountPercentage).isEqualTo(50)
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(500000000L) // $5.00 monthly (15/3)
            )
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Month::class.java)
            assertThat((offer?.offerPeriod as OfferPeriod.Month).value).isEqualTo(3)
        }
    }

    @Test
    fun `test discount calculation with intro yearly and regular 6-month`() {
        runTest {
            val productDetails = createProductDetails(
                productId = "test.product",
                basePriceMicros = 6000000000L, // $60.00 for 6 months
                baseCurrencyCode = "USD",
                offers = listOf(
                    createOffer(
                        offerId = "intro-yearly-regular-6month",
                        introPriceMicros = 12000000000L, // $120.00 yearly intro
                        introCurrencyCode = "USD",
                        introBillingPeriod = "P1Y", // 1 year intro
                        regularPriceMicros = 6000000000L, // $60.00 for 6 months regular
                        regularCurrencyCode = "USD",
                        regularBillingPeriod = "P6M" // 6 months regular
                    )
                )
            )

            val result = underTest(productDetails)

            assertThat(result).isNotNull()
            assertThat(result?.offers).hasSize(1)

            val offer = result?.offers?.first()
            // Intro: $120.00/year = $10.00/month, Regular: $60.00/6months = $10.00/month
            // Discount: (10.00 - 10.00) / 10.00 * 100 = 0%
            assertThat(offer?.discountPercentage).isEqualTo(0)
            assertThat(offer?.discountedPriceMonthly).isEqualTo(
                CurrencyPoint.LocalCurrencyPoint(1000000000L) // $10.00 monthly (120/12)
            )
            assertThat(offer?.offerPeriod).isInstanceOf(OfferPeriod.Year::class.java)
        }
    }

    // Helper methods to create test data
    private fun createProductDetails(
        productId: String,
        basePriceMicros: Long,
        baseCurrencyCode: String,
        offers: List<Any>,
    ): ProductDetails {
        val productDetails = mock<ProductDetails>()
        whenever(productDetails.productId).thenReturn(productId)

        val baseOffer = mock<ProductDetails.SubscriptionOfferDetails>()
        whenever(baseOffer.offerId).thenReturn(null) // Base offer has no offerId

        val basePricingPhase = mock<ProductDetails.PricingPhase>()
        whenever(basePricingPhase.priceAmountMicros).thenReturn(basePriceMicros)
        whenever(basePricingPhase.priceCurrencyCode).thenReturn(baseCurrencyCode)

        val basePricingPhases = mock<ProductDetails.PricingPhases>()
        whenever(basePricingPhases.pricingPhaseList).thenReturn(listOf(basePricingPhase))
        whenever(baseOffer.pricingPhases).thenReturn(basePricingPhases)

        val allOffers = listOf(baseOffer) + offers.map { createMockOffer(it) }
        whenever(productDetails.subscriptionOfferDetails).thenReturn(allOffers)

        return productDetails
    }

    private fun createOffer(
        offerId: String,
        introPriceMicros: Long,
        introCurrencyCode: String,
        introBillingPeriod: String,
        regularPriceMicros: Long,
        regularCurrencyCode: String,
        regularBillingPeriod: String,
    ): Any = mapOf(
        "offerId" to offerId,
        "introPriceMicros" to introPriceMicros,
        "introCurrencyCode" to introCurrencyCode,
        "introBillingPeriod" to introBillingPeriod,
        "regularPriceMicros" to regularPriceMicros,
        "regularCurrencyCode" to regularCurrencyCode,
        "regularBillingPeriod" to regularBillingPeriod
    )

    private fun createOfferWithSinglePhase(
        offerId: String,
        priceMicros: Long,
        currencyCode: String,
        billingPeriod: String,
    ): Any = mapOf(
        "offerId" to offerId,
        "priceMicros" to priceMicros,
        "currencyCode" to currencyCode,
        "billingPeriod" to billingPeriod,
        "singlePhase" to true
    )

    private fun createMockOffer(offerData: Any): ProductDetails.SubscriptionOfferDetails {
        val offer = mock<ProductDetails.SubscriptionOfferDetails>()

        when (offerData) {
            is Map<*, *> -> {
                val offerId = offerData["offerId"] as String
                whenever(offer.offerId).thenReturn(offerId)

                if (offerData.containsKey("singlePhase")) {
                    // Single phase offer
                    val priceMicros = offerData["priceMicros"] as Long
                    val currencyCode = offerData["currencyCode"] as String
                    val billingPeriod = offerData["billingPeriod"] as String

                    val pricingPhase = mock<ProductDetails.PricingPhase>()
                    whenever(pricingPhase.priceAmountMicros).thenReturn(priceMicros)
                    whenever(pricingPhase.priceCurrencyCode).thenReturn(currencyCode)
                    whenever(pricingPhase.billingPeriod).thenReturn(billingPeriod)

                    val pricingPhases = mock<ProductDetails.PricingPhases>()
                    whenever(pricingPhases.pricingPhaseList).thenReturn(listOf(pricingPhase))
                    whenever(offer.pricingPhases).thenReturn(pricingPhases)
                } else {
                    // Multi-phase offer
                    val introPriceMicros = offerData["introPriceMicros"] as Long
                    val introCurrencyCode = offerData["introCurrencyCode"] as String
                    val introBillingPeriod = offerData["introBillingPeriod"] as String
                    val regularPriceMicros = offerData["regularPriceMicros"] as Long
                    val regularCurrencyCode = offerData["regularCurrencyCode"] as String
                    val regularBillingPeriod = offerData["regularBillingPeriod"] as String

                    val introPhase = mock<ProductDetails.PricingPhase>()
                    whenever(introPhase.priceAmountMicros).thenReturn(introPriceMicros)
                    whenever(introPhase.priceCurrencyCode).thenReturn(introCurrencyCode)
                    whenever(introPhase.billingPeriod).thenReturn(introBillingPeriod)
                    whenever(introPhase.recurrenceMode).thenReturn(ProductDetails.RecurrenceMode.FINITE_RECURRING)
                    whenever(introPhase.billingCycleCount).thenReturn(1)

                    val regularPhase = mock<ProductDetails.PricingPhase>()
                    whenever(regularPhase.priceAmountMicros).thenReturn(regularPriceMicros)
                    whenever(regularPhase.priceCurrencyCode).thenReturn(regularCurrencyCode)
                    whenever(regularPhase.billingPeriod).thenReturn(regularBillingPeriod)
                    whenever(regularPhase.recurrenceMode).thenReturn(ProductDetails.RecurrenceMode.INFINITE_RECURRING)
                    whenever(regularPhase.billingCycleCount).thenReturn(0)

                    val pricingPhases = mock<ProductDetails.PricingPhases>()
                    whenever(pricingPhases.pricingPhaseList).thenReturn(
                        listOf(
                            introPhase,
                            regularPhase
                        )
                    )
                    whenever(offer.pricingPhases).thenReturn(pricingPhases)
                }
            }
        }

        return offer
    }
}