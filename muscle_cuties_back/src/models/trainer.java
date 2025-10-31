
package models;

import java.util.ArrayList;
import java.util.List;

public class trainer {
    private String username;
    private String password;
    private String fullName;
    private int yearsExperience;
    private String credentials;
    private List<user> users = new ArrayList<>();

    public trainer(String username, String password, String fullName, int yearsExperience, String credentials) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.yearsExperience = yearsExperience;
        this.credentials = credentials;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public int getYearsExperience() { return yearsExperience; }
    public String getCredentials() { return credentials; }
    public List<user> getUsers() { return users; }
    public void addUser(user u) { if (u != null) users.add(u); }
}
