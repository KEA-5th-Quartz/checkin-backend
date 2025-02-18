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
    public static final String USER_LOGIN_REQUEST;
    public static final String MANAGER_LOGIN_REQUEST;
    public static final String ADMIN_LOGIN_REQUEST;


    static  {
        properties = new Properties();
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application-secret.yml"));
        properties = yamlFactory.getObject();

        USER_LOGIN_REQUEST = getProperty("test.login.user");
        MANAGER_LOGIN_REQUEST = getProperty("test.login.manager");
        ADMIN_LOGIN_REQUEST = getProperty("test.login.admin");
    }

    private static String getProperty(String key){
        return properties.getProperty(key);
    }

    public static RequestPostProcessor authenticatedAsUser(MockMvc mockMvc) throws Exception {
        return authenticated(mockMvc, USER_LOGIN_REQUEST);
    }

    public static RequestPostProcessor authenticatedAsManager(MockMvc mockMvc) throws Exception {
        return authenticated(mockMvc, MANAGER_LOGIN_REQUEST);
    }


    public static RequestPostProcessor authenticatedAsAdmin(MockMvc mockMvc) throws Exception {
        return authenticated(mockMvc, ADMIN_LOGIN_REQUEST);
    }

    public static RequestPostProcessor setAccessToken(String accessToken) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + accessToken);
            return  request;
        };
    }

    public static RequestPostProcessor setRefreshToken(String refreshToken) {
        Cookie refreshCookie =  new Cookie("Refresh", refreshToken);
        return request -> {
            request.setCookies(refreshCookie);
            return  request;
        };
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

        return request -> {
            request = setAccessToken(accessToken).postProcessRequest(request);
            request = setRefreshToken(refreshToken).postProcessRequest(request);
            return request;
        };
    }
}
