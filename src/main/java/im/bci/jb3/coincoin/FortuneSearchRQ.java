package im.bci.jb3.coincoin;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
public class FortuneSearchRQ {

    private int year = DateTime.now().getYear();
    private String nicknameFilter;
    private String messageFilter;
    private String roomFilter;
    private int page = 0;
    private int pageSize = 200;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Interval getDateInterval() {
        try {
            DateTime start = new DateTime().withYear(year).withMonthOfYear(1).withDayOfMonth(1).withTimeAtStartOfDay();
            DateTime end = start.plusYears(1);
            return new Interval(start, end);
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
