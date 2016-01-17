package com.yli.derg.backends.nice2predict;

import com.yli.derg.Config;
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Graph;
import com.yli.derg.core.Node;
import com.yli.derg.utils.IgnoreUnknownTokenParser;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liyc on 1/14/16.
 * convert graph into nice2predict format
 */
public class Nice2Predict extends DERGBackend {
    public static final String NAME = "nice2predict";
    public static final String DESCRIPTION = "export DERG to nice2predict format.";

    private boolean enableConstant = true;
    private boolean enableModifier = true;
    private boolean enableRefer = true;
    private boolean enableDU = true;

    public void parseArgs(String[] args) throws ParseException {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        Option enable_constant = Option.builder("const").argName("true/false")
                .longOpt("enable-constant").hasArg().desc("enable constant nodes in Nice2Predict, default is true").build();
        Option enable_modifier = Option.builder("modifier").argName("true/false")
                .longOpt("enable-modifier").hasArg().desc("enable modifier nodes in Nice2Predict, default is true").build();
        Option enable_refer = Option.builder("refer").argName("true/false")
                .longOpt("enable-refer").hasArg().desc("enable refer relations in Nice2Predict, default is true").build();
        Option enable_du = Option.builder("du").argName("true/false")
                .longOpt("enable-DU").hasArg().desc("enable DU relations in Nice2Predict, default is true").build();
        Option help_opt = Option.builder("h").desc("print this help message")
                .longOpt("help").build();

        options.addOption(enable_constant);
        options.addOption(enable_modifier);
        options.addOption(enable_refer);
        options.addOption(enable_du);
        options.addOption(help_opt);

        CommandLineParser parser = new IgnoreUnknownTokenParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("const")) {
                String enable_const_opt = cmd.getOptionValue("const").toLowerCase();
                if ("true".equals(enable_const_opt)) {
                    this.enableConstant = true;
                }
                else if ("false".equals(enable_const_opt)) {
                    this.enableConstant = false;
                }
                else {
                    throw new ParseException("const option should be true or false, given: " + enable_const_opt);
                }
            }
            if (cmd.hasOption("modifier")) {
                String enable_modifier_opt = cmd.getOptionValue("modifier");
                if ("true".equals(enable_modifier_opt)) {
                    this.enableModifier = true;
                }
                else if ("false".equals(enable_modifier_opt)) {
                    this.enableModifier = false;
                }
                else {
                    throw new ParseException("modifier option should be true or false, given: " + enable_modifier_opt);
                }
            }
            if (cmd.hasOption("refer")) {
                String enable_refer_opt = cmd.getOptionValue("refer").toLowerCase();
                if ("true".equals(enable_refer_opt)) {
                    this.enableRefer = true;
                }
                else if ("false".equals(enable_refer_opt)) {
                    this.enableRefer = false;
                }
                else {
                    throw new ParseException("refer option should be true or false, given: " + enable_refer_opt);
                }
            }
            if (cmd.hasOption("du")) {
                String enable_du_opt = cmd.getOptionValue("du");
                if ("true".equals(enable_du_opt)) {
                    this.enableDU = true;
                }
                else if ("false".equals(enable_du_opt)) {
                    this.enableDU = false;
                }
                else {
                    throw new ParseException("du option should be true or false, given: " + enable_du_opt);
                }
            }
            if (cmd.hasOption("h")) {
                throw new ParseException("print help message.");
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
            formatter.printHelp(Nice2Predict.NAME, options, true);
            throw new ParseException("Parsing arguments failed in " + Nice2Predict.NAME);
        }
    }

    @Override
    public void run(Graph g) {
        String export_file_name = String.format("%s/nice2predict.json", Config.outputDir);
        File export_file = new File(export_file_name);

        if (!enableConstant) g = g.removeNodesOfType(Node.TYPE_CONST);
        if (!enableModifier) g = g.removeNodesOfType(Node.TYPE_MODIFIER);
        if (!enableRefer) g = g.removeEdgesOfType(Edge.TYPE_REFER);
        if (!enableDU) g = g.removeEdgesOfType(Edge.TYPE_DEFINE_USE);

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
