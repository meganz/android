package mega.privacy.android.app.utils.greeter

/**
 * Greeter is a debugging tool by displaying a Toast showing Android UI component's name
 * when developer opens them.
 *
 * Greeter currently supports following components:
 * Activity, Fragment, DialogFragment, or BottomDialogFragment.
 *
 * The purpose is to ease developer get familiarized faster to screens in Mega application.
 * It can also help developer to reason navigation relationship between components easier.
 */
fun interface Greeter {
    /**
     * Initialize to ignite the engine
     */
    fun initialize()
}
