package im.bci.jb3.archives;

import im.bci.jb3.bouchot.data.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.coincoin.PostSearchRQ;
import im.bci.jb3.coincoin.PostSearchResultMV;
import javax.servlet.http.HttpServletRequest;

import java.time.ZonedDateTime;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/archives")
public class ArchivesController {

	@Autowired
	private PostRepository postRepository;

	@RequestMapping(path = "", method = RequestMethod.GET)
	public String index(Model model, PostSearchRQ rq) {
		model.addAttribute("wro-group", "archives");
		model.addAttribute("rq", rq);
		model.addAttribute("postsMV", searchPosts(rq));
		return "archives/archives";
	}

	@RequestMapping(path = "/post/{postId}", method = RequestMethod.GET)
	public String post(Model model, HttpServletRequest request, @PathVariable String postId) {
		Post post = postRepository.findOne(postId);
		UriComponentsBuilder uri = ServletUriComponentsBuilder.fromRequest(request).replacePath("/archives");
		if (null != post) {
			ZonedDateTime time = post.getTime();
			ZonedDateTime since = time.minusHours(1);
			ZonedDateTime until = time.plusHours(1);
			uri.queryParam("roomFilter", post.getCleanedRoom()).queryParam("since", PostSearchRQ.DATE_FORMATTER.format(since))
					.queryParam("sinceTime", PostSearchRQ.TIME_FORMATTER.format(since))
					.queryParam("until", PostSearchRQ.DATE_FORMATTER.format(until))
					.queryParam("untilTime", PostSearchRQ.TIME_FORMATTER.format(until)).fragment(postId);
		}
		return "redirect:" + uri.build().encode().toString();
	}

	private PostSearchResultMV searchPosts(PostSearchRQ rq) {
		PostSearchResultMV mv = new PostSearchResultMV();
		if (null != rq) {
			mv.setPosts(postRepository.search(rq));
			mv.setHasPrevious(rq.getPage() > 0);
			mv.setHasNext(mv.getPosts().size() >= rq.getPageSize());
		}
		return mv;
	}
}
