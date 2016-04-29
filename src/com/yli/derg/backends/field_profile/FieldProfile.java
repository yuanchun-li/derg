package com.yli.derg.backends.field_profile;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liyc on 1/4/16.
 * field profile
 */
public class FieldProfile {
    private String identifier;
    private String signature;
    private int DERG_id;
    private String relation;

    public FieldProfile(String identifier, String signature, int DERG_id, String relation) {
        this.identifier = identifier;
        this.signature = signature;
        this.DERG_id = DERG_id;
        this.relation = relation;
    }

    public Map<String, String> toMap() {
        Map<String, String> profileMap = new HashMap<>();
        profileMap.put("id", identifier);
        profileMap.put("sig", signature);
        profileMap.put("DERG_id", String.valueOf(DERG_id));
        profileMap.put("relation", relation);
        return profileMap;
    }

    public JSONObject toJson() {
        return new JSONObject(this.toMap());
    }
}
