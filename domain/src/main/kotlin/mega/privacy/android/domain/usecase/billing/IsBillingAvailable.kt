package mega.privacy.android.domain.usecase.billing

/**
 * Is billing available
 *
 */
fun interface IsBillingAvailable {
    /**
     * Invoke
     *
     * @return true if skus from billing is not empty otherwise false
     */
    operator fun invoke(): Boolean
}