package mega.privacy.android.navigation.contract.home

/**
 * Home widget provider
 * If you have multiple widgets of the same type (e.g multiple file widgets for different files), then you need to provide an implementation
 * of this interface and inject it into a set. Otherwise you can just provide an implementation of [HomeWidget] (Also injected into a set)
 *
 */
fun interface HomeWidgetProvider {
    suspend fun getWidgets(): Set<HomeWidget>
}