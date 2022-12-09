package mega.privacy.android.app.upgradeAccount

import mega.privacy.android.domain.entity.Product
import java.util.BitSet

internal data class ChooseUpgradeAccountState(
    val paymentBitSet: BitSet = BitSet(),
    val product: List<Product> = emptyList(),
)