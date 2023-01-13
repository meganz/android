package mega.privacy.android.domain.usecase.setting

/**
 * Set file versions option
 *
 */
fun interface EnableFileVersionsOption {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(enabled: Boolean)
}