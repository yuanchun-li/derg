package com.yli.derg.core;

import org.json.JSONObject;
import java.util.HashMap;

/**
 * Created by LiYC on 2015/7/19.
 * Package: UnuglifyDEX
 */
public class Edge {
    public String type;
    public Node source;
    public Node target;

    // relationships between classes
    public static final String TYPE_INHERIT = "CC_inherit";
    public static final String TYPE_OUTER = "CC_outer";
    public static final String TYPE_IMPLEMENT = "CC_implement";

    // relationships inside class
    public static final String TYPE_OVERRIDE = "MM_override";
    public static final String TYPE_CONTAINS = "XX_contains";
    public static final String TYPE_MODIFIER = "XS_modifier";
    public static final String TYPE_INSTANCE = "FT_instance";
    public static final String TYPE_PARAMETER = "MT_parameter";
    public static final String TYPE_RETURN = "MT_return";
    public static final String TYPE_EXCEPTION = "MC_exception";

    // inside a method
    public static final String TYPE_REFER = "MX_refer";

    // define-use relationships
    public static final String TYPE_DEFINE_USE = "XX_DU";

    private Edge(Node source, Node target, String type) {
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public static Edge make(Node source, Node target, String type) {
        if (source == null || target == null || source == target)
            return null;
        return new Edge(source, target, type);
    }

    public static Edge make(Graph g, JSONObject json_edge) {
        if (json_edge.has("source") && json_edge.has("target") && json_edge.has("relation")) {
            int sourceId = json_edge.getInt("source");
            int targetId = json_edge.getInt("target");
            String edgeType = json_edge.getString("relation");
            return new Edge(g.getNodeById(sourceId), g.getNodeById(targetId), edgeType);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return ((o instanceof Edge) && this.type.equals(((Edge) o).type)
                && this.source == ((Edge) o).source && this.target == ((Edge) o).target);
    }

    public JSONObject toJson() {
        return new JSONObject(this.toMap());
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> edgeMap = new HashMap<>();
        edgeMap.put("source", source.id);
        edgeMap.put("target", target.id);
        edgeMap.put("relation", type);
        return edgeMap;
    }

    public String toString() {
        return this.toJson().toString();
    }

    public String getS2Tstr() {return String.format("%d->%d", source.id, target.id);}
    public String getT2Sstr() {return String.format("%d->%d", target.id, source.id);}
}
