package mega.privacy.android.shared.resources

import androidx.annotation.StringRes

/**
 * Provider that maps an input value to a string resource ID.
 * Implementations are typically provided by feature modules (e.g. account type name from myaccount).
 *
 * @param T the type of input (e.g. account type for account name string resource)
 */
interface SharedStringResourceProvider<T> {

    /**
     * Returns the string resource ID for the given input.
     *
     * @param input the input to map to a string resource
     * @return the string resource ID
     */
    @StringRes
    operator fun invoke(input: T): Int
}
