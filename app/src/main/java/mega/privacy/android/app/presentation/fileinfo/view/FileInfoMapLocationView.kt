package mega.privacy.android.app.presentation.fileinfo.view

import android.content.Context
import android.location.Address
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R.drawable
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary

@Composable
internal fun FileInfoMapLocationView(
    modifier: Modifier = Modifier,
    getAddress: suspend (Context, Double, Double) -> Address?,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
) {
    var address by remember { mutableStateOf<Address?>(null) }
    val context = LocalContext.current

    LaunchedEffect(latitude, longitude) {
        address = getAddress(context, latitude, longitude)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBox()

        address?.let { address ->
            LocationMap(latitude, longitude, address)
        } ?: NoLocationInfo()
    }
}

@Composable
private fun TitleBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(id = R.string.file_properties_info_map_location),
            style = typography.subtitle2medium.copy(color = MaterialTheme.colors.textColorPrimary),
            modifier = Modifier.testTag(TEST_TAG_MAP_LOCATION_TITLE)
        )
    }
}

@Composable
private fun LocationMap(latitude: Double, longitude: Double, address: Address) {
    val location = LatLng(latitude, longitude)
    val markerState = rememberMarkerState(position = location)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 10f)
    }

    val featureName = address.featureName?.takeIf { it.isNotBlank() }
    val thoroughfare = address.thoroughfare?.takeIf { it.isNotBlank() }
    val subLocality = address.subLocality?.takeIf { it.isNotBlank() }
    val locality = address.locality?.takeIf { it.isNotBlank() }
    val adminArea = address.adminArea?.takeIf { it.isNotBlank() }
    val countryName = address.countryName?.takeIf { it.isNotBlank() }

    val titleText = listOfNotNull(
        featureName,
        thoroughfare
    ).joinToString(", ")

    val snippetText = listOfNotNull(
        subLocality,
        locality
    ).joinToString(", ")

    val addressText = listOfNotNull(
        subLocality,
        locality,
        adminArea,
        countryName
    ).joinToString(", ")

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .clip(RoundedCornerShape(8.dp))
            .testTag(TEST_TAG_MAP_LOCATION_MAP),
        uiSettings = MapUiSettings(
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
        ),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = markerState,
            title = titleText,
            snippet = snippetText,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {

        Text(
            text = addressText,
            style = typography.caption.copy(color = MaterialTheme.colors.textColorPrimary),
            modifier = Modifier.testTag(TEST_TAG_MAP_LOCATION_ADDRESS)
        )
    }
}

@Composable
private fun NoLocationInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            painter = painterResource(id = drawable.ic_info_medium_regular_outline),
            contentDescription = null,
            tint = MaterialTheme.colors.black_white,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .testTag(TEST_TAG_MAP_LOCATION_NO_LOCATION_INFO_ICON),
        )

        Text(
            text = stringResource(id = R.string.file_properties_info_map_location_no_location_info),
            style = typography.subtitle1medium.copy(color = MaterialTheme.colors.textColorPrimary),
            modifier = Modifier.testTag(TEST_TAG_MAP_LOCATION_NO_LOCATION_INFO_TEXT)
        )
    }
}
