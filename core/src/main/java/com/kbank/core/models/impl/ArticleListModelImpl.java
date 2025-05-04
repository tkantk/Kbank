/*
 *  Copyright 2019 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kbank.core.models.impl;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kbank.core.models.ArticleListModel;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import com.kbank.core.services.ResourceResolverService;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {ArticleListModel.class},
        resourceType = {ArticleListModelImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ArticleListModelImpl implements ArticleListModel {
    private static final Logger log = LoggerFactory.getLogger(ArticleListModelImpl.class);

    protected static final String RESOURCE_TYPE = "kbank/components/articlecard";

    @ValueMapValue
    private String configurationType;

    @Self
    @Required
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    protected ComponentContext componentContext;

    @OSGiService
    private ResourceResolverService resourceResolverService;

    @OSGiService
    private AEPUtilService aepUtilService;

    @OSGiService
    private AIGeneratedPersonalizedDataService aiGeneratedPersonalizedDataService;

    private List<ListItem> articleListItems;

    @Override
    public final Collection<ListItem> getListItems() {
        if (articleListItems == null) {
            JsonArray coreList = getAIDataFromCurrentUserProfile();
            if (coreList == null) {
                log.warn("Could not locate the AEM WCM Core Components List SlingModel via this component's ResourceSuperType. Returning an empty list.");
                articleListItems = Collections.EMPTY_LIST;
            } else {
                articleListItems = buildArticleListItems(coreList);
            }
        }

        return ImmutableList.copyOf(articleListItems);
    }

    private List<ListItem> buildArticleListItems(JsonArray coreList) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ArticleItem>>() {}.getType();
        return gson.fromJson(coreList, listType);
    }

    private JsonArray getAIDataFromCurrentUserProfile() {
        JsonArray items = new JsonArray();
        try(ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            Session session = resourceResolver.adaptTo(Session.class);
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            Session currentUserSession = request.getResourceResolver().adaptTo(Session.class);

            if (session != null) {
                User currentUser = (User) userManager.getAuthorizable(currentUserSession.getUserID());
                if (currentUser != null && !"anonymous".equals(currentUser.getID()) &&
                        session.getNode(currentUser.getPath()).hasNode("profile") &&
                        currentUser.hasProperty("profile/email"))  {
                    //JsonObject data = JsonParser.parseString(currentUser.getProperty("profile/aidata")[0].getString()).getAsJsonObject();
                    assert aepUtilService != null;
                    assert configurationType != null;
                    //if (configurationType != null && configurationType.equalsIgnoreCase("currentuserprofileview")) {
                        String interests = aepUtilService.getInterestsFromAEP(currentUser.getProperty("profile/email")[0].getString());
                        assert aiGeneratedPersonalizedDataService != null;
                        JsonObject data = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(interests, "articlelist", null, null, request);
                        items = data.getAsJsonArray("data");
                    /*} else {
                        items = JsonParser.parseString(currentUser.getProperty("profile/aidata")[0].getString()).getAsJsonArray();
                    }*/
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error getting AI data from user profile {}", e.getMessage());
        }
        return items;
    }

    @Override
    public final boolean isEmpty() {
        return getListItems().isEmpty();
    }

    @Override
    public String getId() {
        Resource imageListResource = this.request.getResource();
        return ComponentUtils.getId(imageListResource, this.currentPage, this.componentContext);
    }

    @Override
    public ComponentData getData() {
        Resource imageListResource = this.request.getResource();
        if (ComponentUtils.isDataLayerEnabled(imageListResource)) {
            return DataLayerBuilder.forComponent()
                .withId(() -> getId())
                .withType(() -> RESOURCE_TYPE)
                .build();
        }
        return null;
    }

    public class ArticleItem implements ListItem {
        private String url;
        private String author;
        private String source;
        private String urlToImage;
        private String publishedAt;
        private String heading;
        private String article;

        public ArticleItem(String urlToImage, String heading, String article, String url, String publishedAt, String author) {
            this.urlToImage = urlToImage;
            this.heading = heading;
            this.article = article;
            this.url = url;
            this.publishedAt = publishedAt;
            this.author = author;
        }

        @Override
        public String getImage() {
            // Assuming that you have a method to get a Resource from a URL
            // This needs to be implemented based on your specific requirements
            return urlToImage;
        }

        @Override
        public String getTitle() {
            return heading;
        }

        @Override
        public String getDescription() {
            return article.substring(0, 50) + "...";
        }

        @Override
        public String getFullDescription() {
            return article;
        }

        @Override
        public String getURL() {
            return url;
        }

        @Override
        public String getPublishedDate() {
            return convertDate(publishedAt);
        }

        @Override
        public String getAuthor() {
            return author;
        }

        private String convertDate(String dateString) {
            // Define the input date format
            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;

            // Define the output date format
            DateTimeFormatter outputFormatter = new DateTimeFormatterBuilder()
                    .appendPattern("MMM")
                    .appendLiteral(' ')
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .toFormatter(Locale.ENGLISH)
                    .withLocale(Locale.ENGLISH);

            // Parse the input date string to a ZonedDateTime object
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, inputFormatter);

            // Format the ZonedDateTime object to the desired output format
            return outputFormatter.format(zonedDateTime).toUpperCase();
            }
        }

    }
