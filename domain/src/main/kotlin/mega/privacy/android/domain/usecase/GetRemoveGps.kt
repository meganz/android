package mega.privacy.android.domain.usecase

/**
 * Get remove GPS setting
 *
 */
interface GetRemoveGps {

    /**
     * Invoke
     *
     * @return remove GPS setting
     */
    operator fun invoke(): Boolean
}
