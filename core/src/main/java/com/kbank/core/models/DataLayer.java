package com.kbank.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public interface DataLayer {

    /**
     *
     * @return MAP data to populate the data layer
     */
     String getData();
}
