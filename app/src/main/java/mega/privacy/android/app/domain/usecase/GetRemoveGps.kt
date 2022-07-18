package mega.privacy.android.app.domain.usecase

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
