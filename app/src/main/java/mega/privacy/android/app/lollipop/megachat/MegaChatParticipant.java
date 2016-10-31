package mega.privacy.android.app.lollipop.megachat;

public class MegaChatParticipant {

    private String fullName;
    private String email;
    private long handle;
    private int privilege;

    public MegaChatParticipant(long handle, String fullName, String email, int privilege) {
        this.fullName = fullName;
        this.handle = handle;
        this.email = email;
        this.privilege = privilege;
    }

    public long getHandle() {
        return handle;
    }

    public void setHandle(long handle) {
        this.handle = handle;
    }

    public int getPrivilege() {
        return privilege;
    }

    public void setPrivilege(int privilege) {
        this.privilege = privilege;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
