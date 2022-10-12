package mega.privacy.android.data.model.chat;


public class NonContactInfo {

    String handle;
    String fullName;
    String firstName;
    String lastName;
    String email;
//    boolean requestDone = false;

    public NonContactInfo(String handle, String fullName, String firstName, String lastName, String email) {
        this.fullName = fullName;
        this.handle = handle;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
//        this.requestDone = requestDone;
    }

    public String getFullName() {

        if(firstName==null){
            firstName="";
        }
        if(lastName == null){
            lastName="";
        }

        if (firstName.trim().length() <= 0){
            fullName = lastName;
        }
        else{
            fullName = firstName + " " + lastName;
        }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
