package im.bci.jb3.bouchot.data;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;

import im.bci.jb3.coincoin.PostSearchRQ;

@Component
public class PostRepositoryImpl implements PostRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${jb3.defaults.room}")
    private String defaultRoom;

    @Value("${jb3.room.history.size}")
    private int roomHistorySize;

    private String roomOrDefault(String room) {
        return StringUtils.isNotBlank(room) ? room : defaultRoom;
    }

    @Override
    public List<Post> findPosts(ZonedDateTime start, ZonedDateTime end, String room) {
        Criteria criteria = Criteria.where("time").gte(start.toInstant()).lt(end.toInstant()).and("room")
                .is(roomOrDefault(room));
        Query query = new Query().addCriteria(criteria)
                .with(PageRequest.of(0, roomHistorySize, Sort.Direction.DESC, "time", "gatewayPostId.postId"));
        List<Post> result = mongoTemplate.find(query, Post.class, COLLECTION_NAME);
        return result;
    }

    @Override
    public List<Post> findPostsReverse(ZonedDateTime start, ZonedDateTime end, String room) {
        Criteria criteria = Criteria.where("time").gte(start.toInstant()).lt(end.toInstant()).and("room")
                .is(roomOrDefault(room));
        Query query = new Query().addCriteria(criteria)
                .with(PageRequest.of(0, roomHistorySize, Sort.Direction.ASC, "time", "gatewayPostId.postId"));
        List<Post> result = mongoTemplate.find(query, Post.class, COLLECTION_NAME);
        return result;
    }

    @Override
    public long countPosts(ZonedDateTime start, ZonedDateTime end, String room) {
        Criteria criteria = Criteria.where("time").gte(start.toInstant()).lt(end.toInstant()).and("room")
                .is(roomOrDefault(room));
        Query query = new Query().addCriteria(criteria)
                .with(PageRequest.of(0, roomHistorySize, Sort.Direction.ASC, "time", "gatewayPostId.postId"));
        return mongoTemplate.count(query, COLLECTION_NAME);
    }

    @Override
    public void save(Post post) {
        post.setRoom(roomOrDefault(post.getRoom()));
        mongoTemplate.save(post, COLLECTION_NAME);
    }

    @Override
    public Post findOne(String id) {
        return mongoTemplate.findById(id, Post.class, COLLECTION_NAME);
    }

    @Override
    public boolean existsById(String id) {
        Query query = new Query().addCriteria(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, COLLECTION_NAME);
    }

    private static final String COLLECTION_NAME = "post";

    @Override
    public Post findOne(String room, ZonedDateTime start, ZonedDateTime end, int indice) {
        Criteria criteria = Criteria.where("time").gte(start.toInstant()).lt(end.toInstant()).and("room")
                .is(roomOrDefault(room));
        Query query = new Query().addCriteria(criteria)
                .with(PageRequest.of(0, 9, Sort.Direction.ASC, "time", "gatewayPostId.postId"));
        List<Post> posts = mongoTemplate.find(query, Post.class, COLLECTION_NAME);
        if (posts.isEmpty()) {
            return null;
        }
        int clampedIndice = Math.min(posts.size(), Math.max(1, indice));
        return posts.get(clampedIndice - 1);
    }

    @Override
    public List<Post> search(PostSearchRQ rq) {
        Query query;
        int nbCriteria = 0;
        PostSearchRQ.Sort sort = rq.getSort();
        if (StringUtils.isNotBlank(rq.getMessageFilter())) {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(rq.getMessageFilter());
            TextQuery textQuery = TextQuery.queryText(criteria);
            if (null == sort || PostSearchRQ.Sort.RELEVANCE == sort) {
                sort = PostSearchRQ.Sort.RELEVANCE;
                textQuery.sortByScore();
            }
            query = textQuery;
            ++nbCriteria;
        } else {
            if (null == sort) {
                sort = PostSearchRQ.Sort.TIME_DESC;
            }
            query = new Query();
        }
        if (StringUtils.isNotBlank(rq.getNicknameFilter())) {
            query = query.addCriteria(Criteria.where("nickname").is(rq.getNicknameFilter()));
            ++nbCriteria;
        }
        if (StringUtils.isNotBlank(rq.getRoomFilter())) {
            query = query.addCriteria(Criteria.where("room").is(rq.getRoomFilter()));
            ++nbCriteria;
        }
        ZonedDateTime sinceDate = rq.getSinceDate();
        ZonedDateTime untilDate = rq.getUntilDate();
        if (null != sinceDate && null != untilDate) {
            query.addCriteria(Criteria.where("time").gte(sinceDate.toInstant()).lte(untilDate.toInstant()));
            ++nbCriteria;
        } else {
            if (null != sinceDate) {
                query.addCriteria(Criteria.where("time").gte(sinceDate.toInstant()));
                ++nbCriteria;
            }
            if (null != untilDate) {
                query.addCriteria(Criteria.where("time").lte(untilDate.toInstant()));
                ++nbCriteria;
            }
        }
        if (nbCriteria > 0) {
            switch(sort) {
                case RELEVANCE:
                    query.with(PageRequest.of(rq.getPage(), rq.getPageSize()));
                    break;
                case TIME_ASC:
                    query.with(PageRequest.of(rq.getPage(), rq.getPageSize(), Sort.Direction.ASC, "time"));
                    break;
                case TIME_DESC:
                    query.with(PageRequest.of(rq.getPage(), rq.getPageSize(), Sort.Direction.DESC, "time"));
                    break;
            }
            return mongoTemplate.find(query, Post.class, COLLECTION_NAME);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean existsByGatewayPostId(GatewayPostId gpid) {
        Query query = new Query().addCriteria(Criteria.where("gatewayPostId.gateway").is(gpid.getGateway())
                .and("gatewayPostId.postId").is(gpid.getPostId()));
        return mongoTemplate.exists(query, COLLECTION_NAME);
    }

    @Override
    public Post findOneByGatewayId(GatewayPostId gpid) {
        Query query = new Query().addCriteria(Criteria.where("gatewayPostId.gateway").is(gpid.getGateway())
                .and("gatewayPostId.postId").is(gpid.getPostId()));
        return mongoTemplate.findOne(query, Post.class, COLLECTION_NAME);
    }

}
