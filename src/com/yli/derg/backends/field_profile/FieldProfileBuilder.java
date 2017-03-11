package com.yli.derg.backends.field_profile;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import com.yli.derg.utils.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by liyc on 1/4/16.
 * build method profiles for the DERG
 */
public class FieldProfileBuilder extends DERGBackend {
    public static final String NAME = "field_profile";
    public static final String DESCRIPTION = "generate field profile for each field";

    private HashMap<Node, Node> belongsToMap;
    private HashMap<Node, HashSet<Node>> modifierMap;
    private HashMap<Node, HashSet<Node>> referbyMap;
    private HashMap<Node, HashSet<Node>> duMap;
    private HashMap<Node, Node> instanceMap;

    @Override
    public void run(Graph g) {
        Util.LOGGER.info("start building field profiles");

        belongsToMap = new HashMap<>();
        modifierMap = new HashMap<>();
        referbyMap = new HashMap<>();
        duMap = new HashMap<>();
        instanceMap = new HashMap<>();

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
                if (!referbyMap.containsKey(e.target))
                    referbyMap.put(e.target, new HashSet<Node>());
                referbyMap.get(e.target).add(e.source);
            }
            else if (Edge.TYPE_DEFINE_USE.equals(e.type)) {
                if (!duMap.containsKey(e.source))
                    duMap.put(e.source, new HashSet<Node>());
                duMap.get(e.source).add(e.target);

                if (!duMap.containsKey(e.target))
                    duMap.put(e.target, new HashSet<Node>());
                duMap.get(e.target).add(e.source);
            }
            else if (Edge.TYPE_INSTANCE.equals(e.type)) {
                instanceMap.put(e.source, e.target);
            }
        }

        ArrayList<Map> profiles = new ArrayList<>();
        for (Node node : g.nodes) {
            if (Node.TYPE_FIELD.equals(node.type)) {
                String id = this.getFieldIdentifier(node);
                String signature = this.getFieldSignature(node);
//                String sigHash = DigestUtils.sha256Hex(signature);
                String relation = this.getFieldRelation(node);
                FieldProfile profile = new FieldProfile(id, signature, node.id, relation);
                profiles.add(profile.toMap());
            }
        }

        dumpProfiles(profiles);
        Util.LOGGER.info("finish building field profiles");
    }

    private String getFieldIdentifier(Node fieldNode) {
        Node node = fieldNode;
        ArrayList<String> segs = new ArrayList<>();

        while (belongsToMap.containsKey(node)) {
            segs.add(0, node.name);
            node = belongsToMap.get(node);
        }

        return StringUtils.join(segs, '.');
    }

    private String getFieldSignature(Node fieldNode) {
        ArrayList<String> segs = new ArrayList<>();

        segs.add(String.format("class:%s", belongsToMap.get(fieldNode).sig));

        if (instanceMap.containsKey(fieldNode)) {
            Node typeNode = instanceMap.get(fieldNode);
            if (typeNode.isLib() || Node.TYPE_TYPE.equals(typeNode.type))
                segs.add(String.format("type:%s", typeNode.name));
            else
                segs.add(String.format("type:%s", typeNode.type));
        }

        Collections.sort(segs);
        return StringUtils.join(segs, '\n');
    }

    private String getFieldRelation(Node fieldNode) {
        ArrayList<String> segs = new ArrayList<>();

        if (duMap.containsKey(fieldNode)) {
            HashSet<Node> duNodes = duMap.get(fieldNode);
            for (Node duNode : duNodes) {
                segs.add(String.format("du:%s", duNode.name));
            }
        }

        if (instanceMap.containsKey(fieldNode)) {
            Node typeNode = instanceMap.get(fieldNode);
            segs.add(String.format("type:%s", typeNode.name));
        }

        if (referbyMap.containsKey(fieldNode)) {
            HashSet<Node> methodNodes = referbyMap.get(fieldNode);
            for (Node methodNode : methodNodes) {
                segs.add(String.format("ref_by:%s", methodNode.name));
            }
        }

        Collections.sort(segs);
        return StringUtils.join(segs, '\n');
    }

    private void dumpProfiles(ArrayList<Map> profiles) {
        String export_file_name = String.format("%s/field_profile.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, new JSONArray(profiles).toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
