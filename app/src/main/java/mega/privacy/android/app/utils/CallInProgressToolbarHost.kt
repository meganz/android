package mega.privacy.android.app.utils

/**
 * Implemented by screens that host their own toolbar and need to adjust its elevation when the
 * "call in progress" banner is shown or hidden.
 *
 * [CallUtil] uses this instead of referencing a concrete Activity so the banner handling works for
 * any host that manages such a toolbar, regardless of which contact-info host is active.
 */
interface CallInProgressToolbarHost {

    /**
     * Update the toolbar elevation to reflect the current visibility of the call-in-progress banner.
     */
    fun changeToolbarLayoutElevation()
}
