package mega.privacy.android.feature.payment.presentation.upgrade

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UpgradeAccountPurchaseUpdatedTest {

    @ParameterizedTest(name = "source: {0}")
    @EnumSource(UpgradeAccountSource::class)
    fun `test that onPurchasesUpdated completes the purchase when it is not an upgrade`(
        source: UpgradeAccountSource,
    ) {
        var completed = false

        onPurchasesUpdated(
            isUpgradeAccount = false,
            openFromSource = source,
            onPurchaseCompleted = { completed = true },
        )

        assertThat(completed).isTrue()
    }

    @Test
    fun `test that onPurchasesUpdated completes the purchase when upgrading from my account`() {
        var completed = false

        onPurchasesUpdated(
            isUpgradeAccount = true,
            openFromSource = UpgradeAccountSource.MY_ACCOUNT_SCREEN,
            onPurchaseCompleted = { completed = true },
        )

        assertThat(completed).isTrue()
    }

    @ParameterizedTest(name = "source: {0}")
    @EnumSource(
        value = UpgradeAccountSource::class,
        names = ["MY_ACCOUNT_SCREEN"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that onPurchasesUpdated keeps the screen open when upgrading from other sources`(
        source: UpgradeAccountSource,
    ) {
        var completed = false

        onPurchasesUpdated(
            isUpgradeAccount = true,
            openFromSource = source,
            onPurchaseCompleted = { completed = true },
        )

        assertThat(completed).isFalse()
    }
}
