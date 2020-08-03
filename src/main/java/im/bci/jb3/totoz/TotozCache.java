package im.bci.jb3.totoz;

import im.bci.jb3.bouchot.logic.CleanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author devnewton
 */
@Component
public class TotozCache {

    private Path totozDir;
    private final Log LOGGER = LogFactory.getLog(this.getClass());

    @Value("${jb3.totoz.url}")
    private String totozUrl;

    @Value("${jb3.totoz.dir:}")
    public void setTotozDir(String totozDir) throws IOException {
        if (StringUtils.isEmpty(totozDir)) {
            String cacheDir = System.getenv("XDG_CACHE_HOME");
            if (StringUtils.isEmpty(cacheDir)) {
                cacheDir = new File(System.getProperty("user.home"), ".cache").getAbsolutePath();
            }
            this.totozDir = Path.of(cacheDir, "jb3", "totoz");
            Files.createDirectories(this.totozDir);
        } else {
            this.totozDir = Path.of(totozDir);
        }
    }

    public Path cacheTotoz(String totoz) throws IOException {
        String totozFilename = CleanUtils.encodeFilename(totoz);
        Path totozFile = totozDir.resolve(totozFilename);
        if (!Files.exists(totozFile)) {
            URL totozImgUrl = UriComponentsBuilder.fromHttpUrl(totozUrl).path("/img/").path(totoz).build().toUri()
                    .toURL();
            try (InputStream is = totozImgUrl.openStream()) {
                Files.copy(is, totozFile);
            }
        }
        return totozFile;
    }

    public void cacheMetadata(String totoz) {
        try {
            String totozPageUrl = UriComponentsBuilder.fromHttpUrl(totozUrl).path("/totoz/").path(totoz).build().toString();
            Document doc = Jsoup.connect(totozPageUrl).get();
            Properties metadata = new Properties();
            metadata.setProperty("author", doc.selectFirst(".username").text());
            metadata.setProperty("tags", doc.select(".tags a").text());
            if (null != doc.selectFirst("span:contains(NSFW)")) {
                metadata.setProperty("nsfw", "true");
            }
            String totozFilename = CleanUtils.encodeFilename(totoz) + ".properties";
            Path totozFile = totozDir.resolve(totozFilename);
            try (BufferedWriter writer = Files.newBufferedWriter(totozFile)) {
                metadata.store(writer, null);
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot retrieve totoz metadata for " + totoz, e);
        }
    }

}
