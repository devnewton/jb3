package im.bci.jb3.totoz;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/totoz")
public class TotozController {

    @Autowired
    private TotozCache cache;

    @RequestMapping("/img/{totoz}")
    @ResponseBody
    public ResponseEntity<FileSystemResource> img(@PathVariable("totoz") String totoz)
            throws MalformedURLException, IOException {
        Path totozFile = cache.cacheTotoz(totoz);
        cache.cacheMetadata(totoz);

        return ResponseEntity.ok().lastModified(Files.getLastModifiedTime(totozFile).toInstant()).contentType(detectContentType(totozFile))
                .contentLength(Files.size(totozFile)).body(new FileSystemResource(totozFile));
    }

    private MediaType detectContentType(Path totozFile) {
        try {
            return MediaType.parseMediaType(Files.probeContentType(totozFile));
        } catch (Exception e) {
            return MediaType.IMAGE_PNG;
        }

    }

}
