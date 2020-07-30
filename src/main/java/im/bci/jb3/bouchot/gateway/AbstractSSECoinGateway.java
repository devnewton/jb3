package im.bci.jb3.bouchot.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.data.PostRevision;
import im.bci.jb3.bouchot.logic.CleanUtils;
import im.bci.jb3.event.NewPostsEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

/**
 *
 * @author devnewton
 */
public abstract class AbstractSSECoinGateway extends EventSourceListener implements Gateway {

    private final Log LOGGER = LogFactory.getLog(this.getClass());
    @Autowired
    private OkHttpClient httpClient;
    @Autowired
    private EventSource.Factory eventSourceFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private PostRepository postPepository;
    @Resource(name = "mouleScheduler")
    private TaskScheduler scheduler;
    private final Jb3BouchotConfig config;
    private int nbConnexionFailOrClose;

    public AbstractSSECoinGateway(Jb3BouchotConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void connect() {
        scheduler.schedule(() -> {
            Request request = new Request.Builder().url(config.getUrl() + "/ssecoin/posts/stream?rooms=" + config.getRemoteRoom()).build();
            eventSourceFactory.newEventSource(request, this);
        }, Instant.now().plus(nbConnexionFailOrClose, ChronoUnit.MINUTES));
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        if (response.isSuccessful()) {
            nbConnexionFailOrClose = Math.max(0, nbConnexionFailOrClose - 1);
        }
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        try {
            if ("presence".equals(type)) {
                //TODO broadcast presence ?
            } else {
                Post post = objectMapper.readValue(data, Post.class);
                if (null != post) {
                    importPosts(Arrays.asList(post));
                }
            }
        } catch (IOException ex) {
            LOGGER.error(ex);
        }
    }

    @Override
    public synchronized boolean handlePost(String nickname, String messageBody, String room, String auth) {
        if (StringUtils.equals(config.getLocalRoom(), room)) {
            try {
                RequestBody formBody = new FormBody.Builder()
                        .add("nickname", nickname)
                        .add("message", messageBody)
                        .add("room", config.getRemoteRoom())
                        .add("auth", auth)
                        .build();
                Request request = new Request.Builder()
                        .url(config.getUrl() + "/ssecoin/posts/add")
                        .post(formBody)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
            return true;
        } else {
            return false;
        }
    }

    private synchronized void importPosts(List<Post> posts) {
        ArrayList<Post> newPosts = new ArrayList<>();
        for (Post post : posts) {
            post.setId(CleanUtils.truncateId(post.getId()));
            if (StringUtils.equals(config.getRemoteRoom(), post.getRoom())) {
                post.setRoom(config.getLocalRoom());
                post.setNickname(CleanUtils.truncateNickname(post.getNickname()));
                post.setMessage(CleanUtils.truncateMessage(post.getMessage()));
                if (null != post.getRevisions()) {
                    for (PostRevision revision : post.getRevisions()) {
                        revision.setMessage(CleanUtils.truncateMessage(revision.getMessage()));
                    }
                }
                postPepository.save(post);
                newPosts.add(post);
            }
        }
        if (!newPosts.isEmpty()) {
            publisher.publishEvent(new NewPostsEvent(newPosts));
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        LOGGER.info("Disconnected from " + config.getLocalRoom());
        nbConnexionFailOrClose = Math.min(30, nbConnexionFailOrClose + 1);
        this.connect();
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        LOGGER.error("Connection failure from " + config.getLocalRoom(), t);
        nbConnexionFailOrClose = Math.min(30, nbConnexionFailOrClose + 1);
        this.connect();
    }

}
