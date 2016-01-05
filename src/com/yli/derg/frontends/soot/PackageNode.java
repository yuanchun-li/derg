package com.yli.derg.frontends.soot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiYC on 2015/7/25.
 * Package: com.lynnlyc.sootextension
 * PackageNode represent a segment of a package name
 * e.g. in package name com.lynnlyc.sootextension, we have three PackageSegs:
 * com, lynnlyc, and sootextension
 * Why don't use a String?
 * Because the 'a' in com.alice.a and com.bob.a are equal as Strings,
 * but not equal as PackageSegs
 */
public class PackageNode {
    private String packageName;
    private String segName;

    public PackageNode(String packageName, String segName) {
        assert packageName.endsWith(segName);
        this.packageName = packageName;
        this.segName = segName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageNode))
            return false;
        PackageNode other = (PackageNode) obj;
        return this.packageName.equals(other.packageName)
                && this.segName.equals(other.segName);
    }

    public String getSegName() {
        return this.segName;
    }

    public int hashCode() {
        return packageName.hashCode();
    }

    public static List<PackageNode> parsePackageSegs(String packageName) {
        List<PackageNode> result = new ArrayList<>();
        String[] segs = packageName.split("\\.");
        String packagePath = "";
        for (String seg : segs) {
            packagePath += "." + seg;
            PackageNode packageSeg = new PackageNode(packagePath, seg);
            result.add(packageSeg);
        }
        return result;
    }
}
