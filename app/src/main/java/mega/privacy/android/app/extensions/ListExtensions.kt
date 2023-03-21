package mega.privacy.android.app.extensions

/**
 * Update item on list on specific index
 * @param index Index on which item needs to be updated
 * @param item item to be updated
 * @return [List] updated list with updated item
 */
fun <T> List<T>.updateItemAt(index: Int, item: T): List<T> =
    toMutableList().apply { this[index] = item }