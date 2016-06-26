package im.bci.jb3.logic;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 *
 * @author devnewton <devnewton@bci.im>
 */
public class CleanUtils {

	private static final int MAX_POST_LENGTH = 512;
	private static final int MAX_NICKNAME_LENGTH = 32;
	private static final int MAX_ROOM_LENGTH = 32;
	private static final Whitelist messageWhitelist = Whitelist.none().addTags("b", "i", "s", "u", "tt");

	public static String truncateMessage(String message) {
		return StringUtils.abbreviate(message, MAX_POST_LENGTH);
	}

	public static String truncateRoom(String room) {
		return StringUtils.abbreviate(room, MAX_ROOM_LENGTH);
	}

	public static String truncateNickname(String nickname) {
		return StringUtils.abbreviate(nickname, MAX_NICKNAME_LENGTH);
	}

	public static String cleanMessage(String message) {
		Document doc = Jsoup.parseBodyFragment(message);
		for (Element element : doc.body().children().select(":not(a,b,i,s,u,tt)")) {
			element.replaceWith(TextNode.createFromEncoded(element.toString(), null));
		}
		for (Element element : doc.body().children().select("a")) {
			element.replaceWith(TextNode.createFromEncoded(element.attr("href"), null));
		}
		Cleaner cleaner = new Cleaner(messageWhitelist);
		doc = cleaner.clean(doc);
		message = doc.body().html();
		return message;
	}

	public static String cleanRoom(String room) {
		if (null != room) {
			room = Jsoup.clean(room, Whitelist.none());
		}
		return room;
	}

	public static String cleanNickname(String nickname) {
		if (null != nickname) {
			nickname = Jsoup.clean(nickname, Whitelist.none());
		}
		return nickname;
	}

}
