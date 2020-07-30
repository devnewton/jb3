package im.bci.jb3.handlebars;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.PostConstruct;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.data.PostRepository;
import im.bci.jb3.bouchot.logic.Norloge;
import java.time.ZonedDateTime;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;

@Component
@Scope("thread")
public class FormatMessageHelper implements Helper<Post> {

    @Autowired
    private PostRepository postRepository;

    public static class Console {

        public void log(String text) {
            LogFactory.getLog(this.getClass()).info(text);
        }
    }

    public static class NorlogeFormatter {

        public String format(Post post) {
            return Norloge.norlogePrintFormatter.format(post.getTime());
        }
    }

    public static class ParseOptions {

        private PostRepository postStore;
        private NorlogeFormatter norlogeFormatter;

        public PostRepository getPostStore() {
            return postStore;
        }

        public void setPostStore(PostRepository postStore) {
            this.postStore = postStore;
        }

        public NorlogeFormatter getNorlogeFormatter() {
            return norlogeFormatter;
        }

        public void setNorlogeFormatter(NorlogeFormatter norlogeFormatter) {
            this.norlogeFormatter = norlogeFormatter;
        }
    }

    private ParseOptions parseOptions;
    private Invocable invocable;
    private Object postToHtml;

    @PostConstruct
    public void setup() {
        parseOptions = new ParseOptions();
        parseOptions.setPostStore(postRepository);
        parseOptions.setNorlogeFormatter(new NorlogeFormatter());

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        engine.put("console", new Console());
        InputStreamReader postToHtmlSource = new InputStreamReader(getClass().getResourceAsStream("/static/assets/common/post-to-html.js"));
        try {
            engine.eval(postToHtmlSource);
            postToHtml = engine.eval("jb3_post_to_html");
            invocable = (Invocable) engine;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CharSequence apply(Post post, Options options) throws IOException {
        String formattedMessage = post.getCleanedMessage();
        try {
            formattedMessage = invocable.invokeMethod(postToHtml, "parse", formattedMessage, parseOptions).toString();
        } catch (Exception ex) {
            LogFactory.getLog(this.getClass()).fatal("pegjs error", ex);
        }
        return new Handlebars.SafeString(formattedMessage);
    }

}
