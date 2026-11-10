package com.bloodbridge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Forwards single-segment client-side routes (/login, /donor, ...) to the React app. */
@Controller
public class SpaController {
    @GetMapping(value = {"/", "/{path:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
