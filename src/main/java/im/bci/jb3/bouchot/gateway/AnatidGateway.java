package im.bci.jb3.bouchot.gateway;

import im.bci.jb3.bouchot.data.GatewayPostId;
import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.legacy.LegacyPost;
import im.bci.jb3.bouchot.legacy.LegacyUtils;
import im.bci.jb3.bouchot.logic.CleanUtils;
import im.bci.jb3.event.NewPostsEvent;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Request.Builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
@ConditionalOnProperty(name = "jb3.anatid.url")
@Component
public class AnatidGateway extends WebSocketListener implements Gateway {

	private final Log LOGGER = LogFactory.getLog(this.getClass());

	@Autowired
	private PostRepository postPepository;

	@Autowired
	private OkHttpClient httpClient;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	protected LegacyUtils legacyUtils;

	@Resource(name = "mouleScheduler")
	private TaskScheduler scheduler;

	@Autowired
	private ApplicationEventPublisher publisher;

	private HttpUrl postUrl, pollUrl;
	private List<String> rooms;
	private final BouchotPostCallBack bouchotPostCallback = new BouchotPostCallBack();
	private int nbConnexionFailOrClose;

	@Value("${jb3.anatid.url}")
	public void setPlopToUrl(String anatidUrl) {
		if (StringUtils.isNotBlank(anatidUrl)) {
			this.postUrl = HttpUrl.parse(anatidUrl).newBuilder().addPathSegments("post").build();
			this.pollUrl = HttpUrl.parse(anatidUrl).newBuilder().addPathSegments("poll").build();
		}
	}

	@Value("${jb3.anatid.rooms:}")
	public void setRooms(String rooms) {
		this.rooms = Arrays.asList(StringUtils.split(rooms, ','));
	}

	@PostConstruct
	public void connect() {
		scheduler.schedule(() -> {
			Request request = new Request.Builder().url(pollUrl).build();
			httpClient.newWebSocket(request, this);
		}, Instant.now().plus(nbConnexionFailOrClose, ChronoUnit.MINUTES));
	}

	@Override
	public void onOpen(WebSocket ws, Response response) {
		nbConnexionFailOrClose = Math.max(0, nbConnexionFailOrClose - 1);
		LOGGER.info("Connected to anatid");
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		try {
			LegacyPost legacyPost = objectMapper.readValue(text, LegacyPost.class);
			if (rooms.contains(legacyPost.getTribune())) {
				importPost(legacyPost);
			}
		} catch (IOException e) {
			LOGGER.error(text);
		}

	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		LOGGER.info("Disconnected from anatid: " + code + " " + reason);
		nbConnexionFailOrClose = Math.min(30, nbConnexionFailOrClose + 1);
		this.connect();
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		LOGGER.error("Connection failure from anatid", t);
		nbConnexionFailOrClose = Math.min(30, nbConnexionFailOrClose + 1);
		this.connect();
	}

	public void importPost(LegacyPost legacyPost) {
		try {
			GatewayPostId gatewayPostId = new GatewayPostId();
			gatewayPostId.setGateway(legacyPost.getTribune());
			gatewayPostId.setPostId(String.valueOf(legacyPost.getId()));
			if (!postPepository.existsByGatewayPostId(gatewayPostId)) {
				Post post = new Post();
				post.setGatewayPostId(gatewayPostId);
				post.setRoom(legacyPost.getTribune());
				ZonedDateTime postTimeRounded = ZonedDateTime.parse(legacyPost.getTime(),LegacyUtils.legacyPostTimeFormatter).truncatedTo(ChronoUnit.SECONDS);
				long nbPostsAtSameSecond = postPepository.countPosts(postTimeRounded, postTimeRounded.plusSeconds(1),
						legacyPost.getTribune());
				post.setTime(postTimeRounded.with(ChronoField.MILLI_OF_SECOND, nbPostsAtSameSecond));
				String nickname = CleanUtils.truncateNickname(legacyPost.getLogin());
				if (StringUtils.isBlank(nickname)) {
					nickname = CleanUtils.truncateNickname(legacyPost.getInfo());
				}
				post.setNickname(CleanUtils.cleanNickname(nickname));
				post.setMessage(legacyUtils.convertFromLegacyNorloges(
						CleanUtils.cleanMessage(CleanUtils.truncateMessage(legacyPost.getMessage())), post.getTime(),
						legacyPost.getTribune()));
				postPepository.save(post);
				publisher.publishEvent(new NewPostsEvent(post));
			}
		} catch (Exception e) {
			LogFactory.getLog(this.getClass()).warn(e);
		}
	}

	@Override
	public boolean handlePost(String nickname, String message, String room, String auth) {
		if (rooms.contains(room)) {
			try {
				if (null != postUrl) {
					okhttp3.FormBody.Builder body = new FormBody.Builder().add("message",
							legacyUtils.convertToLegacyNorloges(message, ZonedDateTime.now(LegacyUtils.legacyTimeZone).truncatedTo(ChronoUnit.SECONDS), room))
							.add("tribune", room);
					if (StringUtils.isNotBlank(auth)) {
						body.add("auth", auth);
					}
					Builder request = new Request.Builder().url(postUrl).header("User-Agent", nickname)
							.post(body.build());
					httpClient.newCall(request.build()).enqueue(bouchotPostCallback);
				}
			} catch (Exception e) {
				LOGGER.error("post error", e);
			}
			return true;
		} else {
			return false;
		}
	}

	private class BouchotPostCallBack implements Callback {

		@Override
		public void onResponse(Call call, Response response) throws IOException {
		}

		@Override
		public void onFailure(Call call, IOException e) {
			LOGGER.error("post error", e);
		}
	}
}
