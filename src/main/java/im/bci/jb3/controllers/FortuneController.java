package im.bci.jb3.controllers;

import im.bci.jb3.data.Fortune;
import im.bci.jb3.data.FortuneRepository;
import im.bci.jb3.frontend.PostSearchRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/fortune")
public class FortuneController {

    @Autowired
    private FortuneRepository fortuneRepository;

    @RequestMapping(path="", method = RequestMethod.GET)
    public String index(PostSearchRQ rq, Model model) {
        model.addAttribute("rq", rq);
        model.addAttribute("fortunes", fortuneRepository.search(rq));
        return "fortune/index";
    }

    @RequestMapping(path="/{fortuneId}", method = RequestMethod.GET)
    public String view(@PathVariable("fortuneId") String fortuneId, Model model) {
        Fortune fortune = fortuneRepository.findOne(fortuneId);
        model.addAttribute("fortune", fortune);
        return "fortune/view";
    }
}
