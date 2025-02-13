package mega.privacy.android.app.presentation.container

import androidx.compose.runtime.Composable

/**
 * App container
 *
 * @param containers - ordered from inner to outer container
 * @param content
 */
@Composable
internal fun AppContainer(
    containers: List<@Composable (@Composable () -> Unit) -> Unit>,
    content: @Composable () -> Unit,
) {
    containers.fold(content) { acc, container -> { container(acc) } }()
}
