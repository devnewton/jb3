package im.bci.jb3.bouchot.data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import im.bci.jb3.bouchot.logic.CleanUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Document(language = "french")
public class Post {

    @Id
    private String id;

    @Indexed
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private ZonedDateTime time;
    @Indexed
    @JsonIgnore
    private String nickname;
    @TextIndexed
    @JsonIgnore
    private String message;
    private List<PostRevision> revisions;
    @Indexed
    @JsonIgnore
    private String room;

    @Indexed
    private GatewayPostId gatewayPostId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }

    @JsonGetter("nickname")
    public String getCleanedNickname() {
        return CleanUtils.cleanNickname(nickname);
    }

    public String getNickname() {
        return nickname;
    }

    @JsonSetter("nickname")
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @JsonGetter("message")
    public String getCleanedMessage() {
        return CleanUtils.cleanMessage(message);
    }

    public String getMessage() {
        return message;
    }

    @JsonSetter("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonGetter("room")
    public String getCleanedRoom() {
        return CleanUtils.cleanRoom(room);
    }

    public String getRoom() {
        return room;
    }

    @JsonSetter("room")
    public void setRoom(String room) {
        this.room = room;
    }

    public void setGatewayPostId(GatewayPostId gatewayPostId) {
        this.gatewayPostId = gatewayPostId;
    }

    public GatewayPostId getGatewayPostId() {
        return gatewayPostId;
    }

    public List<PostRevision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<PostRevision> revisions) {
        this.revisions = revisions;
    }

    public void revise(String newMessage) {
        if (null == revisions) {
            revisions = new ArrayList<PostRevision>();
        }
        if (revisions.isEmpty()) {
            PostRevision revision = new PostRevision();
            revision.setMessage(message);
            revision.setTime(time);
            revisions.add(revision);
        }
        PostRevision revision = new PostRevision();
        revision.setMessage(newMessage);
        revision.setTime(ZonedDateTime.now(ZoneId.of("UTC")));
        revisions.add(revision);
        this.message = newMessage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Post other = (Post) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
