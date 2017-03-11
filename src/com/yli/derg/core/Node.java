package com.yli.derg.core;


import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by LiYC on 2015/7/19.
 * Package: DERG
 */
public class Node {
    public Object content;
    public int id;
    public String name;
    public String type;
    public String sig;

    public static final String TYPE_PACKAGE = "package";
    public static final String TYPE_CLASS = "class";
    public static final String TYPE_FIELD = "field";
    public static final String TYPE_METHOD = "method";
    public static final String TYPE_PARAMETER = "parameter";
    public static final String TYPE_RETURN = "return";
    public static final String TYPE_TYPE = "type";
    public static final String TYPE_CONST = "const";
    public static final String TYPE_MODIFIER = "modifier";

    public static final String TYPE_LIB = "_LIB";

    public static final String TYPE_API = "API";


    private Node(Object content, String name, String type, String sig, int id) {
        this.content = content;
        this.name = name;
        this.type = type;
        this.sig = sig;
        this.id = id;
    }

    private static int count = 0;
    public static Node make(Object obj, String name, String type) {
        if (obj == null) return null;
        return new Node(obj, name, type, "", count++);
    }

    public static Node make(JSONObject jsonObject) {
        if (jsonObject.has("name") && jsonObject.has("type") && jsonObject.has("id")) {
            return new Node(jsonObject,
                    jsonObject.getString("name"),
                    jsonObject.getString("type"),
                    jsonObject.getString("sig"),
                    jsonObject.getInt("id"));
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Node) && this.id == ((Node) o).id;
    }

    public String toString() {
        return this.toJson().toString();
    }

    public JSONObject toJson() {
        return new JSONObject(this.toMap());
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> vertexMap = new HashMap<>();
        vertexMap.put("id", id);
        vertexMap.put("name", name);
        vertexMap.put("type", type);
        vertexMap.put("sig", sig);
        return vertexMap;
    }

    public void setSig(String sig) {
        this.sig = sig;
    }

    public void setAsLib() {
        this.type += TYPE_LIB;
    }
    public boolean isLib() { return this.type.endsWith(TYPE_LIB); }
    public boolean isMethod() { return this.type.startsWith(TYPE_METHOD); }
}