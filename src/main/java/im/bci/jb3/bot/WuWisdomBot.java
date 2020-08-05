package im.bci.jb3.bot;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.logic.Norloge;
import im.bci.jb3.bouchot.logic.Tribune;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WuWisdomBot implements Bot {

    @Autowired
    private Tribune tribune;

    private static final String NAME = "wisdom";

    @Override
    public void handle(Post post, UriComponentsBuilder uriBuilder) {
        try {
            if (tribune.isBotCall(post, NAME)) {
                Document doc = Jsoup.connect("https://wutangclan.net/wu-wisdom/").get();
                Element wisdom = doc.select("blockquote.wu-wisdom").first();
                String quote = wisdom.text();
                tribune.post(NAME, String.format("%s Here some Wu-Tang wisdom : %s", Norloge.format(post), quote), post.getRoom());
            }
        } catch (Exception ex) {
            LogFactory.getLog(this.getClass()).error(String.format("%s bot error", NAME), ex);
        }
    }

    static String extractWisdom(String content) {
        Document doc = Jsoup.parse(content, "UTF-8");
        Element wisdom = doc.select("blockquote.wu-wisdom").first();
        return wisdom.text();
    }
}
