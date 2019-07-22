package mega.privacy.android.app.lollipop.megachat;

import com.google.android.gms.maps.model.LatLng;

public class MapAddress {

    private LatLng latLng;
    private String name;
    private String address;

    public MapAddress(LatLng latLng, String name, String address) {
        this.latLng = latLng;
        this.name = name;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getLatLng() {
        return latLng;
    }
}
