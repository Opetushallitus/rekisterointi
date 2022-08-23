package fi.vm.sade.koodisto.koodistoui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KoodistoUiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc
                .perform(get("/"))
                .andExpectAll(
                        status().isFound(),
                        header().string(HttpHeaders.LOCATION, containsString("/cas/login"))
                );
    }

    @Test
    void healthCheckCanBeAccessedAnonymously() throws Exception {
        mockMvc
                .perform(get("/health"))
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser
    void rootRedirectsToIndex() throws Exception {
        mockMvc
                .perform(get("/"))
                .andExpectAll(
                        status().isOk(),
                        forwardedUrl("/index.html")
                );
    }

    @Test
    @WithMockUser
    void randomPathRedirectsToIndex() throws Exception {
        mockMvc
                .perform(get("/this/should/be/handled/by/frontend"))
                .andExpectAll(
                        status().isOk(),
                        forwardedUrl("/index.html")
                );
    }
}
