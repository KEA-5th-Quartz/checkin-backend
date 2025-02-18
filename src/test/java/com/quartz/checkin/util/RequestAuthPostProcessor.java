package com.quartz.checkin.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class RequestAuthPostProcessor {

    private static Properties properties;

    static  {
        properties = new Properties();
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application-secret.yml"));
        properties = yamlFactory.getObject();
    }

    public static String getProperty(String key){
        return properties.getProperty(key);
    }

    public static RequestPostProcessor authenticatedAsUser(MockMvc mockMvc) throws Exception {
        String userLoginRequest = getProperty("test.login.user");

        return authenticated(mockMvc, userLoginRequest);
    }

    public static RequestPostProcessor authenticatedAsManager(MockMvc mockMvc) throws Exception {
        String managerLoginRequest = getProperty("test.login.manager");

        return authenticated(mockMvc, managerLoginRequest);
    }


    public static RequestPostProcessor authenticatedAsAdmin(MockMvc mockMvc, String loginRequest) throws Exception {
        String adminLoginRequest = getProperty("test.login.admin");

        return authenticated(mockMvc, adminLoginRequest);
    }

    public static RequestPostProcessor authenticatedCustom(MockMvc mockMvc, String username, String password) throws Exception {
        String loginRequest = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return authenticated(mockMvc, loginRequest);
    }


    private static RequestPostProcessor authenticated(MockMvc mockMvc, String loginRequest)
            throws Exception {

        MvcResult mvcResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(responseContent);

        String accessToken  = jsonNode.path("data").path("accessToken").asText();

        String refreshToken = mvcResult.getResponse().getCookie("Refresh").getValue();
        Cookie refreshCookie =  new Cookie("Refresh", refreshToken);

        return request -> {
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.setCookies(refreshCookie);
            return request;
        };
    }
}
