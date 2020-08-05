package im.bci.jb3.coincoin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class CoincoinController {

	@Value("${jb3.defaults.room}")
	private String defaultRoom;
        
        @Value("${jb3.defaults.rooms:}")
	private String defaultsRooms;

	@RequestMapping(path = "", method = RequestMethod.GET)
	public String index(Model model) {
		model.addAttribute("jb3DefaultRoom", defaultRoom);
                model.addAttribute("jb3DefaultsRooms", defaultsRooms);
		return "coincoin/coincoin";
	}

	@RequestMapping(path = "/rooms", method = RequestMethod.GET)
	public String rooms(Model model) {
                model.addAttribute("jb3DefaultsRooms", defaultsRooms);
		return "rooms/rooms";
	}

}
