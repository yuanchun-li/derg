package com.yli.derg.backends.method_permission;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.backends.method_profile.MethodProfile;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import com.yli.derg.utils.IgnoreUnknownTokenParser;
import com.yli.derg.utils.Util;
import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by liyc on 1/4/16.
 * build method profiles for the DERG
 */
public class MethodPermissionExtractor extends DERGBackend {
    public static final String NAME = "method_permission";
    public static final String DESCRIPTION = "extract each method's required Android permissions";

    private HashMap<Node, HashSet<Node>> referMap;
    private HashMap<String, Set<String>> API2Permissions;
    private File mappingFile;

    public void parseArgs(String[] args) throws ParseException {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        Option mappingOpt = Option.builder("mapping").argName("mapping.csv")
                .longOpt("permission_mapping").hasArg().desc("A file containing the mapping from API to permissions, in PScout format.").build();

        options.addOption(mappingOpt);

        CommandLineParser parser = new IgnoreUnknownTokenParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("mapping")) {
                String mappingFilePath = cmd.getOptionValue("mapping");
                mappingFile = new File(mappingFilePath);
                if (!mappingFile.exists()) {
                    throw new ParseException("mapping file does not exist: " + mappingFilePath);
                }
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1, Option o2) {
                    return o1.getOpt().length() - o2.getOpt().length();
                }
            });
            formatter.printHelp(MethodPermissionExtractor.NAME, options, true);
            throw new ParseException("Parsing arguments failed in " + MethodPermissionExtractor.NAME);
        }
    }

    @Override
    public void run(Graph g) {
        Util.LOGGER.info("loading API to permissions mapping file...");
        loadMappingFile();

        Util.LOGGER.info("start extracting method permissions");

        referMap = new HashMap<>();

        for (Edge e : g.edges) {
            if (Edge.TYPE_REFER.equals(e.type)) {
                if (!referMap.containsKey(e.source))
                    referMap.put(e.source, new HashSet<Node>());
                referMap.get(e.source).add(e.target);
            }
        }

        Map<String, Set<String>> method2Permissions = new HashMap<>();
        for (Node node : g.nodes) {
            if (Node.TYPE_METHOD.equals(node.type)) {
                Set<String> permissions = getPermissionsOfMethod(node);
                method2Permissions.put(node.sig, permissions);
            }
        }

        dumpPermissions(method2Permissions);
        Util.LOGGER.info("finish extracting method permissions");
    }

    // TODO load PScout mapping.csv file, and complete the API2Permissions mapping
    // Note that the method signatures in PScout is different from DERG
    // e.g. In PScout mapping file, a method:
    //         com/android/server/LocationManagerService,getProviders,(Landroid/location/Criteria;Z)Ljava/util/List;
    //      The signature in DERG is:
    //         <java.util.List: com.android.server.LocationManagerService getProviders(android.location.Criteria, int)>
    private void loadMappingFile() {
        API2Permissions = new HashMap<>();
        try {
            List<String> lines = FileUtils.readLines(mappingFile);
            // code here
        } catch (IOException e) {
            Util.LOGGER.warning("exception during loading mapping file.");
            e.printStackTrace();
        }
    }

    private Set<String> getPermissionsOfMethod(Node methodNode) {
        Set<String> permissions = new HashSet<>();

        if (referMap.containsKey(methodNode)) {
            HashSet<Node> referNodes = referMap.get(methodNode);
            for (Node referNode : referNodes) {
                if (referNode.isLib()) {
                    Set<String> permissionsOfAPI = getPermissionsOfAPI(referNode.sig);
                    permissions.addAll(permissionsOfAPI);

                }
            }
        }

        return permissions;
    }

    private Set<String> getPermissionsOfAPI(String API) {
        Set<String> permissions = new HashSet<>();
        if (API2Permissions.containsKey(API)) permissions = API2Permissions.get(API);

        return permissions;
    }

    private void dumpPermissions(Map method2Permissions) {
        String export_file_name = String.format("%s/method_permission.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, new JSONObject(method2Permissions).toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
