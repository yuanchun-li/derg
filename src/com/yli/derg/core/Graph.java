package com.yli.derg.core;

import org.json.JSONObject;

import java.io.PrintStream;
import java.util.*;

/**
 * Created by LiYC on 2015/7/19.
 * Package: DERG
 */
public class Graph {
    public ArrayList<Node> nodes;
    public ArrayList<Edge> edges;
    public Node v_root;

    private static final String rootCode = "DERG_ROOT";
    private HashMap<Object, Node> obj2nodeMap;

    public Graph() {
        obj2nodeMap = new HashMap<>();
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public Node genDefaultRoot() {
        v_root = this.getNodeOrCreate(rootCode, rootCode, Node.TYPE_PACKAGE);
        return v_root;
    }

    // sort the edges in DERG, remove the duplicated edges
    public void sortGraph() {
        ArrayList<Edge> dup_edges = new ArrayList<>();
        dup_edges.addAll(edges);
        Collections.sort(dup_edges, new Comparator<Edge>() {
            @Override
            public int compare(Edge o1, Edge o2) {
                if (o1.source.id == o2.source.id) {
                    if (o1.target.id == o2.target.id) {
                        return String.CASE_INSENSITIVE_ORDER.compare(o1.type, o2.type);
                    }
                    return o1.target.id - o2.target.id;
                }
                return o1.source.id - o2.source.id;
            }
        });

        ArrayList<Edge> distinct_edges = new ArrayList<>();
        Edge prev_edge = null;
        for (Edge edge : dup_edges) {
            if (edge.equals(prev_edge)) continue;
            distinct_edges.add(edge);
            prev_edge = edge;
        }

        this.edges = distinct_edges;
    }

    public JSONObject toJson() {
        return new JSONObject(this.toMap());
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> requestMap = new HashMap<>();
        ArrayList<Map> edges = new ArrayList<>();
        ArrayList<Map> nodes = new ArrayList<>();
        for (Edge e : this.edges) {
            edges.add(e.toMap());
        }
        for (Node v : this.nodes) {
            nodes.add(v.toMap());
        }
        requestMap.put("edges", edges);
        requestMap.put("nodes", nodes);
        return requestMap;
    }

    public Node getNodeOrCreate(Object object, String name, String type) {
        Node node = this.getNodeByObject(object);
        if (node == null) {
            node = Node.make(object, name, type);
            if (node != null) {
                this.nodes.add(node);
                this.obj2nodeMap.put(object, node);
            }
        }
        return node;
    }

    public Node getNodeByObject(Object object) {
        if (this.obj2nodeMap.containsKey(object)) {
            return this.obj2nodeMap.get(object);
        }

        return null;
    }

    public Node getNodeById(int id) {
        return nodes.get(id);
    }

    public Edge createEdge(Node source, Node target, String type) {
        Edge edge = Edge.make(source, target, type);
        if (edge != null) {
            this.edges.add(edge);
        }
        return edge;
    }

    public Graph removeNodesOfType(String nodeType) {
        Graph g_new = new Graph();
        for (Node node : this.nodes) {
            if (node.type.equals(nodeType))
                continue;
            g_new.nodes.add(node);
        }
        for (Edge edge : this.edges) {
            if (edge.source.type.equals(nodeType) || edge.target.type.equals(nodeType))
                continue;
            g_new.edges.add(edge);
        }
        return g_new;
    }

    public Graph removeEdgesOfType(String edgeType) {
        Graph g_new = new Graph();
        for (Node node : this.nodes) {
            g_new.nodes.add(node);
        }
        for (Edge edge : this.edges) {
            if (edge.type.equals(edgeType))
                continue;
            g_new.edges.add(edge);
        }
        return g_new;
    }
}
