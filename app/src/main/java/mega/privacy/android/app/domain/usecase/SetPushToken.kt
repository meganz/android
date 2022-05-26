package mega.privacy.android.app.domain.usecase

interface SetPushToken {

    /**
     * Invoke
     *
     * @param newToken The push token.
     */
    operator fun invoke(newToken: String)
}