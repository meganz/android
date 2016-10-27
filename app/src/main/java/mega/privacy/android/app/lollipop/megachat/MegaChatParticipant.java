package mega.privacy.android.app.lollipop.megachat;

public class MegaChatParticipant {

    String firstName;
    String lastName;
    String email;
    long handle;
    int privilege;

    public MegaChatParticipant(long handle, String firstName, String lastName, String email, int privilege) {
        this.firstName = firstName;
        this.handle = handle;
        this.email = email;
        this.lastName = lastName;
        this.privilege = privilege;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
