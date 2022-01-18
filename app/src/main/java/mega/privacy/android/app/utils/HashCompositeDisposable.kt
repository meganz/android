package mega.privacy.android.app.utils

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.internal.util.ExceptionHelper

/**
 * A disposable that can hold onto multiple other Disposables
 * and only keeps the last Disposable added with the same key.
 *
 * Note: Existing Disposables will be disposed if a new one is added with the same key.
 */
class HashCompositeDisposable : Disposable {

    private var resources: HashMap<Long, Disposable>? = null

    @Volatile
    private var disposed = false

    /**
     * Dispose the resource, the operation should be idempotent.
     */
    override fun dispose() {
        if (disposed) {
            return
        }
        var map: HashMap<Long, Disposable>?
        synchronized(this) {
            if (disposed) {
                return
            }
            disposed = true
            map = resources
            resources = null
        }
        dispose(map)
    }

    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    override fun isDisposed(): Boolean =
        disposed

    fun containsKey(key: Long): Boolean =
        resources?.containsKey(key) ?: false

    fun size(): Int =
        resources?.size ?: 0

    /**
     * Adds a disposable to this container uniquely
     * or disposes it if the container has been disposed.
     *
     * @param key           Unique key related to the disposable
     * @param disposable    Disposable to be added
     * @return              true if successful, false if this container has been disposed
     */
    fun add(key: Long, disposable: Disposable): Boolean {
        if (!disposed) {
            synchronized(this) {
                if (!disposed) {
                    var map = resources
                    if (map == null) {
                        map = hashMapOf()
                        resources = map
                    } else if (map.containsKey(key)) {
                        map[key]?.dispose()
                        map.remove(key)
                    }
                    map[key] = disposable
                    return true
                }
            }
        }
        disposable.dispose()
        return false
    }

    /**
     * Removes and disposes the given disposable if it is part of this
     * container.
     *
     * @param key   The disposable key to remove and dispose
     * @return      true if the operation was successful
     */
    fun remove(key: Long): Boolean {
        if (disposed) {
            return false
        }
        synchronized(this) {
            if (disposed) {
                return false
            }
            val map = resources
            if (map?.containsKey(key) != null) {
                map[key]?.dispose()
                return map.remove(key) != null
            }
        }
        return false
    }

    /**
     * Removes and disposes the given disposable if it is part of this
     * container.
     *
     * @param disposable    The disposable to remove and dispose
     * @return              true if the operation was successful
     */
    fun remove(disposable: Disposable): Boolean {
        if (disposed) {
            return false
        }
        synchronized(this) {
            if (disposed) {
                return false
            }
            val map = resources
            map?.entries?.find { it.value == disposable }?.let { entry ->
                entry.value.dispose()
                return map.remove(entry.key) != null
            }
        }
        return false
    }

    /**
     * Clear and dispose all contained disposables
     */
    fun clear() {
        if (disposed) {
            return
        }
        var map: HashMap<Long, Disposable>?
        synchronized(this) {
            if (disposed) {
                return
            }
            map = resources
            resources = null
        }
        dispose(map)
    }

    /**
     * Dispose the map resource.
     */
    private fun dispose(map: HashMap<Long, Disposable>?) {
        val disposables = map?.values ?: return
        val errors = mutableListOf<Throwable>()
        disposables.forEach { disposable ->
            try {
                disposable.dispose()
            } catch (ex: Throwable) {
                Exceptions.throwIfFatal(ex)
                errors.add(ex)
            }
        }
        if (errors.isNotEmpty()) {
            if (errors.size == 1) {
                throw ExceptionHelper.wrapOrThrow(errors[0])
            }
            throw CompositeException(errors)
        }
    }
}
