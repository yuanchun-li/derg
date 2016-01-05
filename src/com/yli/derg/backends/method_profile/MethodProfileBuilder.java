package com.yli.derg.backends.method_profile;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import com.yli.derg.utils.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by liyc on 1/4/16.
 * build method profiles for the DERG
 */
public class MethodProfileBuilder extends DERGBackend {
    public static final String NAME = "method_profile";
    public static final String DESCRIPTION = "generate method profile for each method";

    private HashMap<Node, Node> belongsToMap;
    private HashMap<Node, HashSet<Node>> modifierMap;
    private HashMap<Node, HashSet<Node>> referMap;
    private HashMap<Node, HashSet<Node>> paraMap;
    private HashMap<Node, Node> retMap;

    @Override
    public void run(Graph g) {
        Util.LOGGER.info("start building method profiles");

        belongsToMap = new HashMap<>();
        modifierMap = new HashMap<>();
        referMap = new HashMap<>();
        paraMap = new HashMap<>();
        retMap = new HashMap<>();

        for (Edge e : g.edges) {
            if (Edge.TYPE_CONTAINS.equals(e.type)) {
                belongsToMap.put(e.target, e.source);
            }
//            else if (Edge.TYPE_MODIFIER.equals(e.type)) {
//                if (!modifierMap.containsKey(e.source))
//                    modifierMap.put(e.source, new HashSet<Node>());
//                modifierMap.get(e.source).add(e.target);
//            }
            else if (Edge.TYPE_REFER.equals(e.type)) {
                if (!referMap.containsKey(e.source))
                    referMap.put(e.source, new HashSet<Node>());
                referMap.get(e.source).add(e.target);
            }
            else if (Edge.TYPE_PARAMETER.equals(e.type)) {
                if (!paraMap.containsKey(e.source))
                    paraMap.put(e.source, new HashSet<Node>());
                paraMap.get(e.source).add(e.target);
            }
            else if (Edge.TYPE_RETURN.equals(e.type)) {
                retMap.put(e.source, e.target);
            }
        }

        ArrayList<Map> profiles = new ArrayList<>();
        for (Node node : g.nodes) {
            if (Node.TYPE_METHOD.equals(node.type)) {
                String id = this.getMethodIdentifier(node);
                String signature = this.getMethodSignature(node);
                String sigHash = DigestUtils.sha256Hex(signature);
                MethodProfile profile = new MethodProfile(id, sigHash);
                profiles.add(profile.toMap());
            }
        }

        dumpProfiles(profiles);
        Util.LOGGER.info("finish building method profiles");
    }

    private String getMethodIdentifier(Node methodNode) {
        Node node = methodNode;
        ArrayList<String> segs = new ArrayList<>();

        while (belongsToMap.containsKey(node)) {
            segs.add(0, node.name);
            node = belongsToMap.get(node);
        }

        return StringUtils.join(segs, '.');
    }

    private String getMethodSignature(Node methodNode) {
        ArrayList<String> segs = new ArrayList<>();

        if (paraMap.containsKey(methodNode)) {
            HashSet<Node> paraNodes = paraMap.get(methodNode);
            for (Node paraNode : paraNodes) {
                if (paraNode.isLib() || Node.TYPE_TYPE.equals(paraNode.type))
                    segs.add(String.format("para:%s", paraNode.name));
                else
                    segs.add(String.format("para:%s", paraNode.type));
            }
        }

        if (retMap.containsKey(methodNode)) {
            Node retNode = retMap.get(methodNode);
            if (retNode.isLib() || Node.TYPE_TYPE.equals(retNode.type))
                segs.add(String.format("ret:%s", retNode.name));
            else
                segs.add(String.format("ret:%s", retNode.type));
        }

        if (referMap.containsKey(methodNode)) {
            HashSet<Node> referNodes = referMap.get(methodNode);
            for (Node referNode : referNodes) {
                if (referNode.isLib())
                    segs.add(String.format("refer:%s", referNode.name));
                else
                    segs.add(String.format("refer:%s", referNode.type));
            }
        }

        Collections.sort(segs);
        return StringUtils.join(segs, '\n');
    }

    private void dumpProfiles(ArrayList<Map> profiles) {
        String export_file_name = String.format("%s/method_profile.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, new JSONArray(profiles).toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
