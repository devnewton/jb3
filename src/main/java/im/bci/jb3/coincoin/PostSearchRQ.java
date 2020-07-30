package im.bci.jb3.coincoin;

import org.apache.commons.lang3.StringUtils;
import java.time.ZonedDateTime;

import im.bci.jb3.bouchot.legacy.LegacyUtils;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
public class PostSearchRQ {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(LegacyUtils.legacyTimeZone);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);

    private String since, sinceTime;
    private String until, untilTime;
    private String nicknameFilter;
    private String messageFilter;
    private String roomFilter;
    private int page = 0;
    private int pageSize = 3600 * 2;

    public enum Sort {
        TIME_ASC,
        TIME_DESC,
        RELEVANCE
    }

    private Sort sort;

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }

    public String getSinceTime() {
        return sinceTime;
    }

    public void setSinceTime(String sinceTime) {
        this.sinceTime = sinceTime;
    }

    public String getUntilTime() {
        return untilTime;
    }

    public void setUntilTime(String untilTime) {
        this.untilTime = untilTime;
    }

    public ZonedDateTime getSinceDate() {
        try {
            if (StringUtils.isNotBlank(sinceTime)) {
                return ZonedDateTime.parse(since + "T" + sinceTime, DATETIME_FORMATTER);
            } else {
                return ZonedDateTime.parse(since, DATETIME_FORMATTER).truncatedTo(ChronoUnit.DAYS);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public ZonedDateTime getUntilDate() {
        try {
            if (StringUtils.isNotBlank(untilTime)) {
                return ZonedDateTime.parse(until + "T" + untilTime, DATETIME_FORMATTER);
            } else {
                return ZonedDateTime.parse(until, DATE_FORMATTER).plusDays(1).minusNanos(1);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getNicknameFilter() {
        return nicknameFilter;
    }

    public void setNicknameFilter(String nicknameFilter) {
        this.nicknameFilter = nicknameFilter;
    }

    public String getMessageFilter() {
        return messageFilter;
    }

    public void setMessageFilter(String messageFilter) {
        this.messageFilter = messageFilter;
    }

    public String getRoomFilter() {
        return roomFilter;
    }

    public void setRoomFilter(String roomFilter) {
        this.roomFilter = roomFilter;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
