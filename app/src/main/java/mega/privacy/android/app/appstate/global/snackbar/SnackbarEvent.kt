package mega.privacy.android.app.appstate.global.snackbar

import mega.android.core.ui.model.SnackbarAttributes

/**
 * Wrapper that ensures each snackbar event has a unique identity for compose effects
 *
 * Compose effects uses the event as the unique key. When the same [SnackbarAttributes]
 * is emitted repeatedly, effect will not re-trigger because the key appears unchanged.
 * Adding [uniqueId] ensures each emission is distinct, so every snackbar is shown and consumed correctly.
 *
 * @param attributes The snackbar content / attributes.
 * @param uniqueId Monotonically increasing ID that uniquely identifies this emission.
 */
internal data class SnackbarEvent(
    val attributes: SnackbarAttributes,
    val uniqueId: Long,
)