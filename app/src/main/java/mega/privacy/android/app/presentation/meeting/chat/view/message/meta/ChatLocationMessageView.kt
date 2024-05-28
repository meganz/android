package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.LocationMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.getMessageText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chat location message view
 */
@Composable
fun ChatLocationMessageView(
    message: LocationMessage,
    isEdited: Boolean,
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
) {
    message.chatGeolocationInfo?.let { geolocation ->
        val locationPreview = produceState<Bitmap?>(initialValue = null) {
            geolocation.image?.let { bitmapString ->
                viewModel.getBitmap(bitmapString)?.let { bitmap -> value = bitmap }
            }
        }

        ChatLocationMessageView(
            isMine = message.isMine,
            isEdited = isEdited,
            geolocation = getGPSCoordinates(geolocation.latitude, geolocation.longitude),
            locationPreview = locationPreview.value?.asImageBitmap(),
            modifier = modifier
        )
    }
}

@Composable
private fun ChatLocationMessageView(
    isMine: Boolean,
    isEdited: Boolean,
    geolocation: String,
    locationPreview: ImageBitmap?,
    modifier: Modifier,
) {
    LocationMessageView(
        isMe = isMine,
        title = stringResource(id = R.string.title_geolocation_message).getMessageText(isEdited = isEdited),
        geolocation = geolocation,
        map = locationPreview,
        modifier = modifier
    )
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

@CombinedThemePreviews
@Composable
private fun NotMyLocationMessagePreview() {
    Preview(isMine = false, isEdited = false)
}

@CombinedThemePreviews
@Composable
private fun MyLocationMessagePreview() {
    Preview(isMine = true, isEdited = false)
}

@CombinedThemePreviews
@Composable
private fun EditedLocationMessagePreview() {
    Preview(isMine = true, isEdited = true)
}

@Composable
private fun Preview(isMine: Boolean, isEdited: Boolean) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatLocationMessageView(
            isMine = isMine,
            isEdited = isEdited,
            geolocation = "41.1472° N, 8.6179° W",
            locationPreview = ImageBitmap.imageResource(id = R.drawable.avatar_qr_background),
            modifier = Modifier,
        )
    }
}