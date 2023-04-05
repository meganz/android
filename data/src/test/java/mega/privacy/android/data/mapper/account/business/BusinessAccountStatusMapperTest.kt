package mega.privacy.android.data.mapper.account.business

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

internal class BusinessAccountStatusMapperTest {
    private val underTest = BusinessAccountStatusMapper()

    @TestFactory
    fun `test business account status mapping`() =
        listOf(
            MegaApiJava.BUSINESS_STATUS_EXPIRED to BusinessAccountStatus.Expired,
            MegaApiJava.BUSINESS_STATUS_INACTIVE to BusinessAccountStatus.Inactive,
            MegaApiJava.BUSINESS_STATUS_ACTIVE to BusinessAccountStatus.Active,
            MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD to BusinessAccountStatus.GracePeriod,
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("test that $input is mapped to $expected") {
                Truth.assertThat(underTest(input)).isEqualTo(expected)
            }
        }

    @Test
    internal fun `test that unknown value throws an exception`() {
        assertThrows<IllegalArgumentException>() { underTest(-1234567) }
    }
}