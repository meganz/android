package mega.privacy.android.app.extensions

/**
 * Update item on list on specific index
 * @param index Index on which item needs to be updated
 * @param item item to be updated
 * @return [List] updated list with updated item
 */
fun <T> List<T>.updateItemAt(index: Int, item: T): List<T> {
    return if (index in indices) toMutableList().apply { this[index] = item }
    else this
}

/**
 * Move an element of a mutable list to a new position maintaining the order of the other elements and returns it for chaining
 * @param fromIndex Index of the element to move
 * @param toIndex Index of the new position
 * @return the original list reordered for chaining
 */
fun <T> MutableList<T>.moveElement(fromIndex: Int, toIndex: Int): MutableList<T> {
    if (fromIndex == toIndex) return this
    removeAt(fromIndex).also { element ->
        add(toIndex, element)
    }
    return this
}

/**
 * Reorders a collection to match the order of an existing list,
 * placing new items at the end.
 *
 * @param T The type of the elements in the collections.
 * @param K The type of the key used to identify elements.
 * @param currentList The current list that defines the desired order of existing elements.
 * @param keySelector A lambda function that extracts a unique identifying key from an element `T`.
 * @return A new list containing all elements from the original collection.
 * Elements that also exist in `currentList` appear in the same relative order as they had in `currentList`.
 * Elements that only exist in the original collection are appended to the end of the resulting list.
 */
fun <T, K> Collection<T>.matchOrderWithNewAtEnd(
    currentList: List<T>,
    keySelector: (T) -> K,
): List<T> {
    val currentOrderKeys = currentList.map(keySelector)
    val updatedByKey = this.associateBy(keySelector)

    val ordered = currentOrderKeys.mapNotNull { updatedByKey[it] }
    val newItems = this.filter { keySelector(it) !in currentOrderKeys }

    return ordered + newItems
}