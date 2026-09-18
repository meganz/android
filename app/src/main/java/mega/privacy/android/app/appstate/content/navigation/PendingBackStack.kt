package mega.privacy.android.app.appstate.content.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.StateObject
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

@Serializable
class PendingBackStack<T : NavKey>(
    internal val base: NavBackStack<T>,
) : MutableList<T> by base, StateObject by base, RandomAccess by base {

    /**
     * Navigation requests that have been queued, in the order they were made.
     *
     * A plain navigation is only ever queued once: when its destination is already carried by an
     * earlier request it is dropped from the later one, and a request left without destinations is
     * dropped entirely. A request carrying an operation or options keeps every destination, because
     * dropping the last one would take the back stack manipulation the caller asked for with it.
     */
    var pendingNavigations: List<PendingNavigation<T>> = emptyList()
        set(value) {
            field = value.withoutRepeatedDestinations()
        }

    /**
     * The destinations of [pendingNavigations], flattened in request order and without repeats.
     *
     * A destination can be carried by more than one queued request, so readers that only care about
     * what is queued - rather than about what will be done with it - see it once.
     *
     * Assigning replaces the queue with plain navigations, discarding the operation and options of
     * anything already queued; assign [pendingNavigations] to preserve them.
     */
    var pending: List<T>
        get() = pendingNavigations.flatMap { it.destinations }.distinct()
        set(value) {
            pendingNavigations = value.asPendingNavigations()
        }
}

private fun <T : NavKey> List<PendingNavigation<T>>.withoutRepeatedDestinations(): List<PendingNavigation<T>> {
    val alreadyQueued = mutableSetOf<T>()
    return mapNotNull { navigation ->
        val deduplicated = if (navigation.isPlainNavigation) {
            navigation.removeDestinations { it in alreadyQueued }
        } else {
            navigation
        }
        alreadyQueued.addAll(navigation.destinations)
        deduplicated
    }
}

@Composable
fun rememberPendingBackStack(): PendingBackStack<NavKey> {
    return rememberSerializable(
        serializer = PendingBackStackSerializer(NavKeySerializer()),
        init = {
            PendingBackStack(NavBackStack())
        }
    )
}

@Composable
fun rememberPendingBackStack(
    vararg elements: NavKey
): PendingBackStack<NavKey> {
    return rememberSerializable(
        serializer = PendingBackStackSerializer(NavKeySerializer()),
        init = {
            PendingBackStack(NavBackStack(*elements))
        }
    )
}


class PendingBackStackSerializer<T : NavKey>(private val elementSerializer: KSerializer<T>) :
    KSerializer<PendingBackStack<T>> {

    private val delegate = NavBackStackSerializer(elementSerializer)
    private val pendingDelegate = ListSerializer(elementSerializer)
    private val pendingNavigationsDelegate =
        ListSerializer(PendingNavigation.serializer(elementSerializer))


    /**
     * Element 1 only ever holds destinations written before the queue carried their navigation
     * semantics. It is still read so that a back stack saved by an older build restores, but it is
     * no longer written; both it and element 2 are optional so that either shape decodes.
     *
     * The reverse direction is not supported: an older build decoding element 2 fails, because a
     * downgrade only ever happens on a manual reinstall, which discards the saved state anyway.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("mega.privacy.android.app.appstate.content.navigation.PendingBackStack") {
            element("base", delegate.descriptor)
            element("pending", pendingDelegate.descriptor, isOptional = true)
            element(
                "pendingNavigations",
                pendingNavigationsDelegate.descriptor,
                isOptional = true
            )
        }


    override fun serialize(encoder: Encoder, value: PendingBackStack<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, delegate, value.base)
            encodeSerializableElement(
                descriptor,
                2,
                pendingNavigationsDelegate,
                value.pendingNavigations
            )
        }
    }

    override fun deserialize(decoder: Decoder): PendingBackStack<T> {
        var base: NavBackStack<T>? = null
        var pending: List<T> = emptyList()
        var pendingNavigations: List<PendingNavigation<T>>? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> base = decodeSerializableElement(descriptor, 0, delegate)
                    1 -> pending = decodeSerializableElement(descriptor, 1, pendingDelegate)
                    2 -> pendingNavigations =
                        decodeSerializableElement(descriptor, 2, pendingNavigationsDelegate)

                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return PendingBackStack(base!!).also {
            it.pendingNavigations = pendingNavigations ?: pending.asPendingNavigations()
        }
    }
}


inline fun <reified T : NavKey> PendingBackStackSerializer(): PendingBackStackSerializer<T> {
    return PendingBackStackSerializer(elementSerializer = serializer<T>())
}
