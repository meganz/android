package mega.privacy.android.app.activities

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionWithOfferUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.SubscriptionOfferNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.SubscriptionOfferSource
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpgradeAccountDeepLinkHandlerTest {
    private lateinit var underTest: UpgradeAccountDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val getRecommendedSubscriptionWithOfferUseCase =
        mock<GetRecommendedSubscriptionWithOfferUseCase>()

    @BeforeAll
    fun setup() {
        underTest = UpgradeAccountDeepLinkHandler(
            getRecommendedSubscriptionWithOfferUseCase = getRecommendedSubscriptionWithOfferUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(snackbarEventQueue, getRecommendedSubscriptionWithOfferUseCase)
    }

    private fun megaUpgradeUri(offer: String? = null) = mock<Uri> {
        on { scheme } doReturn "mega"
        on { host } doReturn "upgrade"
        on { getQueryParameter("offer") } doReturn offer
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches UPGRADE_PAGE_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/upgrade"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest
            .getNavKeysInternal(uri, RegexPatternType.UPGRADE_PAGE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(UpgradeAccountNavKey())
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches UPGRADE_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/pro"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.UPGRADE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(UpgradeAccountNavKey())
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @Test
    fun `test that the offer link opens the subscription offer screen when an offer is available`() =
        runTest {
            whenever(getRecommendedSubscriptionWithOfferUseCase())
                .thenReturn(mock<RecommendedSubscriptionOffer>())

            val actual = underTest.getNavKeysInternal(megaUpgradeUri(offer = "1"), null, true)

            assertThat(actual)
                .containsExactly(SubscriptionOfferNavKey(SubscriptionOfferSource.Notification))
        }

    @Test
    fun `test that the offer link opens the upgrade screen when no offer is available`() = runTest {
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(null)

        val actual = underTest.getNavKeysInternal(megaUpgradeUri(offer = "1"), null, true)

        assertThat(actual).containsExactly(UpgradeAccountNavKey())
    }

    @Test
    fun `test that the offer link opens the upgrade screen when the offer cannot be fetched`() =
        runTest {
            whenever(getRecommendedSubscriptionWithOfferUseCase())
                .thenAnswer { throw RuntimeException("Billing unavailable") }

            val actual = underTest.getNavKeysInternal(megaUpgradeUri(offer = "1"), null, true)

            assertThat(actual).containsExactly(UpgradeAccountNavKey())
        }

    @Test
    fun `test that the upgrade link without an offer opens the upgrade screen`() = runTest {
        val actual = underTest.getNavKeysInternal(megaUpgradeUri(), null, true)

        assertThat(actual).containsExactly(UpgradeAccountNavKey())
        verifyNoInteractions(getRecommendedSubscriptionWithOfferUseCase)
    }

    @Test
    fun `test that the offer link shows a message and does not look up the offer when the user is not logged in`() =
        runTest {
            val actual = underTest.getNavKeysInternal(megaUpgradeUri(offer = "1"), null, false)

            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
            verifyNoInteractions(getRecommendedSubscriptionWithOfferUseCase)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match upgrade pattern types`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

