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
    var pending: List<T> = emptyList()
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


    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("mega.privacy.android.app.appstate.content.navigation.PendingBackStack") {
            element("base", delegate.descriptor)
            element("pending", pendingDelegate.descriptor)
        }


    override fun serialize(encoder: Encoder, value: PendingBackStack<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, delegate, value.base)
            encodeSerializableElement(descriptor, 1, pendingDelegate, value.pending)
        }
    }

    override fun deserialize(decoder: Decoder): PendingBackStack<T> {
        var base: NavBackStack<T>? = null
        var pending: List<T> = emptyList()

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> base = decodeSerializableElement(descriptor, 0, delegate)
                    1 -> pending = decodeSerializableElement(descriptor, 1, pendingDelegate)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return PendingBackStack(base!!).also { it.pending = pending }
    }
}


inline fun <reified T : NavKey> PendingBackStackSerializer(): PendingBackStackSerializer<T> {
    return PendingBackStackSerializer(elementSerializer = serializer<T>())
}