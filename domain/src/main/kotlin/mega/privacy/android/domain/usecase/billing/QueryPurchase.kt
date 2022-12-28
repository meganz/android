package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.billing.MegaPurchase

/**
 * Query purchase from billing system (Google, Huawei...)
 *
 */
fun interface QueryPurchase {
    suspend operator fun invoke(): List<MegaPurchase>
}