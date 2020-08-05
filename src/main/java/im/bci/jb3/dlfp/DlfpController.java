package im.bci.jb3.dlfp;

import java.util.Collections;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/dlfp")
public class DlfpController {

    @Value("${jb3.dlfp.oauth.client_id:}")
    private String clientId;

    @Value("${jb3.dlfp.oauth.client_secret:}")
    private String clientSecret;

    @RequestMapping(path = "/connect", method = RequestMethod.GET)
    public RedirectView connect(RedirectAttributes attributes) {
        attributes.addAttribute("client_id", clientId);
        attributes.addAttribute("redirect_uri", buildRedirectURI());
        attributes.addAttribute("response_type", "code");
        attributes.addAttribute("scope", "account board");
        return new RedirectView("https://linuxfr.org/api/oauth/authorize");
    }

    @RequestMapping(path = "/connected", method = RequestMethod.GET)
    public String connected(Model model, @RequestParam String code) {
        try {
            OauthToken token = retrieveToken(code);
            model.addAttribute("token", token);
            model.addAttribute("login", retrieveLogin(token));
        } catch (Exception ex) {
            LogFactory.getLog(this.getClass()).error("dlfp oauth error", ex);
        }
        return "dlfp/connected";
    }

    private OauthToken retrieveToken(String code) throws RestClientException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.add("User-Agent", "jb3");
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("redirect_uri", buildRedirectURI());
        HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<DlfpOauthToken> response
                = restTemplate.exchange("https://linuxfr.org/api/oauth/token", HttpMethod.POST,
                        formEntity, DlfpOauthToken.class);
        return new OauthToken(response.getBody());
    }

    private String retrieveLogin(OauthToken token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.add("User-Agent", "jb3");
        HttpEntity<?> entity = new HttpEntity<>(headers);
        DlfpMe me = restTemplate.exchange("https://linuxfr.org/api/v1/me?bearer_token={bearer_token}", HttpMethod.GET, entity, DlfpMe.class, Collections.singletonMap("bearer_token", token.getAccess_token())).getBody();
        return me.getLogin();
    }

    @ResponseBody
    @RequestMapping(path = "/refresh-token", method = RequestMethod.POST)
    public OauthToken refreshToken(@RequestParam String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.add("User-Agent", "jb3");

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("refresh_token", token);
            requestBody.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<DlfpOauthToken> response
                    = restTemplate.exchange("https://linuxfr.org/api/oauth/token", HttpMethod.POST,
                            formEntity, DlfpOauthToken.class);
            return new OauthToken(response.getBody());
        } catch (Exception ex) {
            LogFactory.getLog(this.getClass()).error("dlfp oauth error", ex);
            throw ex;
        }

    }

    private String buildRedirectURI() {
        return ServletUriComponentsBuilder.fromCurrentRequest().replacePath("/dlfp/connected").replaceQuery("").build().toString();
    }

}
