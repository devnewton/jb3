package im.bci.jb3.sse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.logic.UserPostHandler;
import im.bci.jb3.event.NewPostsEvent;

@Service
public class SseCoinService {
    @Autowired
    private UserPostHandler tribune;

    @Autowired
    private PostRepository postRepository;

    private CopyOnWriteArraySet<SseMoule> moules = new CopyOnWriteArraySet<>();

    private Period postsGetPeriod;

    @Value("${jb3.posts.get.period}")
    public void setPostsGetPeriod(String p) {
        postsGetPeriod = ISOPeriodFormat.standard().parsePeriod(p);
    }

    public SseMoule addMoule(String[] rooms) {
        SseMoule moule = new SseMoule(rooms);
        moules.add(moule);
        moule.emiter.onCompletion(() -> moules.remove(moule));
        return moule;
    }

    @Async
    public void emitPosts(SseMoule moule) {
        DateTime end = DateTime.now(DateTimeZone.UTC).plusHours(1);
        DateTime start = end.minus(postsGetPeriod);
        for (String room : moule.rooms) {
            List<Post> posts = postRepository.findPosts(start, end, room);
            try {
                for (Post post : posts) {
                    moule.emiter.send(post);
                }
            } catch (Exception e) {
                moules.remove(moule);
                return;
            }
        }
    }
    
    public void post(String nickname, String message, String room, String auth) {
        tribune.post(nickname, message, room, auth, ServletUriComponentsBuilder.fromCurrentRequest());
    }

    public Post findOne(String messageId) {
        return postRepository.findOne(messageId);
    }

    @EventListener
    public void broadcastPost(NewPostsEvent event) {
        for (SseMoule moule : moules) {
            try {
                for (Post post : event.getPosts()) {
                    if (moule.rooms.contains(post.getRoom())) {
                        moule.emiter.send(post);
                    }
                }
            } catch (Exception e) {
                moules.remove(moule);
            }
        }
    }

    @Async
    public void broadcastPresence(Presence presence) {
        for (SseMoule moule : moules) {
            try {
                moule.emiter.send(SseEmitter.event().name("presence").data(presence));
            } catch (Exception e) {
                moules.remove(moule);
            }
        }
    }
}
