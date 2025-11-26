package mega.privacy.android.app.appstate.content.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

@Serializable(with = TopLevelBackStackSerializer::class)
class TopLevelBackStack<T : NavKey, U : T>(val startKey: U) {

    internal var topLevelBackStacks: HashMap<U, NavBackStack<T>> = hashMapOf(
        startKey to NavBackStack(startKey)
    )

    var topLevelKey by mutableStateOf(startKey)
        internal set

    val backStack = NavBackStack<T>(startKey)

    internal fun updateBackStack() {
        backStack.clear()
        val currentStack = topLevelBackStacks[topLevelKey] ?: emptyList()

        if (topLevelKey == startKey) {
            backStack.addAll(currentStack)
        } else {
            val startStack = topLevelBackStacks[startKey] ?: emptyList()
            backStack.addAll(startStack + currentStack)
        }
    }

    fun switchTopLevel(key: U) {
        switchOrCreateStack(key)
        updateBackStack()
    }

    private fun switchOrCreateStack(key: U) {
        if (topLevelBackStacks[key] == null) {
            topLevelBackStacks[key] = NavBackStack(key)
        }
        topLevelKey = key
    }

    fun add(key: T) {
        topLevelBackStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun switchAndAdd(topLevelKey: U, vararg keys: T) {
        switchOrCreateStack(topLevelKey)
        topLevelBackStacks[topLevelKey]?.addAll(keys)
        updateBackStack()
    }

    fun removeLast() {
        val currentStack = topLevelBackStacks[topLevelKey] ?: return

        if (currentStack.size > 1) {
            currentStack.removeLastOrNull()
        } else if (topLevelKey != startKey) {
            topLevelKey = startKey
        }
        updateBackStack()
    }

    fun replaceStack(vararg keys: T) {
        topLevelBackStacks[topLevelKey] = NavBackStack(topLevelKey, *keys)
        updateBackStack()
    }

    fun addAll(destinations: List<T>) {
        topLevelBackStacks[topLevelKey]?.addAll(destinations)
        updateBackStack()
    }
}

@Composable
fun rememberTopLevelBackStack(startKey: MainNavItemNavKey): TopLevelBackStack<NavKey, MainNavItemNavKey> {
    return rememberSerializable(
        serializer = TopLevelBackStackSerializer(NavKeySerializer(), NavKeySerializer()),
        init = {
            TopLevelBackStack(startKey)
        }
    )
}


class TopLevelBackStackSerializer<T : NavKey, U : T>(
    private val elementSerializer: KSerializer<T>,
    private val keyElementSerializer: KSerializer<U>,
) : KSerializer<TopLevelBackStack<T, U>> {

    private val navBackStackSerializer = NavBackStackSerializer(elementSerializer)
    private val mapDelegate = MapSerializer(keyElementSerializer, navBackStackSerializer)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("mega.privacy.android.app.appstate.content.navigation.TopLevelBackStack") {
            element("startKey", elementSerializer.descriptor)
            element("topLevelKey", elementSerializer.descriptor)
            element("topLevelBackStacks", mapDelegate.descriptor)
        }

    override fun serialize(encoder: Encoder, value: TopLevelBackStack<T, U>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, elementSerializer, value.startKey)
            encodeSerializableElement(descriptor, 1, elementSerializer, value.topLevelKey)
            encodeSerializableElement(
                descriptor,
                2,
                mapDelegate,
                value.topLevelBackStacks
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): TopLevelBackStack<T, U> {
        var startKey: T? = null
        var topLevelKey: U? = null
        var topLevelBackStacks: Map<U, NavBackStack<T>> = emptyMap()

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> startKey = decodeSerializableElement(descriptor, 0, keyElementSerializer)
                    1 -> topLevelKey =
                        decodeSerializableElement(descriptor, 1, keyElementSerializer)

                    2 -> topLevelBackStacks = decodeSerializableElement(descriptor, 2, mapDelegate)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return TopLevelBackStack(startKey!!).also {
            it.topLevelBackStacks = HashMap(topLevelBackStacks)
            it.topLevelKey = topLevelKey!!
            it.updateBackStack()
        } as TopLevelBackStack<T, U>
    }
}

inline fun <reified T : NavKey, reified U : T> TopLevelBackStackSerializer(): TopLevelBackStackSerializer<T, U> {
    return TopLevelBackStackSerializer(
        elementSerializer = serializer<T>(),
        keyElementSerializer = serializer<U>()
    )
}