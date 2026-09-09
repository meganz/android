package mega.privacy.android.data.test.state

/**
 * Mutable account-level defaults backing a fake [mega.privacy.android.data.gateway.api.MegaApiGateway].
 *
 * Tests mutate the fields directly to change what account-related gateway members return.
 * Defaults describe a healthy, logged-in free account.
 */
class FakeAccountState {

    /** Backs logged-in checks; true means a valid session exists. */
    var isLoggedIn: Boolean = true

    /** Email of the logged-in user. */
    var email: String = "test@mega.nz"

    /** Handle of the logged-in user. */
    var myUserHandle: Long = 111L

    /** Session key returned by session dumps. */
    var session: String = DEFAULT_SESSION

    /** Whether the account is a business account. */
    var isBusinessAccount: Boolean = false

    /** Whether the account is a master business account. */
    var isMasterBusinessAccount: Boolean = false

    /** Whether the business account is currently active. */
    var isBusinessAccountActive: Boolean = true

    /** Business status as reported by the SDK. */
    var businessStatus: Int = 0

    /** Whether achievements are enabled for the account. */
    var isAchievementsEnabled: Boolean = true

    /** Whether the account is an ephemeral plus plus account. */
    var isEphemeralPlusPlus: Boolean = false

    /** Credentials of the logged-in user. */
    var myCredentials: String? = "fake-credentials"

    /** Restore every field to its default value. */
    fun reset() {
        isLoggedIn = true
        email = "test@mega.nz"
        myUserHandle = 111L
        session = DEFAULT_SESSION
        isBusinessAccount = false
        isMasterBusinessAccount = false
        isBusinessAccountActive = true
        businessStatus = 0
        isAchievementsEnabled = true
        isEphemeralPlusPlus = false
        myCredentials = "fake-credentials"
    }

    companion object {

        /**
         * Instrumented tests share the app package, so whatever the fakes report as a session is
         * persisted to the real credential store and read back on the next ordinary launch. It is
         * padded to the length of a session the SDK would actually issue, because a shorter one is
         * rejected by the chat SDK and treated as no credentials at all.
         */
        const val DEFAULT_SESSION =
            "fake-session-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    }
}
