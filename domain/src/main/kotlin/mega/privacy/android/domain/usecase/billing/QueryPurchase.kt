package mega.privacy.android.domain.usecase.billing

/**
 * Query purchase from billing system (Google, Huawei...)
 *
 */
fun interface QueryPurchase {
    suspend operator fun invoke()
}