package im.bci.jb3.bouchot.gateway;

import im.bci.jb3.bouchot.data.GatewayPostId;
import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.legacy.LegacyUtils;
import im.bci.jb3.bouchot.logic.CleanUtils;
import im.bci.jb3.bouchot.websocket.WebDirectCoinConnectedMoules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TriggerContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
public abstract class AbstractXmlBouchotGateway implements Gateway, SchedulableGateway {

    @Autowired
    private WebDirectCoinConnectedMoules connectedMoules;
    @Autowired
    private PostRepository postPepository;
    @Autowired
    protected LegacyUtils legacyUtils;
    private long lastPostId = -1;

    @Value("${jb3.secure}")
    private boolean validateCrapCertificate;

    private final BouchotConfig config;
    private final BouchotAdaptiveRefreshComputer adaptativeRefreshComputer = new BouchotAdaptiveRefreshComputer();

    protected AbstractXmlBouchotGateway(BouchotConfig config) {
        this.config = config;
    }

    protected void importPosts() {
        try {
            Connection connect = Jsoup.connect(config.getGetUrl()).userAgent("jb3")
                    .validateTLSCertificates(validateCrapCertificate || !config.isUsingCrapCertificate());
            if (null != config.getLastIdParameterName()) {
                connect = connect.data(config.getLastIdParameterName(), String.valueOf(lastPostId));
            }
            Document doc = connect.parser(Parser.xmlParser()).get();
            parsePosts(doc);
        } catch (org.jsoup.HttpStatusException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_MODIFIED.value()) {
                adaptativeRefreshComputer.error();
                LogFactory.getLog(this.getClass()).error("http get error", ex);
            }
        } catch (IOException ex) {
            adaptativeRefreshComputer.error();
            LogFactory.getLog(this.getClass()).error("get error", ex);
        }
    }

    private synchronized void parsePosts(Document doc) throws JsonProcessingException {
        Elements postsToImport = doc.select("post");
        ArrayList<Post> newPosts = new ArrayList<>();
        for (ListIterator<Element> iterator = postsToImport.listIterator(postsToImport.size()); iterator
                .hasPrevious();) {
            Element postToImport = iterator.previous();
            GatewayPostId gatewayPostId = new GatewayPostId();
            gatewayPostId.setGateway(config.getRoom());
            long postId = Long.parseLong(postToImport.attr("id"));
            gatewayPostId.setPostId(String.valueOf(postId));
            if (!postPepository.existsByGatewayPostId(gatewayPostId)) {
                Post post = new Post();
                post.setGatewayPostId(gatewayPostId);
                String nickname = decodeTags(postToImport.select("login").first());
                if (StringUtils.isBlank(nickname)) {
                    nickname = CleanUtils.truncateNickname(decodeTags(postToImport.select("info").first()));
                }
                post.setNickname(CleanUtils.cleanNickname(nickname));
                post.setRoom(config.getRoom());
                DateTime postTimeRounded = LegacyUtils.legacyPostTimeFormatter.parseDateTime(postToImport.attr("time")).secondOfMinute().roundFloorCopy();
                long nbPostsAtSameSecond = postPepository.countPosts(postTimeRounded, postTimeRounded.plusSeconds(1), config.getRoom());
                post.setTime(postTimeRounded.withMillisOfSecond((int) nbPostsAtSameSecond));
                post.setMessage(legacyUtils.convertFromLegacyNorloges(CleanUtils.cleanMessage(CleanUtils.truncateMessage(decodeTags(postToImport.select("message").first()))), post.getTime(), config.getRoom()));
                postPepository.save(post);
                newPosts.add(post);
            }
            if (postId > lastPostId) {
                lastPostId = postId;
            }
        }
        adaptativeRefreshComputer.analyseBouchotPostsResponse(newPosts);
        if (!newPosts.isEmpty()) {
            connectedMoules.send(newPosts);
        }
    }

    @Override
    public void post(String nickname, String message, String auth) {
        try {
            Connection connect = Jsoup.connect(config.getPostUrl()).userAgent("jb3")
                    .data(config.getMessageContentParameterName(),
                            legacyUtils.convertToLegacyNorloges(message,
                                    DateTime.now().withZone(LegacyUtils.legacyTimeZone).secondOfMinute()
                                    .roundFloorCopy(), getRoom()))
                    .userAgent(nickname)
                    .validateTLSCertificates(validateCrapCertificate || !config.isUsingCrapCertificate());
            if (null != config.getCookieName()) {
                connect = connect.cookie(config.getCookieName(), auth);
            }
            if (null != config.getReferrer()) {
                connect = connect.referrer(config.getReferrer());
            }
            if (null != config.getLastIdParameterName()) {
                connect = connect.data(config.getLastIdParameterName(), String.valueOf(lastPostId));
            }
            Document doc = connect.parser(Parser.xmlParser()).post();
            if (config.isUsingXPost()) {
                parsePosts(doc);
            } else {
                importPosts();
            }
        } catch (IOException ex) {
            LogFactory.getLog(this.getClass()).error("post error", ex);
        }
    }

    @Override
    public String getRoom() {
        return config.getRoom();
    }

    private String decodeTags(Element message) {
        if (config.isTagsEncoded()) {
            return StringEscapeUtils.unescapeXml(message.text());
        } else {
            return message.html();
        }
    }

    @Override
    public Date nextExecutionTime(TriggerContext tc) {
        return adaptativeRefreshComputer.nextRefreshDate();
    }

    @Override
    public void run() {
        importPosts();
    }

}
