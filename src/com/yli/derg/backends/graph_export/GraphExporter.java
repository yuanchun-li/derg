package com.yli.derg.backends.graph_export;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Graph;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by liyc on 1/4/16.
 * export graph to file or database
 */
public class GraphExporter extends DERGBackend {
    public static final String NAME = "graph_export";
    public static final String DESCRIPTION = "export DERG to file.";

    @Override
    public void run(Graph g) {
        String export_file_name = String.format("%s/derg.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, g.toJson().toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
