package mega.privacy.android.app.lollipop.megachat;


public class NonContactInfo {

    String handle;
    String fullName;
//    boolean requestDone = false;

    public NonContactInfo(String handle, String fullName) {
        this.fullName = fullName;
        this.handle = handle;
//        this.requestDone = requestDone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
