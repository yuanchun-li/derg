package com.yli.derg.backends.dict_build;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by liyc on 1/5/16.
 * build a dictionary of the element names
 */
public class DictBuilder extends DERGBackend {
    public static final String NAME = "dict_build";
    public static final String DESCRIPTION = "build a dictionary of the element names";

    private List<NodeVocabulary> nodeVocabularies;
    private List<String> wordList;

    @Override
    public void run(Graph g) {
        nodeVocabularies = new ArrayList<>();
        for (Node node : g.nodes) {
            if (node.isLib()) continue;
            if (Node.TYPE_PACKAGE.equals(node.type) || Node.TYPE_CLASS.equals(node.type)
                    || Node.TYPE_METHOD.equals(node.type) || Node.TYPE_FIELD.equals(node.type))
                nodeVocabularies.add(new NodeVocabulary(node));
        }
        wordList = new ArrayList<>();
        for (NodeVocabulary nodeVocabulary : nodeVocabularies) {
            wordList.add(nodeVocabulary.toWordsString());
        }
        this.exportDict();
    }

    private void exportDict() {
        try {
            ArrayList<Map<String, Object>> dict = new ArrayList<>();
            for (NodeVocabulary nodeVocabulary : nodeVocabularies) {
                dict.add(nodeVocabulary.toMap());
            }
            JSONArray jsonDict = new JSONArray(dict);
            String export_file_name = String.format("%s/dict.json", Config.outputDir);
            File export_file = new File(export_file_name);
            FileUtils.writeStringToFile(export_file, jsonDict.toString(2), "UTF-8");

            String plain_export_file_name = String.format("%s/dict_plain.txt", Config.outputDir);
            File plain_export_file = new File(plain_export_file_name);
            FileUtils.writeLines(plain_export_file, "UTF-8", wordList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
