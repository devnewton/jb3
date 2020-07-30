package im.bci.jb3.bouchot.logic;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.legacy.LegacyUtils;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class Norloge {

    private String id;
    private TemporalAccessor time;
    private String bouchot;

    public Norloge(Post post) {
        this.id = post.getId();
    }

    public Norloge() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Norloge withId(String i) {
        setId(i);
        return this;
    }

    public TemporalAccessor getTime() {
        return time;
    }

    ZonedDateTime toZonedDateTime() {
        if(null == time) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now(LegacyUtils.legacyTimeZone);
        int year = time.isSupported(ChronoField.YEAR) ? time.get(ChronoField.YEAR) : now.getYear();
        int month = time.isSupported(ChronoField.MONTH_OF_YEAR) ? time.get(ChronoField.MONTH_OF_YEAR) : now.getMonthValue();
        int day = time.isSupported(ChronoField.DAY_OF_MONTH) ? time.get(ChronoField.DAY_OF_MONTH) : now.getDayOfMonth();
        int hour = time.get(ChronoField.HOUR_OF_DAY);
        int minutes = time.get(ChronoField.MINUTE_OF_HOUR);
        int seconds = time.isSupported(ChronoField.SECOND_OF_MINUTE) ? time.get(ChronoField.SECOND_OF_MINUTE) : 0;
        return ZonedDateTime.of(year, month, day, hour, minutes, seconds, 0, LegacyUtils.legacyTimeZone);
    }

    public void setTime(TemporalAccessor time) {
        this.time = time;
    }

    public Norloge withTime(TemporalAccessor t) {
        setTime(t);
        return this;
    }

    public String getBouchot() {
        return bouchot;
    }

    public void setBouchot(String bouchot) {
        this.bouchot = bouchot;
    }

    public Norloge withBouchot(String b) {
        setBouchot(b);
        return this;
    }

    public boolean getHasYear() {
        return time.isSupported(ChronoField.YEAR);
    }

    public boolean getHasMonth() {
        return time.isSupported(ChronoField.MONTH_OF_YEAR);
    }

    public boolean getHasDay() {
        return time.isSupported(ChronoField.DAY_OF_MONTH);
    }

    public boolean getHasSeconds() {
        return time.isSupported(ChronoField.SECOND_OF_MINUTE);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.id != null ? this.id.hashCode() : 0);
        ZonedDateTime dateTime = toZonedDateTime();
        hash = 67 * hash + (dateTime != null ? dateTime.hashCode() : 0);
        hash = 67 * hash + (this.bouchot != null ? this.bouchot.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Norloge other = (Norloge) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        
        if((this.bouchot == null) ? (other.bouchot != null) : !this.bouchot.equals(other.bouchot)) {
            return false;
        }
        
        ZonedDateTime thisDateTime = toZonedDateTime();
        ZonedDateTime otherDateTime = other.toZonedDateTime();
        return thisDateTime == null ? otherDateTime == null : thisDateTime.equals(otherDateTime);
        
    }
    
    

    public static final DateTimeFormatter norlogePrintFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter norlogeParseFullIsoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter norlogeParseFullFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd'#'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter norlogeParseLongFormatter = DateTimeFormatter.ofPattern("MM/dd'#'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter norlogeParseNormalFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter norlogeParseShortFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(LegacyUtils.legacyTimeZone);
    private static final Pattern norlogesPattern = Pattern.compile("((#(?<id>\\w+))|(?<time>(?<date>((?<year>\\d\\d\\d\\d)[/-])?(?:1[0-2]|0[1-9])[/-](?:3[0-1]|[1-2][0-9]|0[1-9])[#T])?((?:2[0-3]|[0-1][0-9])):([0-5][0-9])(:(?<seconds>[0-5][0-9]))?)(?<exp>[¹²³]|[:\\^][1-9]|[:\\^][1-9][0-9])?)(@(?<bouchot>[\\w.]+))?");

    public static class ParsedNorloges extends ArrayList<Norloge> {

        private static final long serialVersionUID = 1L;

        private String remainingMessageContent;

        public String getRemainingMessageContent() {
            return remainingMessageContent;
        }

        public void setRemainingMessageContent(String remainingMessageContent) {
            this.remainingMessageContent = remainingMessageContent;
        }
    }

    public static ParsedNorloges parseNorloges(String message) {
        final ParsedNorloges result = new ParsedNorloges();
        final StringBuffer sb = new StringBuffer();

        forEachNorloge(message, new NorlogeProcessor() {

            @Override
            public void process(Norloge norloge, Matcher matcher) {
                result.add(norloge);
                matcher.appendReplacement(sb, "");
            }

            @Override
            public void end(Matcher matcher) {
                matcher.appendTail(sb);
                result.setRemainingMessageContent(org.springframework.util.StringUtils.trimLeadingWhitespace(sb.toString()));
            }
        });
        return result;
    }

    public int getPrecisionInSeconds() {
        return getHasSeconds() ? 1 : 60;
    }

    public interface NorlogeProcessor {

        void process(Norloge norloge, Matcher matcher);

        void end(Matcher matcher);
    }

    public static void forEachNorloge(String message, NorlogeProcessor processor) {
        Matcher matcher = norlogesPattern.matcher(message);
        while (matcher.find()) {
            String id = matcher.group("id");
            String bouchot = matcher.group("bouchot");
            Norloge norloge = null;
            if (null != id) {
                norloge = new Norloge().withId(id).withBouchot(bouchot);
            } else {
                final String time = matcher.group("time");
                if (null != time) {
                    norloge = parseNorlogeTime(time);
                    if (null != norloge) {
                        norloge.setBouchot(bouchot);
                    }
                }
            }
            if (null != norloge) {
                processor.process(norloge, matcher);
            }
        }
        processor.end(matcher);
    }

    private static Norloge parseNorlogeTime(String item) {
        TemporalAccessor norlogeTime = parseNorlogeTimeWithFormat(item, norlogeParseFullIsoFormatter);
        if (null != norlogeTime) {
            return new Norloge().withTime(norlogeTime);
        }
        norlogeTime = parseNorlogeTimeWithFormat(item, norlogeParseFullFormatter);
        if (null != norlogeTime) {
            return new Norloge().withTime(norlogeTime);
        }
        norlogeTime = parseNorlogeTimeWithFormat(item, norlogeParseLongFormatter);
        if (null != norlogeTime) {
            return new Norloge().withTime(norlogeTime);
        }
        norlogeTime = parseNorlogeTimeWithFormat(item, norlogeParseNormalFormatter);
        if (null != norlogeTime) {
            return new Norloge().withTime(norlogeTime);
        }
        norlogeTime = parseNorlogeTimeWithFormat(item, norlogeParseShortFormatter);
        if (null != norlogeTime) {
            return new Norloge().withTime(norlogeTime);
        }
        return null;
    }

    private static TemporalAccessor parseNorlogeTimeWithFormat(String item, DateTimeFormatter format) {
        try {
            return format.parse(item);
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(Post post) {
        return '#' + post.getId();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(id)) {
            sb.append('#');
            sb.append(id);
        }
        if (null != time) {
            sb.append(norlogePrintFormatter.format(toZonedDateTime()));
        }
        if (StringUtils.isNotBlank(bouchot)) {
            sb.append('@').append(bouchot);
        }
        return sb.toString();
    }
}
