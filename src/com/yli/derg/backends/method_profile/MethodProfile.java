package com.yli.derg.backends.method_profile;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liyc on 1/4/16.
 * method profile
 */
public class MethodProfile {
    private String identifier;
    private String signature;
    public MethodProfile(String identifier, String signature) {
        this.identifier = identifier;
        this.signature = signature;
    }

    public Map<String, String> toMap() {
        Map<String, String> profileMap = new HashMap<>();
        profileMap.put("id", identifier);
        profileMap.put("sig", signature);
        return profileMap;
    }

    public JSONObject toJson() {
        return new JSONObject(this.toMap());
    }
}
