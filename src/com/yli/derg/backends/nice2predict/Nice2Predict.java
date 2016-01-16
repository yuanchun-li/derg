package com.yli.derg.backends.nice2predict;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liyc on 1/14/16.
 * convert graph into nice2predict format
 */
public class Nice2Predict extends DERGBackend {
    public static final String NAME = "nice2predict";
    public static final String DESCRIPTION = "export DERG to nice2predict format.";

    @Override
    public void run(Graph g) {
        String export_file_name = String.format("%s/nice2predict.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, DERG2NPJson(g).toString(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject DERG2NPJson(Graph g) {
        HashMap<String, Object> requestMap = new HashMap<>();
        ArrayList<Map> query = new ArrayList<>();
        ArrayList<Map> assign = new ArrayList<>();
        for (Edge e : g.edges) {
            query.add(DERGEdge2NPEdge(e));
        }
        for (Nice2PredictScope scope : Nice2PredictScope.getScopesFromGraph(g)) {
            query.add(scope.toMap());
        }
        for (Node n : g.nodes) {
            assign.add(DERGNode2NPNode(n));
        }
        requestMap.put("query", query);
        requestMap.put("assign", assign);

        return new JSONObject(requestMap);
    }

    public static HashMap<String, Object> DERGEdge2NPEdge(Edge e) {
        HashMap<String, Object> edgeMap = new HashMap<>();
        String edgeType = String.format("%s_%s_%s", e.type, e.source.type, e.target.type);
        edgeMap.put("a", e.source.id);
        edgeMap.put("b", e.target.id);
        edgeMap.put("f2", edgeType);
        return edgeMap;
    }

    public static HashMap<String, Object> DERGNode2NPNode(Node n) {
        HashMap<String, Object> nodeMap = new HashMap<>();
        nodeMap.put("v", n.id);
        if (isNodeKnown(n))
            nodeMap.put("giv", n.name);
        else
            nodeMap.put("inf", n.name);
        return nodeMap;
    }

    public static boolean isNodeKnown(Node n) {
        return n.isLib() ||
                !(n.type.equals(Node.TYPE_CLASS) || n.type.equals(Node.TYPE_PACKAGE) ||
                        n.type.equals(Node.TYPE_FIELD) || n.type.equals(Node.TYPE_METHOD));
    }
}
