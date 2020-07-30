package im.bci.jb3.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import im.bci.jb3.bouchot.legacy.LegacyUtils;

import java.io.IOException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 *
 * @author devnewton
 */
@Component
public class FormatDateTimeHelper implements Helper<ZonedDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(LegacyUtils.legacyTimeZone);

    @Override
    public CharSequence apply(ZonedDateTime dateTime, Options options) throws IOException {
        return new Handlebars.SafeString(formatter.format(dateTime));
    }

}
