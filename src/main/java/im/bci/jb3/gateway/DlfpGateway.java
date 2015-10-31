package im.bci.jb3.gateway;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
@Component
public class DlfpGateway extends AbstractBouchotGateway {

    private static BouchotConfig createConf() {
        BouchotConfig conf = new BouchotConfig();
        conf.setRoom("dlfp");
        conf.setGetUrl("https://linuxfr.org/board/index.xml");
        conf.setPostUrl("https://linuxfr.org/board");
        conf.setCookieName("linuxfr.org_session");
        conf.setReferrer("https://linuxfr.org/board");
        conf.setMessageContentParameterName("board[message]");
        conf.setUsingCrapCertificate(true);
        return conf;
    }

    public DlfpGateway() {
        super(createConf());
    }

    @Scheduled(cron = "0/30 * * * * *")
    public void scheduledPostsImport() {
        importPosts();
    }
}
