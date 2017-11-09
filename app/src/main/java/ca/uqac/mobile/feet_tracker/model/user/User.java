package ca.uqac.mobile.feet_tracker.model.user;

public class User {

    private String username;
    private String id;

    public User(){
        //empty constructor required by Firebase
    }

    public User(String username, String id){
        this.username = username;
        this.id = id;

    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

}
