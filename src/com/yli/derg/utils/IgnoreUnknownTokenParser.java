package com.yli.derg.utils;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import java.util.ListIterator;

/**
 * Created by liyc on 12/24/15.
 * Extend default parser to ignore unknown tokens
 */
public class IgnoreUnknownTokenParser extends BasicParser {
    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg);

        if (hasOption) {
            super.processOption(arg, iter);
        }
    }
}
