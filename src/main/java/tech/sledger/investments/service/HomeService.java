package tech.sledger.investments.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeService {
    @GetMapping("/tx")
    public String tx() {
        return "index.html";
    }
}
