package com.yli.derg.backends.nice2predict;

import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;

import java.util.*;

/**
 * Created by liyc on 1/14/16.
 * the scope of Nice2Predict dependency network
 */
public class Nice2PredictScope {
    public HashSet<Integer> nodeIds;
    public Nice2PredictScope() {
        nodeIds = new HashSet<>();
    }

    public static ArrayList<Nice2PredictScope> getScopesFromGraph(Graph g) {
        ArrayList<Nice2PredictScope> scopes = new ArrayList<>();
        // overall scope
        Nice2PredictScope overall_scope = new Nice2PredictScope();
        for (Node n : g.nodes) {
            if (n.type.equals(Node.TYPE_PACKAGE) || n.type.equals(Node.TYPE_CLASS))
                overall_scope.nodeIds.add(n.id);
        }
        scopes.add(overall_scope);

        // class/method/field scopes
        HashMap<Integer, Nice2PredictScope> nodeId2Scope = new HashMap<>();
        for (Node n : g.nodes) {
            if (!n.isLib() && (n.type.equals(Node.TYPE_CLASS) ||
                    n.type.equals(Node.TYPE_FIELD) ||
                    n.type.equals(Node.TYPE_METHOD))) {
                Nice2PredictScope scope = new Nice2PredictScope();
                scope.nodeIds.add(n.id);
                nodeId2Scope.put(n.id, scope);
            }
        }
//        for (Edge e : g.edges) {
//            if (e.type.equals(Edge.TYPE_CONTAINS) && e.source.type.equals(Node.TYPE_CLASS)
//                    && !e.source.isLib() && nodeId2Scope.containsKey(e.source.id)) {
//                    nodeId2Scope.put(e.target.id, nodeId2Scope.get(e.source.id));
//            }
//        }
        for (Edge e : g.edges) {
            if (nodeId2Scope.containsKey(e.source.id))
                nodeId2Scope.get(e.source.id).nodeIds.add(e.target.id);
            if (nodeId2Scope.containsKey(e.target.id))
                nodeId2Scope.get(e.target.id).nodeIds.add(e.source.id);
        }

        // add scopes to result
        for (Nice2PredictScope scope : nodeId2Scope.values()) {
            scopes.add(scope);
        }
        return scopes;
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> scopeMap = new HashMap<>();
        ArrayList<Integer> nodeArray = new ArrayList<>();
        nodeArray.addAll(nodeIds);
        Collections.sort(nodeArray);
        scopeMap.put("cn", "!=");
        scopeMap.put("n", nodeArray);
        return scopeMap;
    }
}
