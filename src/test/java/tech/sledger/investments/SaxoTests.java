package tech.sledger.investments;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SaxoTests extends BaseTest {
    @Test
    public void searchByQuery() throws Exception {
        mvc.perform(get("/search?query=x"))
            .andExpect(status().isOk());
    }

    @Test
    public void searchById() throws Exception {
        mvc.perform(get("/search?id=1,2,3"))
            .andExpect(status().isOk());
    }
}
