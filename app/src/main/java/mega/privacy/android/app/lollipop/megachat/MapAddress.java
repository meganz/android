package mega.privacy.android.app.lollipop.megachat;

import mega.privacy.android.app.middlelayer.map.MegaLatLng;

public class MapAddress {

    private MegaLatLng latLng;
    private String name;
    private String address;

    public MapAddress(MegaLatLng latLng, String name, String address) {
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

    public MegaLatLng getLatLng() {
        return latLng;
    }
}
