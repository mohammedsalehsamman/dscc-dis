package employee.model;

/**
 * One line of the server activity log (who did what and when).
 * Clients poll getActivitySince(lastId) to get live updates.
 */
public class ActivityEntry {

    private long id;
    private String time;
    private String username;
    private String action;
    private String details;

    public ActivityEntry() {
    }

    public ActivityEntry(long id, String time, String username, String action, String details) {
        this.id = id;
        this.time = time;
        this.username = username;
        this.action = action;
        this.details = details;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
