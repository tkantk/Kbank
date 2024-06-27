package com.kbank.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Component(service = Servlet.class )
@SlingServletResourceTypes(
        resourceTypes = "kbank/components/logout",
        extensions = "json",
        methods = HttpConstants.METHOD_POST
)
public class LogoutServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LogoutServlet.class);
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        try {
            request.getSession().invalidate();
            clearCookies(request, response);
            // Redirect to a specific page after logout
            String redirectUrl = request.getParameter("redirectUrl");
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                redirectUrl = "/content/kbank/us/en/personal.html"; // Default redirect URL
            }
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            LOG.error("Logout error: ", e);
            sendErrorResponse(response, "Logout failed.");
        }
    }

    private void sendErrorResponse(SlingHttpServletResponse response, String message) throws IOException {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", "error");
        jsonResponse.put("message", message);
        response.getWriter().write(jsonResponse.toString());
    }

    private void clearCookies(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setMaxAge(0); // Invalidate the cookie
                cookie.setValue(""); // Clear the cookie value
                cookie.setPath("/"); // Ensure the cookie is cleared for the entire domain
                response.addCookie(cookie); // Add the cleared cookie back to the response
            }
        }
    }
}
