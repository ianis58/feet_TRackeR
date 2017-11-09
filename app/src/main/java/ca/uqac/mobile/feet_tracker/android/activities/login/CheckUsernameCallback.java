package ca.uqac.mobile.feet_tracker.android.activities.login;

interface CheckUsernameCallback {
    void isValid(String username);
    void isTaken();

}
