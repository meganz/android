package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.chat.messages.LocationMessageView
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chat location message view
 */
@Composable
fun ChatLocationMessageView(
    message: LocationMessage,
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
) {
    message.geolocation?.let { geolocation ->
        val locationPreview = produceState<Bitmap?>(initialValue = null) {
            geolocation.image?.let { bitmapString ->
                viewModel.getBitmap(bitmapString)?.let { bitmap -> value = bitmap }
            }
        }

        LocationMessageView(
            isMe = message.isMine,
            title = stringResource(id = R.string.title_geolocation_message),
            geolocation = getGPSCoordinates(geolocation.latitude, geolocation.longitude),
            map = locationPreview.value?.asImageBitmap(),
            modifier = modifier
        )
    }
}

/**
 * Gets a geolocation string from latitude and longitude.
 */
private fun getGPSCoordinates(latitude: Float, longitude: Float): String {
    val builder = StringBuilder()

    formatCoordinate(builder, latitude)
    if (latitude < 0) {
        builder.append("S ")
    } else {
        builder.append("N ")
    }
    formatCoordinate(builder, longitude)
    if (longitude < 0) {
        builder.append("W")
    } else {
        builder.append("E")
    }
    return builder.toString()
}

private fun formatCoordinate(builder: StringBuilder, coordinate: Float) = with(builder) {
    val degrees = Location.convert(abs(coordinate).toDouble(), Location.FORMAT_SECONDS)
    val degreesSplit = degrees.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    append(degreesSplit[0])
    append("°")
    append(degreesSplit[1])
    append("'")
    runCatching { append(degreesSplit[2].replace(",", ".").toFloat().roundToInt()) }
        .onFailure {
            Timber.w(it, "Error rounding seconds in coordinates")
            append(degreesSplit[2])
        }
    append("''")
}