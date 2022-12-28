package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.account.MegaSku

/**
 * Query skus from billing system (Google, Huawei...)
 *
 */
fun interface QuerySkus {
    suspend operator fun invoke() : List<MegaSku>
}