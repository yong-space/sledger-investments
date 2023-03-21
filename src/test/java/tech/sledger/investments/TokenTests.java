package tech.sledger.investments;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.sledger.investments.model.Config;
import tech.sledger.investments.repository.ConfigRepo;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TokenTests {
    @Autowired
    public MockMvc mvc;
    @MockBean
    public ConfigRepo configRepo;

    @PostConstruct
    public void init() {
        when(configRepo.findById(any())).thenReturn(Optional.of(Config.builder().key("").value("").build()));
    }

    @Test
    public void authorize() throws Exception {
        mvc.perform(get("/api/authorize"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    public void tokenWithError() throws Exception {
        mvc.perform(get("/api/token?code=x&error=hello&error_description=again"))
            .andDo(result -> assertEquals("hello: again", result.getResponse().getContentAsString()));
    }

    @Test
    public void tokenSuccess() throws Exception {
        mvc.perform(get("/api/token?code=904dfd4b-ee58-4bd4-ba63-239bf6ed589b#/lst/1679407726745"))
            .andExpect(status().is3xxRedirection());
    }
}
