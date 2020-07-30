package im.bci.jb3.bouchot.legacy;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.logic.UserPostHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 *
 * @author devnewton
 */
@Controller
@RequestMapping("/legacy")
public class LegacyController {

    @Autowired
    private UserPostHandler tribune;

    @Autowired
    private PostRepository postPepository;

    @Autowired
    private LegacyUtils legacyUtils;

    @Value("${jb3.defaults.room}")
    private String defaultRoom;

    private Duration postsGetPeriod;

    @Value("${jb3.posts.get.period}")
    public void setPostsGetPeriod(String p) {
        postsGetPeriod = Duration.parse(p);
    }

    @RequestMapping(path = "/discovery", method = RequestMethod.GET)
    public String discovery(@RequestParam(value = "room", required = false) String room, Model model,
            HttpServletResponse response) {
        model.addAttribute("room", StringUtils.isNotBlank(room) ? room : defaultRoom);
        model.addAttribute("baseurl",
                ServletUriComponentsBuilder.fromCurrentRequest().replacePath("").build().toString());
        response.setContentType("text/xml");
        return "bouchot/legacy/discovery";
    }

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public String post(@RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "message") String message,
            @RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "last", required = false) Long last,
            @RequestHeader(value = "User-Agent", required = false) String userAgent, WebRequest webRequest,
            @RequestParam(value = "auth", required = false) String auth, Model model, HttpServletResponse response) {
        if (StringUtils.isBlank(nickname)) {
            nickname = userAgent;
        }
        Post post = tribune.post(nickname, legacyUtils.convertFromLegacyNorloges(message, ZonedDateTime.now(), room), room,
                auth, ServletUriComponentsBuilder.fromCurrentRequest());
        if (null != post) {
            response.addHeader("X-Post-Id", Long.toString(post.getTime().toInstant().toEpochMilli()));
        }
        return xml(room, last, webRequest, model, response);
    }

    @RequestMapping(path = "/xml", method = RequestMethod.GET)
    public String xml(@RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "last", required = false) Long lastId, WebRequest webRequest, Model model,
            HttpServletResponse response) {
        if (get(lastId, room, webRequest, model, LegacyUtils.xmlEscaper)) {
            response.setContentType("text/xml");
            return "bouchot/legacy/xml";
        } else {
            return null;
        }
    }

    @RequestMapping(path = "/tsv", method = RequestMethod.GET)
    public String tsv(@RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "last", required = false) Long lastId, WebRequest webRequest, Model model,
            HttpServletResponse response) {
        if (get(lastId, room, webRequest, model, LegacyUtils.csvEscaper)) {
            response.setContentType("text/tab-separated-values");
            response.setHeader("Content-Disposition", "attachment; filename=\"backend.tsv\"");
            return "bouchot/legacy/tsv";
        } else {
            return null;
        }
    }

    @RequestMapping(path = "/bsv", method = RequestMethod.GET)
    public String bsv(@RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "last", required = false) Long lastId, WebRequest webRequest, Model model,
            HttpServletResponse response) {
        if (get(lastId, room, webRequest, model, LegacyUtils.bsvEscaper)) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"backend.bsv\"");
            return "bouchot/legacy/bsv";
        } else {
            return null;
        }
    }

    private boolean get(Long lastId, String room, WebRequest webRequest, Model model, LegacyUtils.Escaper escaper) {
        ZonedDateTime end = ZonedDateTime.now(ZoneId.of("UTC")).plusHours(1);
        ZonedDateTime start = computeStartTime(lastId, end);
        List<Post> posts = postPepository.findPosts(start, end, room);
        if (posts.isEmpty() || webRequest.checkNotModified(posts.get(0).getTime().toInstant().toEpochMilli())) {
            return false;
        } else {
            LegacyBoard board = new LegacyBoard();
            board.setSite(ServletUriComponentsBuilder.fromCurrentRequest().replacePath("").build().toString());
            board.setTimezone(LegacyUtils.legacyTimezoneId);
            List<LegacyPost> legacyPosts = posts2legacy(posts, escaper);
            board.setPosts(legacyPosts);
            model.addAttribute("board", board);
            return true;
        }
    }

    private List<LegacyPost> posts2legacy(List<Post> posts, LegacyUtils.Escaper escaper) {
        List<LegacyPost> legacyPosts = new ArrayList<>(posts.size());
        for (Post post : posts) {
            legacyPosts.add(legacyUtils.post2legacy(post, escaper));
        }
        return legacyPosts;
    }

    private ZonedDateTime computeStartTime(Long lastId, ZonedDateTime end) {
        ZonedDateTime start;
        if (null != lastId) {
            start = Instant.ofEpochMilli(lastId).atZone(ZoneId.of("UTC"));
            if (start.isAfter(end)) {
                start = end.minusSeconds(1);
            }
        } else {
            start = end.minus(postsGetPeriod);
        }
        // workaround shameful olcc new year bug
        if (start.getYear() < end.getYear()) {
            start = ZonedDateTime.of(end.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        }
        return start;
    }

}
