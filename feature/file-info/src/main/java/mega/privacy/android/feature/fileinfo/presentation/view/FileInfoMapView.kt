package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.fileinfo.presentation.model.Coordinates

/**
 * A non-interactive map thumbnail with a place caption for geo-tagged media.
 *
 * @param coordinates the media GPS coordinates
 * @param caption the reverse-geocoded place name, or null when unresolved
 * @param modifier modifier for the section
 */
@Composable
internal fun FileInfoMapView(
    coordinates: Coordinates,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    val location = LatLng(coordinates.latitude, coordinates.longitude)
    val markerState = rememberMarkerState(position = location)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, MAP_ZOOM)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaText(
            // TODO extract to a localized string resource
            text = "Map",
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleSmall,
        )
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag(FILE_INFO_MAP_TAG),
            uiSettings = NON_INTERACTIVE_MAP_SETTINGS,
            cameraPositionState = cameraPositionState,
        ) {
            Marker(state = markerState)
        }
        caption?.let {
            MegaText(
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(FILE_INFO_MAP_ADDRESS_TAG),
                text = it,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodySmall,
            )
        }
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
