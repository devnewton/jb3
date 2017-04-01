package im.bci.jb3.bot.sara;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.logic.Tribune;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author devnewton
 */
@Component
public class JourneeMondiale implements SaraAction {

    @Autowired
    private Tribune tribune;
    
    private static final Pattern JOURNEE = Pattern.compile("\\b(journées?|jours?|aujourd)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public MatchLevel match(Post post) {
        return JOURNEE.matcher(post.getMessage()).find() ? MatchLevel.MUST : MatchLevel.CAN;
    }

    @Override
    public boolean act(Post post, UriComponentsBuilder uriBuilder) {
        try {
            Document doc = Jsoup.connect("http://www.journee-mondiale.com/").get();
            String journee = doc.select("#journeesDuJour > article:nth-child(1) > a:nth-child(1) > h2:nth-child(2)").text();
            if (StringUtils.isNotBlank(journee)) {
                String message = "Aujourd'hui, c'est la " + journee + ". Hihi!";
                tribune.post(Sara.NAME, message, post.getRoom());
                return true;
            }
        } catch (IOException ex) {
            Logger.getLogger(JourneeMondiale.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

}
