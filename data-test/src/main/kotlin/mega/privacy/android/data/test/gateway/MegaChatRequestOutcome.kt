package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest

/**
 * Outcome delivered to the [nz.mega.sdk.MegaChatRequestListenerInterface] of a listener-based
 * [mega.privacy.android.data.gateway.api.MegaChatApiGateway] method.
 *
 * @property request Request passed to the listener callbacks, or null to let the fake build a
 * default stub request carrying the method's request type.
 * @property error Error delivered in onRequestFinish.
 */
data class MegaChatRequestOutcome(
    val request: MegaChatRequest?,
    val error: MegaChatError,
)
