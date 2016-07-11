package im.bci.jb3.websocket.messages;

import java.util.List;

import im.bci.jb3.data.Post;
import im.bci.jb3.websocket.messages.s2c.PresenceS2C;

public class MessageS2C {

    private List<Post> posts;
    private PresenceS2C presence;
    private String ack;

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public PresenceS2C getPresence() {
        return presence;
    }

    public void setPresence(PresenceS2C presence) {
        this.presence = presence;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

}
