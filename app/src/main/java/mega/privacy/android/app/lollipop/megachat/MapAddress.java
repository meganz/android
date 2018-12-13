package mega.privacy.android.app.lollipop.megachat;

import com.google.android.gms.location.places.Place;

public class MapAddress {

    private Place place;
    private String name;
    private String address;

    public MapAddress (String name, String address) {
        this.name = name;
        this.address = address;
    }

    public MapAddress (Place place) {
        this.place = place;
        this.name = place.getName().toString();
        this.address = place.getAddress().toString();
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public Place getPlace() {
        return place;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
