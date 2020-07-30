package im.bci.jb3.bouchot.legacy;

import java.io.InputStreamReader;

import javax.annotation.PostConstruct;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.context.annotation.Scope;

@Component
@Scope("thread")
public class ToLegacyPEGNorlogeConverter {

    @Autowired
    private PostRepository postRepository;

    private Invocable invocable;
    private Object convertLegacyNorloge;

    @PostConstruct
    public void setup() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        InputStreamReader postToHtmlSource = new InputStreamReader(getClass().getResourceAsStream("/peg/to-legacy-norloge.js"));
        try {
            engine.eval(postToHtmlSource);
            convertLegacyNorloge = engine.eval("jb3_to_legacy_norloge");
            invocable = (Invocable) engine;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
    private static final DateTimeFormatter TO_ISO_NORLOGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);
    private static final DateTimeFormatter TO_NORMAL_NORLOGE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);

    public class NorlogeConverter {

        private String room;

        private ZonedDateTime postTime;

        public void setRoom(String room) {
            this.room = room;
        }

        public void setPostTime(ZonedDateTime postTime) {
            this.postTime = postTime;
        }

        public String convertIdNorloge(String id) {
            Post post = postRepository.findOne(id);
            if (null != post) {
                StringBuilder legacyNorloge = new StringBuilder();
                if (postTime.truncatedTo(ChronoUnit.DAYS).compareTo(post.getTime().truncatedTo(ChronoUnit.DAYS)) == 0) {
                    legacyNorloge.append(TO_NORMAL_NORLOGE_FORMATTER.format(post.getTime()));
                } else {
                    legacyNorloge.append(TO_ISO_NORLOGE_FORMATTER.format(post.getTime()));
                }
                addIndice(post, legacyNorloge);
                if (!StringUtils.equals(post.getRoom(), room)) {
                    legacyNorloge.append("@").append(post.getRoom());
                }
                return legacyNorloge.toString();
            }
            return null;
        }

        private void addIndice(Post post, StringBuilder legacyNorloge) {
            ZonedDateTime postTimeRounded = post.getTime().truncatedTo(ChronoUnit.SECONDS);
            List<Post> siblings = postRepository.findPostsReverse(postTimeRounded, postTimeRounded.plusSeconds(1), room);
            if (siblings.size() > 1) {
                int indice = 1;
                for (Post sibling : siblings) {
                    if (post.getId().equals(sibling.getId())) {
                        break;
                    }
                    ++indice;
                }
                switch (indice) {
                    case 1:
                        legacyNorloge.append('¹');
                        break;
                    case 2:
                        legacyNorloge.append('²');
                        break;
                    case 3:
                        legacyNorloge.append('³');
                        break;
                    case 4:
                        legacyNorloge.append('⁴');
                        break;
                    case 5:
                        legacyNorloge.append('⁵');
                        break;
                    case 6:
                        legacyNorloge.append('⁶');
                        break;
                    case 7:
                        legacyNorloge.append('⁷');
                        break;
                    case 8:
                        legacyNorloge.append('⁸');
                        break;
                    case 9:
                        legacyNorloge.append('⁹');
                        break;
                    default:
                        legacyNorloge.append("^").append(indice);
                }
            }
        }
    }

    public static class ParseOptions {

        private NorlogeConverter norlogeConverter;

        public NorlogeConverter getNorlogeConverter() {
            return norlogeConverter;
        }

        public void setNorlogeConverter(NorlogeConverter norlogeConverter) {
            this.norlogeConverter = norlogeConverter;
        }
    }

    public String convertToLegacyNorloges(String message, ZonedDateTime postTime, String room) {
        try {
            ParseOptions options = new ParseOptions();
            NorlogeConverter converter = new NorlogeConverter();
            converter.setPostTime(postTime);
            converter.setRoom(room);
            options.setNorlogeConverter(converter);
            return invocable.invokeMethod(convertLegacyNorloge, "parse", message, options).toString();
        } catch (Exception ex) {
            return message;
        }
    }

}
