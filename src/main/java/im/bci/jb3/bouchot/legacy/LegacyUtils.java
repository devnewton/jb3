package im.bci.jb3.bouchot.legacy;

import im.bci.jb3.bouchot.data.Post;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
@Component
public class LegacyUtils {

    @Autowired
    private FromLegacyPEGNorlogeConverter fromLegacyPEGNorlogeConverter;

    @Autowired
    private ToLegacyPEGNorlogeConverter toLegacyPEGNorlogeConverter;

    public static final String legacyTimezoneId = "Europe/Paris";
    public static final ZoneId legacyTimeZone = ZoneId.of(legacyTimezoneId);
    public static final DateTimeFormatter legacyPostTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(legacyTimeZone);

    public String convertFromLegacyNorloges(String message, ZonedDateTime postTime, String room) {
        return fromLegacyPEGNorlogeConverter.convertFromLegacyNorloge(message, postTime, room);
    }

    public String convertToLegacyNorloges(String message, ZonedDateTime postTime, String room) {
        return toLegacyPEGNorlogeConverter.convertToLegacyNorloges(message, postTime, room);
    }

    LegacyPost post2legacy(Post post, Escaper escaper) {
        LegacyPost legacyPost = new LegacyPost();
        legacyPost.setId(post.getTime().toInstant().toEpochMilli());
        legacyPost.setTime(legacyPostTimeFormatter.format(post.getTime()));
        String info = Jsoup.clean(post.getNickname(), Whitelist.none());
        String message = Jsoup.clean(convertToLegacyNorloges(convertUrls(post.getCleanedMessage()), post.getTime(), post.getRoom()), messageWhitelist);
        legacyPost.setInfo(escaper.escape(info));
        legacyPost.setMessage(escaper.escape(message));
        return legacyPost;

    }

    LegacyPost post2legacy(Post post) {
        return post2legacy(post, csvEscaper);
    }

    static interface Escaper {

        String escape(String s);
    }

    static final Escaper xmlEscaper = new Escaper() {

        @Override
        public String escape(String s) {
            return StringEscapeUtils.escapeXml10(s);
        }
    };
    static final Escaper csvEscaper = new Escaper() {

        @Override
        public String escape(String s) {
            return s.replaceAll("\\p{C}", " ");

        }
    };
        static final Escaper bsvEscaper = new Escaper() {

        @Override
        public String escape(String s) {
            return s.replaceAll("[\u001E\u001F]", "");

        }
    };


    private static final Pattern urlPattern = Pattern.compile("(((https?|ftp|gopher)://)|(data:))[^\\s]+");

    private final Whitelist messageWhitelist = Whitelist.none().addTags("b", "i", "s", "u", "tt", "a", "code", "spoiler").addAttributes("a", "href", "rel", "target");

    private String convertUrls(String message) {
        Matcher matcher = urlPattern.matcher(message);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<a href=\"$0\" target=\"_blank\">[url]</a>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
