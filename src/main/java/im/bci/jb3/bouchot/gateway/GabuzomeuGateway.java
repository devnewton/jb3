package im.bci.jb3.bouchot.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
@ConditionalOnExpression("'${jb3.defaults.room:}' != 'gabuzomeu'")
@Component
public class GabuzomeuGateway extends AbstractSSECoinGateway {
    
    private static Jb3BouchotConfig createConf() {
        Jb3BouchotConfig config = new Jb3BouchotConfig();
        config.setLocalRoom("gabuzomeu");
        config.setRemoteRoom("gabuzomeu");
        config.setUrl("https://jb3.plop.cc");
        return config;
    }

    public GabuzomeuGateway() {
        super(createConf());
    }

}
