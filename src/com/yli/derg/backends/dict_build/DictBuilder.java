package com.yli.derg.backends.dict_build;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by liyc on 1/5/16.
 * build a dictionary of the element names
 */
public class DictBuilder extends DERGBackend {
    public static final String NAME = "dict_build";
    public static final String DESCRIPTION = "build a dictionary of the element names";

    private HashSet<NodeVocabulary> nodeVocabularies;

    @Override
    public void run(Graph g) {
        nodeVocabularies = new HashSet<>();
        for (Node node : g.nodes) {
            if (node.isLib()) continue;
            if (Node.TYPE_PACKAGE.equals(node.type) || Node.TYPE_CLASS.equals(node.type)
                    || Node.TYPE_METHOD.equals(node.type) || Node.TYPE_FIELD.equals(node.type))
                nodeVocabularies.add(new NodeVocabulary(node));
        }
        this.exportDict();
    }

    private void exportDict() {
        ArrayList<Map<String, Object>> dict = new ArrayList<>();
        for (NodeVocabulary nodeVocabulary : nodeVocabularies) {
            dict.add(nodeVocabulary.toMap());
        }
        JSONArray jsonDict = new JSONArray(dict);
        String export_file_name = String.format("%s/dict.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, jsonDict.toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
