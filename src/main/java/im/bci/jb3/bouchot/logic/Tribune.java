package im.bci.jb3.bouchot.logic;

import im.bci.jb3.bouchot.data.Fortune;
import im.bci.jb3.bouchot.data.FortuneRepository;
import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.data.PostRevisor;
import im.bci.jb3.bouchot.logic.Norloge.ParsedNorloges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.RateLimiter;
import im.bci.jb3.event.NewPostsEvent;
import java.util.concurrent.TimeUnit;
import org.springframework.context.ApplicationEventPublisher;

@Component
public class Tribune {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PostRepository postPepository;

    @Autowired
    private FortuneRepository fortunePepository;
    
    private RateLimiter rateLimiter = RateLimiter.create(1.0);

    public Post post(String nickname, String message, String room) {
        if(rateLimiter.tryAcquire(10, TimeUnit.SECONDS)) {
            nickname = CleanUtils.truncateNickname(nickname);
            room = CleanUtils.truncateRoom(room);
            message = CleanUtils.truncateMessage(message);
            if (StringUtils.isNotBlank(message)) {
                Post post = new Post();
                post.setNickname(StringUtils.isNotBlank(nickname) ? nickname : "AnonymousCoward");
                post.setMessage(message);
                post.setRoom(room);
                post.setTime(DateTime.now(DateTimeZone.UTC));
                postPepository.save(post);
                publisher.publishEvent(new NewPostsEvent(post));
                return post;
            }
        }
        return null;
    }

    public Fortune fortune(Post post) {
        ParsedNorloges parsed = Norloge.parseNorloges(post.getMessage());
        final List<Post> posts = getForNorloges(parsed);
        if (!posts.isEmpty()) {
            Fortune f = new Fortune();
            f.setFortuner(post.getCleanedNickname());
            String fortuneTitle = parsed.getRemainingMessageContent();
            fortuneTitle = StringUtils.removeStart(fortuneTitle, "/fortune ");
            fortuneTitle = StringUtils.removeStart(fortuneTitle, "fortune< ");
            fortuneTitle = CleanUtils.truncateFortuneTitle(fortuneTitle);
            f.setTitle(fortuneTitle);
            f.setPosts(posts);
            return fortunePepository.save(f);
        } else {
            return null;
        }
    }

    public boolean isBotCall(Post post, String botName) {
        if (botName.equals(post.getNickname())) {
            return false;
        }
        String message = post.getCleanedMessage();
        return message.contains(bigornoCall(botName)) || message.contains(ircCall(botName));
    }

    public boolean isReplyToBot(Post post, String botName) {
        for (Norloge norloge : Norloge.parseNorloges(post.getMessage())) {
            for (Post referencedPost : getForNorloge(norloge)) {
                if (botName.equals(referencedPost.getNickname())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String messageWithoutBotCall(Post post, String botName) {
        String message = post.getCleanedMessage();
        message = StringUtils.removeStart(message, bigornoCall(botName));
        message = StringUtils.removeStart(message, ircCall(botName));
        return message;
    }

    private static String bigornoCall(String botName) {
        return Jsoup.clean(botName + "<", Whitelist.none());
    }

    private static String ircCall(String botName) {
        return Jsoup.clean("/" + botName, Whitelist.none());
    }

    public List<Post> getForNorloges(List<Norloge> norloges) {
        HashSet<Post> posts = new HashSet<Post>();
        for (Norloge norloge : norloges) {
            posts.addAll(getForNorloge(norloge));
        }
        ArrayList<Post> result = new ArrayList<Post>(posts);
        Collections.sort(result, new Comparator<Post>() {

            @Override
            public int compare(Post o1, Post o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });
        return result;
    }

    public List<Post> getForNorloge(Norloge norloge) {
        if (null == norloge.getBouchot()) {
            if (null != norloge.getId()) {
                final Post post = postPepository.findOne(norloge.getId());
                if (null != post) {
                    return Arrays.asList(post);
                }
            } else if (null != norloge.getTime()) {
                DateTime start = norloge.getTime();
                DateTime end = norloge.getTime().plusSeconds(1);
                return postPepository.findPosts(start, end, null);
            }
        }
        return Collections.emptyList();
    }

    public void revise(PostRevisor revisor, Post post, String newMessage) throws JsonProcessingException {
        if (revisor.canRevise(post) && StringUtils.isNotBlank(newMessage)) {
            post.revise(CleanUtils.truncateMessage(newMessage));
            postPepository.save(post);
            publisher.publishEvent(new NewPostsEvent(post));
        }
    }

}
