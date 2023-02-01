package mega.privacy.android.domain.usecase

/**
 * The use case to check if mobile data allowed
 */
fun interface IsMobileDataAllowed {

    /**
     * check if mobile data allowed
     *
     * @return [Boolean]
     */
    suspend operator fun invoke(): Boolean
}