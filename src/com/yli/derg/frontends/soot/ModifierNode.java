package com.yli.derg.frontends.soot;

import soot.Modifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liyc on 12/25/15.
 * node of modifier, eg. public, private, static...
 */
public class ModifierNode {
    String name;
    public static final ModifierNode constructorModifier = new ModifierNode("constructor");

    private ModifierNode(String name) {
        this.name = name;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object o) {
        return (o instanceof ModifierNode && ((ModifierNode) o).name.equals(this.name));
    }

    public static List<ModifierNode> parseModifierNodes(int modifiers) {
        String modifierString = Modifier.toString(modifiers);
        String[] modifierSegs = modifierString.split(" ");
        List<ModifierNode> modifierNodes = new ArrayList<>();
        for (String modifierSeg : modifierSegs) {
            if (modifierSeg.length() == 0) continue;
            modifierNodes.add(new ModifierNode(modifierSeg));
        }
        return modifierNodes;
    }
}
