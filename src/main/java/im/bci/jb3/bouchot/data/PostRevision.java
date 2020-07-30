package im.bci.jb3.bouchot.data;

import java.time.ZonedDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import im.bci.jb3.bouchot.logic.CleanUtils;

public class PostRevision {

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private ZonedDateTime time;
    @JsonIgnore
    private String message;

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    @JsonSetter("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonGetter("message")
    public String getCleanedMessage() {
        return CleanUtils.cleanMessage(message);
    }
}
