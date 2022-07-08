package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import tech.sledger.investments.client.SaxoClient;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class HomeService {
    private final SaxoClient saxoClient;

    @GetMapping("/")
    public String home(HttpServletResponse response) throws IOException {
        if (saxoClient.isNotAuthenticated()) {
            response.sendRedirect("/authorize");
            return null;
        } else {
            return "portfolio.html";
        }
    }
}
