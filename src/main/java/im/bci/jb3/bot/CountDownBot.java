package im.bci.jb3.bot;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import im.bci.jb3.bouchot.data.Post;
import im.bci.jb3.bouchot.logic.Tribune;

import static java.util.Arrays.asList;


@Component
public class CountDownBot implements Bot {

    private static int MAX_SIZE = 2;
    private static String[] ADMINS = {"devnewton", "dave"};
    private List<String> allowedNames = asList(ADMINS);
    @Autowired
    private Tribune tribune;

    private static final String NAME = "europe";
    private ArrayList<CustomTimer> timers;
    private ScheduledExecutorService executor;

    private static final Pattern COMMAND_PATTERN = Pattern.compile(String.format("^\\/%s (?<command>\\w+) (p:)(?<period>\\d*) (i:)(?<interval>\\d*) (?<message>.*)$", NAME));
    private static final Pattern COMMAND_CLEAR_PATTERN = Pattern.compile(String.format("^\\/%s (?<command>\\w+)", NAME));


    @Override
    public void handle(Post post, UriComponentsBuilder uriBuilder) {
        try {
            if (tribune.isBotCall(post, NAME)) {
                if(!allowedNames.contains(post.getCleanedNickname())) {
                    LogFactory.getLog(this.getClass()).debug(String.format("%s not allowed for user '%s' for bot ", NAME, post.getCleanedNickname()));
                    return;
                }
                Matcher matcher = COMMAND_PATTERN.matcher(post.getMessage());
                if (matcher.matches()) {
                    String command = matcher.group("command");
                    String message = matcher.group("message");
                    String periodStr = matcher.group("period");
                    String intervalStr = matcher.group("interval");
                    switch (command) {
                        case "clear":
                            for (CustomTimer timer : timers) {
                                timer.cancel();
                            }
                            break;
                        case "add":
                            if (timers.size() <= MAX_SIZE) {
                                scheduleTask(post, command, message, periodStr, intervalStr);
                            }
                            break;
                        default:
                            LogFactory.getLog(this.getClass()).debug(String.format("unknown command '%s' for bot ", command, NAME));
                    }
                } else {
                    matcher = COMMAND_CLEAR_PATTERN.matcher(post.getMessage());
                    if (matcher.matches()) {
                        String command = matcher.group("command");
                        switch (command) {
                            case "clear":
                                for (CustomTimer timer : timers) {
                                    timer.cancel();
                                }
                                break;
                            default:
                                LogFactory.getLog(this.getClass()).debug(String.format("unknown command '%s' for bot ", command, NAME));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LogFactory.getLog(this.getClass()).error(String.format("%s bot error", NAME), ex);
        }
    }

    private void scheduleTask(Post post, String command, String message, String periodStr, String intervalStr) {
        Long interval= new Long(48*3600);

        if (!intervalStr.isEmpty()) {
            try {
                interval = Long.valueOf(intervalStr)*1000;
                if (interval < 10*60*60) {
                    interval = 10L*60*60;
                    LogFactory.getLog(this.getClass()).debug(
                            String.format("interval is too low, using default '%d' for bot %s", interval, NAME));

                }
            } catch (NumberFormatException ex) {
                LogFactory.getLog(this.getClass()).debug(
                  String.format("interval is not a number '%s' for bot ", intervalStr, NAME));
                tribune.post(NAME, String.format("invalid interval %s, using default", intervalStr), post.getRoom());
            }
        }
        long delay = 1000L;
        long period = 1000L * 60L * 60L * 24L;
        if (!periodStr.isEmpty()) {
            try {
                period = Long.valueOf(periodStr)*1000;
                if (period < 10*60*60) {
                    period = 10*60*60;
                    LogFactory.getLog(this.getClass()).debug(
                            String.format("period is too low, using default '%d' for bot %s", period, NAME));

                }
            } catch (NumberFormatException ex) {
                LogFactory.getLog(this.getClass()).debug(
                  String.format("period is not a number '%s' for bot %s", periodStr, NAME));
                tribune.post(NAME, String.format("invalid period %s, using default", periodStr), post.getRoom());
            }
        }
        CustomTimer timer = new CustomTimer(interval, period, "Timer" + command);
        timers.add(timer);
        TimerTask repeatedTask = new TimerTask() {
            public void run() {

                final long interval = timer.getInterval();
                if( interval > 0){
                    try {
                        tribune.post(NAME, String.format("Plus que %d secondes avant %s",interval, message), post.getRoom());
                    } catch (Exception ex) {
                        LogFactory.getLog(this.getClass()).error(String.format("%s bot error", NAME), ex);
                    }
                    timer.decrement();
                } else {
                    try {
                        tribune.post(NAME, String.format("C'est le grand moment de %s", message), post.getRoom());
                    } catch (Exception ex) {
                        LogFactory.getLog(this.getClass()).error(String.format("%s bot error", NAME), ex);
                    }
                    timer.cancel();
                    delete(timer);
                }
            }
        };

        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    private void delete(CustomTimer timer) {
        timers.remove(timer);
    }

    @PostConstruct
    void init() {
        executor = Executors.newSingleThreadScheduledExecutor();
        timers = new ArrayList<>();
    }

    @PreDestroy
    void clean() {
        executor.shutdown();
    }

}
class CustomTimer {

    String name;
    Timer timer;
    Long interval;
    Long period;

    public Long getPeriod() {
        return period;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public CustomTimer(Timer timer, Long interval, Long period, String name) {
        this.timer = timer;
        this.interval = interval;
        this.period = period;
        this.name = name;
    }

    public CustomTimer(Long interval, Long period, String name) {
        this.timer = new Timer(name);
        this.interval = interval;
        this.period = period;
        this.name = name;
    }

    void cancel() {
        timer.cancel();
    }

    public void scheduleAtFixedRate(TimerTask repeatedTask, long delay, long period) {
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    public void decrement() {
        interval = interval - period;
    }
}