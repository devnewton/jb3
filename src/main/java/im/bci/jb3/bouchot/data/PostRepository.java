package im.bci.jb3.bouchot.data;

import java.util.List;
import java.time.ZonedDateTime;

import im.bci.jb3.coincoin.PostSearchRQ;

public interface PostRepository {

    List<Post> findPosts(ZonedDateTime start, ZonedDateTime end, String room);

    List<Post> findPostsReverse(ZonedDateTime start, ZonedDateTime end, String room);

    long countPosts(ZonedDateTime start, ZonedDateTime end, String room);

    Post findOne(String room, ZonedDateTime start, ZonedDateTime end, int indice);

    void save(Post post);

    Post findOne(String id);

    boolean existsById(String gpid);

    Post findOneByGatewayId(GatewayPostId gpid);

    boolean existsByGatewayPostId(GatewayPostId gpid);

    List<Post> search(PostSearchRQ rq);
}
