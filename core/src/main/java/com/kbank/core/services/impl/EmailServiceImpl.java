package com.kbank.core.services.impl;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.kbank.core.services.EmailService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.services.ResourceResolverService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;

@Component(service = EmailService.class, immediate = true)
public class EmailServiceImpl implements EmailService {

    private static Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Reference
    private ResourceResolverService resourceResolverService;

    @Reference
    private MessageGatewayService messageGatewayService;

    @Override
    public void sendEmail(String to, String subject, Map<String, String> placeholders, String templatePath) {
        try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            Resource templateResource = resourceResolver.getResource(templatePath);
            if (templateResource != null) {
                InputStream templateStream = templateResource.adaptTo(InputStream.class);
                if (templateStream != null) {
                    String templateContent = IOUtils.toString(templateStream, StandardCharsets.UTF_8);
                    String populatedContent = populateTemplate(templateContent, placeholders);

                    // Send email
                    HtmlEmail email = new HtmlEmail();
                    email.setFrom("no-reply@kbank.com");
                    email.addTo(to);
                    email.setSubject(subject);
                    email.setHtmlMsg(populatedContent);
                    //email.send();
                    MessageGateway<Email> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
                    if (messageGateway != null) {
                        log.debug("sending out email");
                        messageGateway.send((Email) email);
                    } else {
                        log.error("The message gateway could not be retrieved.");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error sending email {}", e.getMessage());
        }
    }

    private String populateTemplate(String template, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }
}
