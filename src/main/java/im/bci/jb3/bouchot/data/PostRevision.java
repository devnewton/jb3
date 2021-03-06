package im.bci.jb3.bouchot.data;

import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import im.bci.jb3.bouchot.logic.CleanUtils;

public class PostRevision {
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private DateTime time;
	@JsonIgnore
	private String message;

	public DateTime getTime() {
		return time;
	}

	public void setTime(DateTime time) {
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
