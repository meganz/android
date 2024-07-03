package mega.privacy.android.app.presentation.cancelaccountplan.model

/**
 * Enum class to define the type of instructions to cancel a subscription.

 */
enum class CancellationInstructionsType {
    /**
     * Instructions to cancel the subscription in the web client.
     */
    WebClient,

    /**
     * Instructions to cancel the subscription in the app store.
     */
    AppStore,

    /**
     * Instructions to cancel the subscription in the play store.
     */
    PlayStore
}