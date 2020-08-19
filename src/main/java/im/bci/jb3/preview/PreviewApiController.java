package im.bci.jb3.preview;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/preview")
public class PreviewApiController {

    private final Log LOGGER = LogFactory.getLog(this.getClass());

    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
    public Preview url(@RequestParam(name = "url", required = true) String url, Model model) {
        Preview preview = new Preview();
        try {
            url = replaceBadSites(url);
            Document doc = Jsoup.connect(url).get();
            extractTitle(preview, doc);
            extractDescription(preview, doc);
            extractImage(preview, doc);
        } catch (IOException e) {
            preview.setTitle(url);
            LOGGER.warn("Cannot preview url " + url, e);
        }
        return preview;
    }

    public String replaceBadSites(String url) {
        URI uri = URI.create(url);
        if ("twitter.com".equals(uri.getHost())) {
            url = UriComponentsBuilder.fromUri(uri).host("nitter.net").toUriString();
        }
        return url;
    }

    private void extractImage(Preview preview, Document doc) {
        Element metaOgImage = doc.selectFirst("meta[property=og:image]");
        if (null != metaOgImage) {
            String image = metaOgImage.absUrl("content");
            image = Jsoup.clean(image, Whitelist.none());
            if (StringUtils.isNotBlank(image)) {
                preview.setImage(image);
            }
        }
    }

    private void extractTitle(Preview preview, Document doc) {
        Element metaOgTitle = doc.selectFirst("meta[property=og:title]");
        String title = StringUtils.firstNonBlank(null != metaOgTitle ? metaOgTitle.attr("content") : null, doc.title(), doc.baseUri());
        preview.setTitle(Jsoup.clean(title, Whitelist.none()));
    }

    private void extractDescription(Preview preview, Document doc) {
        Element metaOgDescription = doc.selectFirst("meta[property=og:description]");
        if(null != metaOgDescription) {
            String description = metaOgDescription.attr("content");
            preview.setDescription(Jsoup.clean(description, Whitelist.none()));
        }
    }

}
