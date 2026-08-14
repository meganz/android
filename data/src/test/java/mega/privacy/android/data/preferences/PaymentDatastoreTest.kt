package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PaymentDatastoreTest {

    private lateinit var underTest: PaymentDatastore

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val paymentPreferenceDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { context.preferencesDataStoreFile(paymentPreferenceFileName) }
        )

    @Before
    fun setup() {
        underTest = PaymentDatastore(paymentPreferenceDataStore = paymentPreferenceDataStore)
    }

    @Test
    fun `test that no subscription offer campaign is dismissed by default`() = runTest {
        underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).isEmpty()
        }
    }

    @Test
    fun `test that a subscription offer campaign is dismissed after it is added`() = runTest {
        underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that dismissing a subscription offer campaign keeps the previously dismissed ones`() =
        runTest {
            underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)
            underTest.addDismissedSubscriptionOfferCampaign(userHandle, otherCampaignId)

            underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
                assertThat(expectMostRecentItem())
                    .containsExactly(campaignId, otherCampaignId)
            }
        }

    @Test
    fun `test that dismissing a subscription offer campaign twice keeps a single entry`() = runTest {
        underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)
        underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that dismissing a subscription offer campaign does not dismiss it for another user`() =
        runTest {
            underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)

            underTest.monitorDismissedSubscriptionOfferCampaigns(otherUserHandle).test {
                assertThat(expectMostRecentItem()).isEmpty()
            }
            underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
                assertThat(expectMostRecentItem()).containsExactly(campaignId)
            }
        }

    @Test
    fun `test that no subscription offer menu campaign is dismissed by default`() = runTest {
        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).isEmpty()
        }
    }

    @Test
    fun `test that a subscription offer menu campaign is dismissed after it is added`() = runTest {
        underTest.addDismissedSubscriptionOfferMenuCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that dismissing a menu campaign does not dismiss it on the home banner`() = runTest {
        underTest.addDismissedSubscriptionOfferMenuCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).isEmpty()
        }
        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that dismissing a home banner campaign does not dismiss it on the menu`() = runTest {
        underTest.addDismissedSubscriptionOfferCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).isEmpty()
        }
        underTest.monitorDismissedSubscriptionOfferCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that dismissing a menu campaign does not dismiss it for another user`() = runTest {
        underTest.addDismissedSubscriptionOfferMenuCampaign(userHandle, campaignId)

        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(otherUserHandle).test {
            assertThat(expectMostRecentItem()).isEmpty()
        }
        underTest.monitorDismissedSubscriptionOfferMenuCampaigns(userHandle).test {
            assertThat(expectMostRecentItem()).containsExactly(campaignId)
        }
    }

    @Test
    fun `test that the subscription offer last shown time is null by default`() = runTest {
        assertThat(underTest.getSubscriptionOfferLastShownTime(userHandle)).isNull()
    }

    @Test
    fun `test that the subscription offer last shown time is returned after it is set`() = runTest {
        underTest.setSubscriptionOfferLastShownTime(
            userHandle = userHandle,
            timeInMillis = lastShownTime,
        )

        assertThat(underTest.getSubscriptionOfferLastShownTime(userHandle)).isEqualTo(lastShownTime)
    }

    @Test
    fun `test that setting the subscription offer last shown time does not set it for another user`() =
        runTest {
            underTest.setSubscriptionOfferLastShownTime(
                userHandle = userHandle,
                timeInMillis = lastShownTime,
            )

            assertThat(underTest.getSubscriptionOfferLastShownTime(otherUserHandle)).isNull()
        }

    private companion object {
        const val userHandle = 123L
        const val otherUserHandle = 456L
        const val campaignId = 90210L
        const val otherCampaignId = 90211L
        const val lastShownTime = 1_700_000_000_000L
    }
}
