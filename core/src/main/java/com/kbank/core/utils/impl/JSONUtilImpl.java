package com.kbank.core.utils.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kbank.core.utils.JSONUtil;

public class JSONUtilImpl implements JSONUtil {
    @Override
    public JsonElement getFirstElement(JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.entrySet().isEmpty()) {
            return null;
        }

        // Get the first entry in the JsonObject
        return jsonObject.entrySet().iterator().next().getValue();
    }
}
