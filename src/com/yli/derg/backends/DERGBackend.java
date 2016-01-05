package com.yli.derg.backends;

import com.yli.derg.backends.dict_build.DictBuilder;
import com.yli.derg.backends.graph_export.GraphExporter;
import com.yli.derg.backends.method_profile.MethodProfileBuilder;
import com.yli.derg.core.Graph;
import com.yli.derg.utils.Util;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;

/**
 * Created by liyc on 1/4/16.
 * super class of all DERG backends
 */
public abstract class DERGBackend {
    public static HashMap<String, DERGBackend> availableBackends = new HashMap<>();
    public static String defaultBackend = "";
    public static void registerBackends() {
        defaultBackend = GraphExporter.NAME;
        availableBackends.put(GraphExporter.NAME, new GraphExporter());
        availableBackends.put(MethodProfileBuilder.NAME, new MethodProfileBuilder());
        availableBackends.put(DictBuilder.NAME, new DictBuilder());
    }
    public static String getAvailableBackends() {
        return StringUtils.join(availableBackends.keySet(), "/");
    }

    public void parseArgs(String[] args) throws ParseException {}

    public abstract void run(Graph g);

    public static DERGBackend get(String type) {
        if (type == null || type.length() == 0) {
            Util.LOGGER.warning(String.format("no backend specified, using %s by default.", defaultBackend));
            type = defaultBackend;
        }
        if (availableBackends.containsKey(type)) {
            return availableBackends.get(type);
        }
        return null;
    }

}
