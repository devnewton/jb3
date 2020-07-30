package im.bci.jb3.bouchot.legacy;

import im.bci.jb3.bouchot.data.GatewayPostId;
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
import im.bci.jb3.bouchot.logic.Norloge;
import java.time.ZonedDateTime;
import org.springframework.context.annotation.Scope;

@Component
@Scope("thread")
public class FromLegacyPEGNorlogeConverter {

    @Autowired
    private PostRepository postRepository;

    private Invocable invocable;
    private Object convertLegacyNorloge;

    @PostConstruct
    public void setup() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        InputStreamReader postToHtmlSource = new InputStreamReader(getClass().getResourceAsStream("/peg/from-legacy-norloge.js"));
        try {
            engine.eval(postToHtmlSource);
            convertLegacyNorloge = engine.eval("jb3_from_legacy_norloge");
            invocable = (Invocable) engine;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public class NorlogeConverter {

        private String room;

        private ZonedDateTime postTime;

        public void setRoom(String room) {
            this.room = room;
        }

        public void setPostTime(ZonedDateTime postTime) {
            this.postTime = postTime;
        }

        public String convertFullNorloge(int y, int m, int d, int h, int mi, int s, int indice, String bouchot) {
            ZonedDateTime norlogeTime = ZonedDateTime.of(y, m, d, h, mi, s, 0, LegacyUtils.legacyTimeZone);
            Norloge norloge = new Norloge().withTime(norlogeTime).withBouchot(bouchot);
            Post post = postRepository.findOne(null != norloge.getBouchot() ? norloge.getBouchot() : room, norlogeTime, norlogeTime.plusSeconds(norloge.getPrecisionInSeconds()), indice);
            if (null != post) {
                return Norloge.format(post);
            } else {
                return null;
            }
        }

        private static final int MAX_YEAR_BEFORE = 2;

        public String convertLongNorloge(int m, int d, int h, int mi, int s, int indice, String bouchot) {
            ZonedDateTime norlogeTime = ZonedDateTime.of(postTime.getYear(), m, d, h, mi, s, 0, LegacyUtils.legacyTimeZone);
            Norloge norloge = new Norloge().withTime(norlogeTime).withBouchot(bouchot);
            for (int year = 0; year <= MAX_YEAR_BEFORE; ++year) {
                ZonedDateTime tryTime = norlogeTime.minusYears(year);
                Post post = postRepository.findOne(null != norloge.getBouchot() ? norloge.getBouchot() : room, tryTime, tryTime.plusSeconds(norloge.getPrecisionInSeconds()), indice);
                if (null != post) {
                    return Norloge.format(post);
                }
            }
            return null;
        }

        private static final int MAX_DAY_BEFORE = 100;

        public String convertNormalNorloge(int h, int mi, int s, int indice, String bouchot) {
            ZonedDateTime norlogeTime = ZonedDateTime.of(postTime.getYear(), postTime.getMonthValue(), postTime.getDayOfMonth(), h, mi, s, 0, LegacyUtils.legacyTimeZone);
            Norloge norloge = new Norloge().withTime(norlogeTime).withBouchot(bouchot);
            for (int day = 0; day <= MAX_DAY_BEFORE; ++day) {
                ZonedDateTime tryTime = norlogeTime.minusDays(day);
                Post post = postRepository.findOne(null != norloge.getBouchot() ? norloge.getBouchot() : room, tryTime, tryTime.plusSeconds(norloge.getPrecisionInSeconds()), indice);
                if (null != post) {
                    return Norloge.format(post);
                }
            }
            return null;
        }

        public String convertShortNorloge(int h, int mi, int indice, String bouchot) {
            ZonedDateTime norlogeTime = ZonedDateTime.of(postTime.getYear(), postTime.getMonthValue(), postTime.getDayOfMonth(), h, mi, 0, 0, LegacyUtils.legacyTimeZone);
            Norloge norloge = new Norloge().withTime(norlogeTime).withBouchot(bouchot);
            for (int day = 0; day <= MAX_DAY_BEFORE; ++day) {
                ZonedDateTime tryTime = norlogeTime.minusDays(day);
                Post post = postRepository.findOne(null != norloge.getBouchot() ? norloge.getBouchot() : room, tryTime, tryTime.plusSeconds(norloge.getPrecisionInSeconds()), indice);
                if (null != post) {
                    return Norloge.format(post);
                }
            }
            return null;
        }

        public String convertIdNorloge(String id, String bouchot) {
            Post post = postRepository.findOne(id);
            if (null == post) {
                GatewayPostId gpid = new GatewayPostId();
                gpid.setPostId(id);
                gpid.setGateway(null != bouchot ? bouchot : room);
                post = postRepository.findOneByGatewayId(gpid);
            }
            if (null != post) {
                return Norloge.format(post);
            }
            return null;
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

    public String convertFromLegacyNorloge(String message, ZonedDateTime postTime, String room) {
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
