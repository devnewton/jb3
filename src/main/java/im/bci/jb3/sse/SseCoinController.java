package im.bci.jb3.sse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.logic.CleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;

@Component
@Controller
public class SseCoinController {

    @Autowired
    private SseCoinService service;

    @GetMapping("/ssecoin/posts/stream")
    public ResponseEntity<SseEmitter> posts(@RequestParam(value = "rooms", required = true) String[] rooms) {
        SseMoule moule = service.addMoule(rooms);
        service.emitPosts(moule);
        return ResponseEntity.ok().header("X-Accel-Buffering", "no").body(moule.emiter);
    }
    
    @PostMapping(value = "/ssecoin/presence")
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void presence(String nickname, String status) {
        Presence presence = new Presence();
        presence.setNickname(CleanUtils.truncateAndCleanNickname(nickname));
        presence.setStatus(CleanUtils.truncateAndCleanStatus(status));
        service.broadcastPresence(presence);
    }

    @PostMapping(value = "/ssecoin/posts/add")
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void post(@RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "message") String message,
            @RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "auth", required = false) String auth) {
        service.post(nickname, message, room, auth);
    }

    @GetMapping(path = "/ssecoin/posts/find/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Post> findById(@PathVariable String messageId) {
        Post post = service.findOne(messageId);
        if(null != post) {
            return new ResponseEntity<>(post, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
  
}
