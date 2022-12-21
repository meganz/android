package mega.privacy.android.domain.usecase.billing

/**
 * Query skus from billing system (Google, Huawei...)
 *
 */
fun interface QuerySkus {
    suspend operator fun invoke()
}