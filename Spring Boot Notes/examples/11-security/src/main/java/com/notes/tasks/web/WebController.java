package com.notes.tasks.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** A tiny browser page, to show session login and CSRF protection. */
@RestController
public class WebController {

    @GetMapping(value = "/", produces = "text/html")
    public String home(Authentication auth, CsrfToken csrf) {
        return """
                <p>Hello, %s</p>
                <form method="post" action="/notes">
                  <input name="text">
                  <input type="hidden" name="%s" value="%s">
                  <button>Save note</button>
                </form>""".formatted(HtmlUtils.htmlEscape(auth.getName()), csrf.getParameterName(), csrf.getToken());
    }

    @PostMapping(value = "/notes", produces = "text/plain")
    public String saveNote(@RequestParam String text, Authentication auth) {
        return "saved for " + auth.getName() + ": " + HtmlUtils.htmlEscape(text);   // escape: no XSS
    }
}
