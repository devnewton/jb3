package im.bci.jb3.logic;

import java.util.ArrayList;
import java.util.List;
import java.time.ZonedDateTime;
import org.junit.Test;

import im.bci.jb3.bouchot.legacy.LegacyUtils;
import im.bci.jb3.bouchot.logic.Norloge;
import java.time.temporal.ChronoField;

import static org.junit.Assert.*;

public class NorlogeTest {

    @Test
    public void testParseNorloges() {
        System.out.println("parseNorloges");
        String message = " moules< 16:25:11 les posts du style 12/31#18:28:15 ou 2012/01/02#18:12:12 ou encore 2016-01-06T21:14:21 c'est conforme à la rfc et #tamaman #1234 #n3 #lol@dlfp  #troll@euro.net? et 12:30?";
        List<Norloge> expResult = new ArrayList<>();
        expResult.add(new Norloge().withTime(ZonedDateTime.now(LegacyUtils.legacyTimeZone).withHour(16).withMinute(25).withSecond(11).with(ChronoField.MILLI_OF_SECOND, 0)));
        expResult.add(new Norloge().withTime(ZonedDateTime.now(LegacyUtils.legacyTimeZone).withMonth(12).withDayOfMonth(31).withHour(18).withMinute(28).withSecond(15).with(ChronoField.MILLI_OF_SECOND, 0)));
        expResult.add(new Norloge().withTime(ZonedDateTime.now(LegacyUtils.legacyTimeZone).withYear(2012).withMonth(1).withDayOfMonth(2).withHour(18).withMinute(12).withSecond(12).with(ChronoField.MILLI_OF_SECOND, 0)));
        expResult.add(new Norloge().withTime(ZonedDateTime.now(LegacyUtils.legacyTimeZone).withYear(2016).withMonth(1).withDayOfMonth(6).withHour(21).withMinute(14).withSecond(21).with(ChronoField.MILLI_OF_SECOND, 0)));
        expResult.add(new Norloge().withId("tamaman"));
        expResult.add(new Norloge().withId("1234"));
        expResult.add(new Norloge().withId("n3"));
        expResult.add(new Norloge().withId("lol").withBouchot("dlfp"));
        expResult.add(new Norloge().withId("troll").withBouchot("euro.net"));
        expResult.add(new Norloge().withTime(ZonedDateTime.now(LegacyUtils.legacyTimeZone).withHour(12).withMinute(30).withSecond(0).with(ChronoField.MILLI_OF_SECOND, 0)));
        List<Norloge> result = Norloge.parseNorloges(message);
        assertArrayEquals(expResult.toArray(), result.toArray());
    }

}
