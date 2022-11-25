package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionStatus
import nz.mega.sdk.MegaAccountDetails
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SubscriptionStatusMapperTest {
    @Test
    fun `test that subscription status can be mapped correctly`() {
        val unknownSubscriptionStatus = -1
        val expectedResults = HashMap<Int, SubscriptionStatus?>().apply {
            put(MegaAccountDetails.SUBSCRIPTION_STATUS_VALID, SubscriptionStatus.VALID)
            put(MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID, SubscriptionStatus.INVALID)
            put(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE, SubscriptionStatus.NONE)
            put(unknownSubscriptionStatus, null)
        }

        expectedResults.forEach { (key, value) ->
            val actual = toSubscriptionStatus(key)
            assertEquals(value, actual)
        }
    }
}