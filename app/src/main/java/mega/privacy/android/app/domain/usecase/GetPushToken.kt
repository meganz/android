package mega.privacy.android.app.domain.usecase

/**
 * Gets push token use case.
 */
interface GetPushToken {

    /**
     * Invoke
     *
     * @return Push token.
     */
    operator fun invoke(): String
}