package com.yli.derg.frontends.soot;

import com.yli.derg.Config;
import com.yli.derg.core.Edge;
import com.yli.derg.core.Node;
import com.yli.derg.utils.IgnoreUnknownTokenParser;
import com.yli.derg.utils.Util;
import com.yli.derg.core.Graph;
import com.yli.derg.frontends.DERGFrontend;
import org.apache.commons.cli.*;
import soot.*;
import soot.Type;
import soot.jimple.*;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.JAssignStmt;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.*;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * Created by liyc on 12/23/15.
 * build the DERG of an Android application
 */
public class SootBuilder extends DERGFrontend {
    public static final String NAME = "soot";
    public static final String DESCRIPTION = "build DERG from java (android) using soot.";

    private ArrayList<SootClass> applicationClasses;

    // File path of android.jar which is forced to use by soot
    private String forceAndroidJarPath = "";
    // Libraries' directory, to be added to soot classpath
    private String librariesDir = "";

    private boolean enableConstant = true;
    private boolean enableModifier = true;

    public SootBuilder() {}

    public void parseArgs(String[] args) throws ParseException {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        Option library = Option.builder("l").argName("directory")
                .longOpt("library").hasArg().desc("path to library dir").build();
        Option sdk = Option.builder("sdk").argName("android.jar")
                .longOpt("android-sdk").hasArg().desc("path to android.jar").build();
        Option enable_constant = Option.builder("const").argName("true/false")
                .longOpt("enable-constant").hasArg().desc("enable constant nodes in DERG, default is true").build();
        Option enable_modifier = Option.builder("modifier").argName("true/false")
                .longOpt("enable-modifier").hasArg().desc("enable modifier nodes in DERG, default is true").build();
        Option help_opt = Option.builder("h").desc("print this help message")
                .longOpt("help").build();

        options.addOption(library);
        options.addOption(sdk);
        options.addOption(enable_constant);
        options.addOption(enable_modifier);
        options.addOption(help_opt);

        CommandLineParser parser = new IgnoreUnknownTokenParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption('l')) {
                librariesDir = cmd.getOptionValue('l');
                File lib = new File(librariesDir);
                if (!lib.exists()) {
                    throw new ParseException("Library does not exist.");
                }
                if (lib.isFile() && !lib.getName().endsWith(".jar")) {
                    throw new ParseException("Library format error, should be directory or jar.");
                }
            }
            if (cmd.hasOption("sdk")) {
                forceAndroidJarPath = cmd.getOptionValue("sdk");
                File sdkFile = new File(forceAndroidJarPath);
                if (!sdkFile.exists()) {
                    throw new ParseException("Android jar does not exist.");
                }
            }
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
            formatter.printHelp(SootBuilder.NAME, options, true);
            throw new ParseException("Parsing arguments failed in " + SootBuilder.NAME);
        }
    }

    private boolean init() {
        Util.LOGGER.info("Start Initializing " + SootBuilder.NAME);
        Options.v().set_debug(false);
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_dir(Config.outputDir);

        List<String> process_dirs = new ArrayList<>();
        process_dirs.add(Config.inputDirOrFile);
        Options.v().set_process_dir(process_dirs);

        if (Config.inputDirOrFile.endsWith(".apk")) {
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_output_format(Options.output_format_dex);
        }
        else if (Config.inputDirOrFile.endsWith(".jar")) {
            Options.v().set_src_prec(Options.src_prec_class);
            Options.v().set_output_jar(true);
        }
        else {
            Options.v().set_src_prec(Options.src_prec_java);
            Options.v().set_output_format(Options.output_format_jimple);
        }

        String classpath = "";
        if (this.librariesDir != null && this.librariesDir.length() != 0) {
            File lib = new File(this.librariesDir);
            if (lib.isFile() && lib.getName().endsWith(".jar"))
                classpath = lib.getAbsolutePath();
            else if (lib.isDirectory()) {
                FileFilter fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".jar");
                    }
                };
                for (File file : lib.listFiles(fileFilter)) {
                    classpath += file.getAbsolutePath() + ";";
                }
            }
            Options.v().set_soot_classpath(classpath);
        }

        Options.v().set_force_android_jar(this.forceAndroidJarPath);

        Scene.v().loadNecessaryClasses();

        applicationClasses = new ArrayList<>();
        for (SootClass cls : Scene.v().getApplicationClasses()) {
            applicationClasses.add(cls);
        }
        Collections.sort(applicationClasses, new Comparator<SootClass>() {
            @Override
            public int compare(SootClass o1, SootClass o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(
                        o1.getName(), o2.getName());
            }
        });
        Util.LOGGER.info("Finish Initializing " + SootBuilder.NAME);
        return true;
    }

    public static Node getPackageNode(Graph g, PackageNode pkgNode) {
        return g.getNodeOrCreate(pkgNode, pkgNode.getSegName(), Node.TYPE_PACKAGE);
    }

    public static Node getClassNode(Graph g, SootClass cls) {
        Node result = g.getNodeOrCreate(cls, cls.getShortName(), Node.TYPE_CLASS);
        result.sig = cls.getName();
        return result;
    }

    public static Node getMethodNode(Graph g, SootMethod method) {
        Node result = g.getNodeOrCreate(method, method.getName(), Node.TYPE_METHOD);
        result.sig = method.getSignature();
        return result;
    }

    public static Node getFieldNode(Graph g, SootField field) {
        Node result = g.getNodeOrCreate(field, field.getName(), Node.TYPE_FIELD);
        result.sig = field.getSignature();
        return result;
    }

    public static Node getTypeNode(Graph g, Type type) {
        if (type instanceof RefType) {
            RefType refType = (RefType) type;
            return getClassNode(g, refType.getSootClass());
        }
        return g.getNodeOrCreate(type, type.toString(), Node.TYPE_TYPE);
    }

    public static Node getConstNode(Graph g, Constant con) {
        return g.getNodeOrCreate(con, con.toString(), Node.TYPE_CONST);
    }

    public void addPackageRelations(Graph g, SootClass cls) {
        Node v_cls = getClassNode(g, cls);
        String pkgName = cls.getPackageName();
        List<PackageNode> packageSegs = PackageNode.parsePackageSegs(pkgName);
        Node v_prev_seg = g.v_root;
        for (PackageNode seg : packageSegs) {
            Node v_seg = getPackageNode(g, seg);
            g.createEdge(v_prev_seg, v_seg, Edge.TYPE_CONTAINS);
            v_prev_seg = v_seg;
        }

        // add CONTAINS edges. (package contains class)
        g.createEdge(v_prev_seg, v_cls, Edge.TYPE_CONTAINS);
    }

    public void addClassRelations(Graph g, SootClass cls) {
        Node v_cls = getClassNode(g, cls);
        // add INHERIT edges
        if (cls.hasSuperclass()) {
            SootClass super_cls = cls.getSuperclass();
            Node v_super_cls = getClassNode(g, super_cls);
            g.createEdge(v_cls, v_super_cls, Edge.TYPE_INHERIT);
        }

        // add OUTER edges
        if (cls.hasOuterClass()) {
            SootClass outer_cls = cls.getOuterClass();
            Node v_outer_cls = getClassNode(g, outer_cls);
            g.createEdge(v_cls, v_outer_cls, Edge.TYPE_OUTER);
        }

        // add implement edges
        for (SootClass interface_cls : cls.getInterfaces()) {
            Node v_interface_cls = getClassNode(g, interface_cls);
            g.createEdge(v_cls, v_interface_cls, Edge.TYPE_IMPLEMENT);
        }

        // add class modifier edges
        addModifierRelations(g, v_cls, cls.getModifiers());
    }

    public void addFieldRelations(Graph g, SootField field) {
        Node v_cls = getClassNode(g, field.getDeclaringClass());
        // add field edges
        Node v_field = getFieldNode(g, field);
        g.createEdge(v_cls, v_field, Edge.TYPE_CONTAINS);

        // add field type edges
        Type type = field.getType();
        Node v_type = getTypeNode(g, type);
        g.createEdge(v_field, v_type, Edge.TYPE_INSTANCE);

        // add field modifier edges
        addModifierRelations(g, v_field, field.getModifiers());
    }

    public void addModifierRelations(Graph g, Node v, int modifiers) {
        if (!enableModifier) return;
        for (ModifierNode modifierNode : ModifierNode.parseModifierNodes(modifiers)) {
            Node v_modifier = g.getNodeOrCreate(modifierNode, modifierNode.name, Node.TYPE_MODIFIER);
            g.createEdge(v, v_modifier, Edge.TYPE_MODIFIER);
        }
    }

    public void addMethodRelations(Graph g, SootMethod method) {
        // add method edges
        Node v_cls = getClassNode(g, method.getDeclaringClass());
        Node v_method = getMethodNode(g, method);
        g.createEdge(v_cls, v_method, Edge.TYPE_CONTAINS);

        // add method override type edges
        if (!method.isConstructor()) {
            SootClass cls_i = method.getDeclaringClass();
            while (cls_i.hasSuperclass()) {
                cls_i = cls_i.getSuperclass();
                for (SootMethod m : cls_i.getMethods()) {
                    if (m.getSubSignature().equals(method.getSubSignature())) {
                        Node v_override_method = getMethodNode(g, m);
                        g.createEdge(v_method, v_override_method, Edge.TYPE_OVERRIDE);
                        break;
                    }
                }
            }
        } else if (enableModifier) {
            Node v_constructor_modifier = g.getNodeOrCreate(ModifierNode.constructorModifier,
                    ModifierNode.constructorModifier.name, Node.TYPE_MODIFIER);
            g.createEdge(v_method, v_constructor_modifier, Edge.TYPE_MODIFIER);
        }

        // add method modifier edges
        addModifierRelations(g, v_method, method.getModifiers());

        // add method return type edges
        Type ret_type = method.getReturnType();
        Node v_ret_type = getTypeNode(g, ret_type);
        g.createEdge(v_method, v_ret_type, Edge.TYPE_RETURN);

        // add method parameter type edges
//        int para_index = 0;
        for (Type para_type : method.getParameterTypes()) {
            Node v_para_type = getTypeNode(g, para_type);
//            g.createEdge(v_method, v_para_type, Edge.TYPE_PARAMETER + (para_index++));
            g.createEdge(v_method, v_para_type, Edge.TYPE_PARAMETER);
        }

        // add exception edges
        for (SootClass exception_cls : method.getExceptions()) {
            Node v_exception_cls = getClassNode(g, exception_cls);
            g.createEdge(v_method, v_exception_cls, Edge.TYPE_EXCEPTION);
        }
    }

    public void addDefineUseRelations(Graph g, SootMethod method) {
        // consider the scope inside a method
        if (method.getSource() == null) return;
        Node v_method = getMethodNode(g, method);
        try {
            Body body = method.retrieveActiveBody();

            // add reference relation
            for (ValueBox valueBox : body.getUseAndDefBoxes()) {
                Value value = valueBox.getValue();
                if (value instanceof FieldRef) {
                    Node v_used_field = getFieldNode(g, ((FieldRef) value).getField());
                    g.createEdge(v_method, v_used_field, Edge.TYPE_REFER);
                } else if (value instanceof InvokeExpr) {
                    Node v_used_method = getMethodNode(g, ((InvokeExpr) value).getMethod());
                    g.createEdge(v_method, v_used_method, Edge.TYPE_REFER);
                }
                else if (enableConstant && value instanceof Constant) {
                    Node v_used_constant = getConstNode(g, (Constant) value);
                    g.createEdge(v_method, v_used_constant, Edge.TYPE_REFER);
                }
            }

            BriefUnitGraph ug = new BriefUnitGraph(body);
            SimpleLocalDefs localDefs = new SimpleLocalDefs(ug);
            SimpleLocalUses localUses = new SimpleLocalUses(body, localDefs);

            // consider def-use relationships
            for (Unit u : body.getUnits()) {
                if (!(u instanceof AbstractDefinitionStmt)) continue;
                AbstractDefinitionStmt s = (AbstractDefinitionStmt) u;
                Value s_rOp = s.getRightOp();
                Value s_lOp = s.getLeftOp();
                if (s_rOp instanceof FieldRef) {
                    // if this stmt uses a field
                    // get all usages of this stmt, and add DU edges
                    Node v_source = getFieldNode(g, ((FieldRef) s_rOp).getField());
                    HashSet<UnitValueBoxPair> allUses = new HashSet<>();
                    getAllUsesOf(u, allUses, localUses);

                    for (UnitValueBoxPair u_vb : allUses) {
                        Unit u_use = u_vb.getUnit();
//                        Value value_use = u_vb.getValueBox().getValue();
                        if (u_use instanceof JAssignStmt) {
                            Value lOp = ((JAssignStmt) u_use).getLeftOp();
                            if (lOp instanceof FieldRef) {
                                Node v_target = getFieldNode(g, ((FieldRef) lOp).getField());
                                g.createEdge(v_source, v_target, Edge.TYPE_DEFINE_USE);
                            }
                        }
                        if (((Stmt) u_use).containsInvokeExpr()) {
                            InvokeExpr invoke_expr = ((Stmt) u_use).getInvokeExpr();
                            Node v_invoked = getMethodNode(g, invoke_expr.getMethod());
                            g.createEdge(v_source, v_invoked, Edge.TYPE_DEFINE_USE);
//                            int para_idx = invoke_expr.getArgs().indexOf(value_use);
//                            if (para_idx < 0) continue;
                        }
                    }
                }
                if (s_lOp instanceof FieldRef) {
                    // if this stmt defines a field
                    // get all defines of this stmt, and add DU edges
                    Node v_target = getFieldNode(g, ((FieldRef) s_lOp).getField());
                    HashSet<Unit> allDefines = new HashSet<>();
                    getAllDefsOf(u, allDefines, localDefs);

                    for (Unit u_def : allDefines) {
//                        Value value_use = u_vb.getValueBox().getValue();
                        if (u_def instanceof JAssignStmt) {
                            Value rOp = ((JAssignStmt) u_def).getRightOp();
                            if (rOp instanceof FieldRef) {
                                Node v_source = getFieldNode(g, ((FieldRef) rOp).getField());
                                g.createEdge(v_source, v_target, Edge.TYPE_DEFINE_USE);
                            }
                            else if (rOp instanceof InvokeExpr) {
                                InvokeExpr invoke_expr = (InvokeExpr) rOp;
                                Node v_invoked = getMethodNode(g, invoke_expr.getMethod());
                                g.createEdge(v_invoked, v_target, Edge.TYPE_DEFINE_USE);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Util.logException(e);
        }
    }

    public void convertLibNodes(Graph g) {
        for (Node n: g.nodes) {
            SootClass cls = getNodeClass(n);
            if (cls == null || cls.isApplicationClass()) continue;
            n.setAsLib();
            if (n.content instanceof SootClass) {
                n.name = ((SootClass) n.content).getName();
            }
            else if (n.content instanceof SootMethod) {
                n.name = ((SootMethod) n.content).getSignature();
            }
            else if (n.content instanceof SootField) {
                n.name = ((SootField) n.content).getSignature();
            }
        }
    }

    public SootClass getNodeClass(Node n) {
        if (n.content instanceof SootClass) return (SootClass) n.content;
        if (n.content instanceof SootField) return ((SootField) n.content).getDeclaringClass();
        if (n.content instanceof SootMethod) return ((SootMethod) n.content).getDeclaringClass();
        return null;
    }

    public Graph build() {
        this.init();
//        PackManager.v().runPacks();
        Util.LOGGER.info("generating DERG");

        Graph g = new Graph();
        g.genDefaultRoot();

        for (SootClass cls : this.applicationClasses) {
            addPackageRelations(g, cls);
            addClassRelations(g, cls);

            // Consider the scope inside the class
            // for each field
            for (SootField field : cls.getFields()) {
                addFieldRelations(g, field);
            }

            // for each method
            for (SootMethod method : cls.getMethods()) {
                addMethodRelations(g, method);
                addDefineUseRelations(g, method);
            }
        }
        convertLibNodes(g);
        g.sortGraph();

        Util.LOGGER.info("finished building DERG");
        return g;
    }

    public void getAllUsesOf(Unit u, HashSet<UnitValueBoxPair> allUses, LocalUses localUses) {
        List<UnitValueBoxPair> uses = localUses.getUsesOf(u);
        for (UnitValueBoxPair unitValueBoxPair : uses) {
            if (allUses.contains(unitValueBoxPair)) continue;
            Unit use = unitValueBoxPair.getUnit();
            allUses.add(unitValueBoxPair);
            getAllUsesOf(use, allUses, localUses);
        }
    }

    public void getAllDefsOf(Unit u, HashSet<Unit> allDefs, LocalDefs localDefs) {
        HashSet<Unit> usedLocalDefs = new HashSet<>();
        for (ValueBox use : u.getUseBoxes()) {
            Value useValue = use.getValue();
            if (useValue instanceof Local) {
                usedLocalDefs.addAll(localDefs.getDefsOfAt((Local) useValue, u));
            }
        }
        for (Unit defUnit : usedLocalDefs) {
            if (allDefs.contains(defUnit)) continue;
            allDefs.add(defUnit);
            getAllDefsOf(defUnit, allDefs, localDefs);
        }
    }
}
