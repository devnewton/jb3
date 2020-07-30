package im.bci.jb3.dlfp;

import java.time.Instant;

/**
 *
 * @author devnewton
 */
public class OauthToken {

    private String access_token;
    private String refresh_token;
    private long expires_timestamp;

    public OauthToken() {
    }

    public OauthToken(DlfpOauthToken token) {
        this.access_token = token.getAccess_token();
        this.refresh_token = token.getRefresh_token();
        this.expires_timestamp = Instant.now().plusSeconds(token.getExpires_in()).toEpochMilli();
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public long getExpires_timestamp() {
        return expires_timestamp;
    }

    public void setExpires_timestamp(long expires_timestamp) {
        this.expires_timestamp = expires_timestamp;
    }

}
