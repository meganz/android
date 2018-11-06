package mega.privacy.android.app.lollipop.megachat;

public class MegaChatParticipant {

    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private long handle;
    private int privilege;
    private boolean isContact;

    private String lastGreen;

    public MegaChatParticipant(long handle, String firstName, String lastName, String fullName, String email, int privilege) {
        this.fullName = fullName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.handle = handle;
        this.email = email;
        this.privilege = privilege;
        lastGreen = "";
    }

    public MegaChatParticipant(long handle) {
        this.handle = handle;
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

    public boolean isContact() {
        return isContact;
    }

    public void setContact(boolean contact) {
        isContact = contact;
    }

    public String getLastGreen() {
        return lastGreen;
    }

    public void setLastGreen(String lastGreen) {
        this.lastGreen = lastGreen;
    }

}
