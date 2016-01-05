package com.yli.derg.frontends.json;

import com.yli.derg.Config;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import com.yli.derg.frontends.DERGFrontend;
import com.yli.derg.utils.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by liyc on 12/23/15.
 * Build DERG from JSON input
 */
public class JSONBuilder extends DERGFrontend {
    public static final String NAME = "json";
    public static final String DESCRIPTION = "build DERG from json";
    private JSONObject json_g;
    private JSONArray json_nodes;
    private JSONArray json_edges;

    public JSONBuilder() {
    }

    public void parseArgs(String[] args) throws ParseException {
        if (!Config.inputDirOrFile.endsWith(".json"))
            throw new ParseException("Input file should be *.json");
    }

    private boolean init() {
        File json_f = new File(Config.inputDirOrFile);
        try {
            String json_str = FileUtils.readFileToString(json_f);
            json_g = new JSONObject(json_str);
            json_nodes = json_g.getJSONArray("nodes");
            json_edges = json_g.getJSONArray("edges");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Graph build() {
        if (!init()) {
            Util.LOGGER.warning("error parsing json derg.");
            return null;
        }

        Graph g = new Graph();
        for (int i = 0; i < json_nodes.length(); i++) {
            JSONObject json_node = json_nodes.getJSONObject(i);
            Node node = Node.make(json_node);
            if (node != null) {
                g.nodes.add(node);
                if (node.id == 0) g.v_root = node;
            }
            else
                Util.LOGGER.warning("error parsing json node: " + json_node);
        }

        for (int i = 0; i < json_edges.length(); i++) {
            JSONObject json_edge = json_edges.getJSONObject(i);
            Edge edge = Edge.make(g, json_edge);
            if (edge != null)
                g.edges.add(edge);
            else
                Util.LOGGER.warning("error parsing json edge: " + json_edge);
        }
        return g;
    }
}
