package im.bci.jb3.bouchot.data;

import java.util.List;
import java.time.ZonedDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import im.bci.jb3.bouchot.logic.CleanUtils;
import java.time.ZoneId;

public class Fortune {

    @Id
    private String id;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime time = ZonedDateTime.now(ZoneId.of("UTC"));
    
    private String fortuner;
    private String title;

    private List<Post> posts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }
    
    public String getCleanedTitle() {
        return CleanUtils.cleanFortuneTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFortuner() {
        return fortuner;
    }

    public void setFortuner(String fortuner) {
        this.fortuner = fortuner;
    }

}
