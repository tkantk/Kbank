package com.kbank.core.schedulers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kbank.core.models.User;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import com.kbank.core.services.ResourceResolverService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = NewsLetterGeneratorScheduledTask.class, immediate = true)
@Designate(ocd = NewsLetterGeneratorScheduledTask.Config.class)
public class NewsLetterGeneratorScheduledTask implements Runnable {

    @ObjectClassDefinition(name = "News Letter Generator Scheduler Job", description = "Scheduler job to demonstrate enable/disable functionality")
    public @interface Config {
        boolean enabled() default false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(NewsLetterGeneratorScheduledTask.class);
    private static final String JOB_NAME = "com.kbank.core.schedulers.NewsLetterGeneratorScheduledTask";

    private static final String JOB_EXPRESSION = "0 0 0 1/1 * ? *";
    private static final String USER_PATH = "/home/users/kbank";
    private boolean enabled;

    private static final String CLIENT_ID = "scoe-hackathon-app";
    private static final String CLIENT_SECRET = "s8e-0KET_sozfAH-x3qp7Mn1agaJzvyi2IOg";
    private static final String TOKEN_CODE = "eyJhbGciOiJSUzI1NiIsIng1dSI6Imltc19uYTEtc3RnMS1rZXktcGFjLTEuY2VyIiwia2lkIjoiaW1zX25hMS1zdGcxLWtleS1wYWMtMSIsIml0dCI6InBhYyJ9.eyJpZCI6InNjb2UtaGFja2F0aG9uLWFwcF9zdGciLCJ0eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwiY2xpZW50X2lkIjoic2NvZS1oYWNrYXRob24tYXBwIiwidXNlcl9pZCI6InNjb2UtaGFja2F0aG9uLWFwcEBBZG9iZUlEIiwiYXMiOiJpbXMtbmExLXN0ZzEiLCJvdG8iOmZhbHNlLCJjcmVhdGVkX2F0IjoiMTY5MTA0MTcyMDYwNCIsInNjb3BlIjoic3lzdGVtIn0.ZRUy3wSFFOdimL_G5TXPLfPtfnVA06kn2RPEl86Vo0fhEZuSzV7YskMhrZueGnwIA_1mC2V_67g4szWALiIoqVhw-ezNNHDu6O-MlG6fzIKchpdud01jXM5rpu13pjk1R1eFa2jwxiglW_Kx0MpRQfpHhRjQb3gk4uOWoynt8kJjC9fDyNES51khuE91AbkBtyRwva8wE_aaRFyFNcghNkT0T0ptQT0HyMjpfXWlO01cjJRdFPUp6e6trPJpImIEjqJZ-258wBW8229yHWHbWNrLTrnYo-iwY9Fs-M9yZFvrQoFc_kiS8529muYoIL1igkx4DKx9dsUbqKe7d7aW8g";

    private static final Map<String, String[]> userToSegmentMap = new HashMap<>();
    // Static block to populate the map
    static {
        userToSegmentMap.put("tushark@adobe.com", new String[]{"4ca4b377-d89f-4889-b376-2a7c5f8cf067"});
        userToSegmentMap.put("sunilkumars@adobe.com", new String[]{"4ca4b377-d89f-4889-b376-2a7c5f8cf067"});
        userToSegmentMap.put("abhishri@adobe.com", new String[]{"4ca4b377-d89f-4889-b376-2a7c5f8cf067"});
    }

    @Reference
    private Scheduler scheduler;

    @Reference
    private AEPUtilService aepUtilService;

    @Reference
    private ResourceResolverService resourceResolverService;

    @Reference
    private AIGeneratedPersonalizedDataService aiGeneratedPersonalizedDataService;

    private SlingHttpServletRequest request;

    @Activate
    protected void activate(Config config) {
        this.enabled = config.enabled();
        if (enabled) {
            ScheduleOptions options = scheduler.EXPR("0 0/1 * * * ?"); // Run every minute
            options.name(JOB_NAME);
            options.canRunConcurrently(false);
            scheduler.schedule(this, options);
            LOG.info("NewsLetterGeneratorScheduledTask scheduled to run every minute");
        } else {
            LOG.info("NewsLetterGeneratorScheduledTask is disabled");
        }
    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(JOB_NAME);
        LOG.info("NewsLetterGeneratorScheduledTask unscheduled");
    }

    @Override
    public void run() {
        LOG.info("NewsLetterGeneratorScheduledTask is running");
        try {
            List<User> users = getUsers();
            final String token = aepUtilService.getLoginToken(CLIENT_ID, CLIENT_SECRET, TOKEN_CODE);
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("x-api-key", "scoe-hackathon-app");
            headers.put("x-gw-ims-org-id", "154340995B76EEF60A494007@AdobeOrg");
            for (User user : users) {
                Set<String> topicSet = new HashSet<>();
                String[] segments = getUserSegmentID(user.getEmail(), headers);
                if (segments != null) {
                    for (String segment : segments) {
                        LOG.info("User: {}, Segment: {}", user, segment);
                        // Get topics from AEP
                        final String topicData = aepUtilService.getTopicDataFromSegment(segment, headers);
                        LOG.info("Topic Data: {}", topicData);
                        // Parse the topic data
                        Set<String> newTopics = Arrays.stream(topicData.split(",\\s*"))
                                .collect(Collectors.toSet());
                        topicSet.addAll(newTopics);
                    }
                }
                JsonObject response  = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(String.join(", ", topicSet), "email", user.getEmail(), user.getName(), request);
                LOG.info("Response: {}", response);
            }
            LOG.info("Token: {}", token);

        } catch (IOException | URISyntaxException | InterruptedException | LoginException | IllegalBlockSizeException |
                 NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 RepositoryException e) {
            throw new RuntimeException(e);
        }
    }



    public void runOnDemand(SlingHttpServletRequest request) {
        LOG.info("NewsLetterGeneratorScheduledTask is running on demand");
        this.request = request;
        run();
    }

    private List<User> getUsers() {
        List<User> users = new ArrayList<>();
        try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            Resource userRoot = resourceResolver.getResource(USER_PATH);
            if (userRoot != null) {
                Iterator<Resource> userResources = userRoot.listChildren();
                while (userResources.hasNext()) {
                    Resource userResource = userResources.next();
                    Resource profileResource = userResource.getChild("profile");
                    if (profileResource != null) {
                        User user = new User();
                        String email = profileResource.getValueMap().get("email", String.class);
                        String name = profileResource.getValueMap().get("givenName", String.class) + " " + profileResource.getValueMap().get("familyName", String.class);
                        if (email != null) {
                            user.setEmail(email);
                        }
                        user.setName(name);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting users", e);
        }
        // Get users from the repository
        return users;
    }

    private String[] getUserSegmentID(String email, Map<String, String> headers) throws IOException, URISyntaxException, InterruptedException {
        // Get user segment ID from AEP
        return aepUtilService.getSegmentData(email, headers);
    }

}
