package com.kbank.core.servlets;

import com.day.crx.security.token.TokenUtil;
import com.kbank.core.services.ResourceResolverService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import org.json.JSONObject;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "kbank/components/login",
        extensions = "json",
        methods = HttpConstants.METHOD_POST
)
public class LoginUserServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LoginUserServlet.class);
    private static final long serialVersionUID = 1L;

    @Reference
    private transient  org.apache.sling.jcr.api.SlingRepository repository;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String username = request.getParameter("j_username");
        String password = request.getParameter("j_password");
        // Create the fully qualified URL for /system/sling/login/j_security_check
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        String loginUrl = scheme + "://" + serverName + ":" + serverPort + contextPath + "/system/sling/login/j_security_check";
        // Create the POST request to /system/sling/login/j_security_check
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(loginUrl);
            Map<String, String> params = new HashMap<>();
            params.put("j_username", username);
            params.put("j_password", password);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(param.getKey()).append('=').append(param.getValue());
            }

            StringEntity entity = new StringEntity(postData.toString(), StandardCharsets.UTF_8);
            entity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Successfully logged in, handle session and redirect
                    String redirectUrl = request.getParameter("redirectUrl");
                    if (redirectUrl == null || redirectUrl.isEmpty()) {
                        redirectUrl = "/content/kbank/us/en/personal.html"; // Default redirect URL
                    }
                    response.sendRedirect(redirectUrl);
                } else {
                    response.getWriter().write("Login failed: " + EntityUtils.toString(httpResponse.getEntity()));
                    response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
                }
            }
        } catch (Exception e) {
            LOG.error("Authentication error: ", e);
            sendErrorResponse(response, "Authentication failed.");
        }
    }

    private void sendErrorResponse(SlingHttpServletResponse response, String message) throws IOException {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", "error");
        jsonResponse.put("message", message);
        response.getWriter().write(jsonResponse.toString());
    }

    private boolean checkLoginStatus(SlingHttpServletRequest request) {
        // Check if the user is logged in by verifying the session
        return request.getUserPrincipal() != null;
    }
}
