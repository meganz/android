package mega.privacy.android.core.ui.utils

/**
 * Create a new [ArrayDeque] with the elements of the [Collection].
 */
fun <T> Collection<T>.toMutableArrayDeque(): ArrayDeque<T> = ArrayDeque(this)

/**
 * Removes the specified element to the end of the list
 */
fun <T> ArrayDeque<T>.pop(): T? = removeLastOrNull()

/**
 * Pushes the specified element to the end of the list.
 * If the list already contains the element, it is moved to the end
 */
fun <T> ArrayDeque<T>.push(element: T) {
    remove(element)
    addLast(element)
}
