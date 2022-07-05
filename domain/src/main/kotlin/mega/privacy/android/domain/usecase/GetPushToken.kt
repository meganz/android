package mega.privacy.android.domain.usecase

/**
 * Gets push token use case.
 */
fun interface GetPushToken {

    /**
     * Invoke
     *
     * @return Push token.
     */
    operator fun invoke(): String
}