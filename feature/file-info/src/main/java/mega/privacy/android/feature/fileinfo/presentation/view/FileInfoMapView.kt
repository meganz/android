package mega.privacy.android.feature.fileinfo.presentation.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.fileinfo.presentation.model.Coordinates
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/**
 * The location "Map" section for media nodes. When [coordinates] are present it shows a
 * non-interactive map thumbnail with a place caption (tapping opens the location in an external maps
 * app); otherwise it shows a "no location information" placeholder.
 *
 * @param coordinates the media GPS coordinates, or null when the node has no valid location
 * @param caption the reverse-geocoded place name, or null when unresolved
 * @param modifier modifier for the section
 */
@Composable
internal fun FileInfoMapView(
    coordinates: Coordinates?,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaText(
            text = stringResource(sharedR.string.file_info_information_map_label),
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
        )
        if (coordinates != null) {
            MapThumbnail(coordinates = coordinates, caption = caption)
        } else {
            NoLocationRow()
        }
    }
}

@Composable
private fun ColumnScope.MapThumbnail(coordinates: Coordinates, caption: String?) {
    val context = LocalContext.current
    val location = LatLng(coordinates.latitude, coordinates.longitude)
    val markerState = rememberMarkerState(position = location)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, MAP_ZOOM)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        GoogleMap(
            modifier = Modifier
                .matchParentSize()
                .testTag(FILE_INFO_MAP_TAG),
            uiSettings = NON_INTERACTIVE_MAP_SETTINGS,
            cameraPositionState = cameraPositionState,
        ) {
            Marker(state = markerState)
        }
        // Transparent overlay so a tap opens the location externally instead of panning the map.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { context.openLocationInMaps(coordinates, caption) },
        )
    }
    caption?.let {
        MegaText(
            modifier = Modifier
                .align(Alignment.End)
                .clickable { context.openLocationInMaps(coordinates, caption) }
                .testTag(FILE_INFO_MAP_ADDRESS_TAG),
            text = it,
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NoLocationRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(FILE_INFO_NO_LOCATION_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            modifier = Modifier.size(24.dp),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
            tint = IconColor.Secondary,
            contentDescription = null,
        )
        MegaText(
            text = stringResource(sharedR.string.file_info_information_no_location),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyLarge,
        )
    }
}

/**
 * Opens the coordinate in an external maps app via an implicit `geo:` intent.
 */
private fun Context.openLocationInMaps(coordinates: Coordinates, label: String?) {
    val point = "${coordinates.latitude},${coordinates.longitude}"
    val query = if (label.isNullOrBlank()) point else "$point(${Uri.encode(label)})"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$point?q=$query"))
    runCatching { startActivity(intent) }
        .onFailure { Timber.e(it, "No app available to open the map location") }
}

@CombinedThemePreviews
@Composable
private fun FileInfoMapViewNoLocationPreview() {
    AndroidThemeForPreviews {
        FileInfoMapView(coordinates = null, caption = null)
    }
}

private const val MAP_ZOOM = 10f

private val NON_INTERACTIVE_MAP_SETTINGS = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    scrollGesturesEnabled = false,
    scrollGesturesEnabledDuringRotateOrZoom = false,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = false,
)

internal const val FILE_INFO_MAP_TAG = "file_info_screen:map"
internal const val FILE_INFO_MAP_ADDRESS_TAG = "file_info_screen:map_address"
internal const val FILE_INFO_NO_LOCATION_TAG = "file_info_screen:no_location"
