package com.kbank.core.models;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * Represents the Kbank Image List Component
 */
public interface EmailAprovalModel {

    /**
     * @return a collection of objects representing the items that compose the the list.
     */
    Collection<ListItem> getListItems() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * @return true if this component has no list items to display.
     */
    boolean isEmpty() throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /**
     * @return JSON data to populate the data layer
     */
    @JsonProperty("dataLayer")
    default ComponentData getData() {
        return null;
    }

    /**
     * @return String representing the unique identifier of the ImageList component on a page
     */
    String getId();

    String getName();

    String getCurrentDate();

    String getEmail();

    /**
     * Describes a item of the Image List.
     */
    interface ListItem {
        /**
         * This method returns a resource that is an Kbank Image Component resource (rather than an image binary, such as a DAM asset).
         * This resource is intended to be rendered via the Kbank Image Component's logic via a Sling include of this resource.
         *
         * @return the (Sling) resource that represents that image to display in the list.
         */
        String getImage();

        /**
         * @return the title of the Image List item (Page).
         */
        String getTitle();

        /**
         * @return the description of the Image List item (Page).
         */
        String getDescription();

        String getFullDescription();

        /**
         * @return the url to the Page the Image List item represents.
         */
        String getURL();

        String getPublishedDate();

        String getAuthor();

        @JsonProperty("dataLayer")
        default ComponentData getData() {
            return null;
        }

    }
}
