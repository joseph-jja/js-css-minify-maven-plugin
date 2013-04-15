package com.buildtools.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileParser {

    private static final String OUTPUT_MARKER = "-output:";
    private static final String INPUT_MARKER = "+";

    private static final String OUTPUT = "output:";
    private static final String INPUT = "input";

    private static final String CSS_END = ".css";
    private static final String JS_END = ".js";

    private static final String CSS = "css";
    private static final String JS = "js";

    private static final String COMMENT = "#";

    public static final Map<String, List<String>> processConfigFiles(List<String> inputConfigFiles) {

        Map<String, List<String>> results = new HashMap<String, List<String>>();

        // loop through config files 
        for ( String config : inputConfigFiles ) {

            BufferedReader reader = null;
            try {
                // open config file
                File input = new File(config);
                reader = new BufferedReader(new FileReader(input));

                processFile(results, reader);

            } catch (IOException io) {
                // log
            } finally {
                if ( reader != null ) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // log
                    }
                }
            }

        }

        return results;
    }

    private static final boolean isComment(String line) { 

        return ( line.length() <= 0 || line.startsWith(COMMENT) );

    }

    private static final String getFileType(String line) {

        String type = "";
        if ( line.endsWith(JS_END) ) {
            type = JS;
        } else if ( line.endsWith(CSS_END) ) {
            type = CSS;
        }
        return type;
    }

    private static final String getLineType(String line) {

        String type = "";
        if ( line.startsWith(OUTPUT_MARKER) ) {
            type = OUTPUT;
        } else if ( line.startsWith(INPUT_MARKER) ) {
            type = INPUT;
        }
        return type;
    }

    private static final void processFile(Map<String, List<String>> results, BufferedReader reader)  throws IOException {

        String line = reader.readLine();

        String key = "";
        
        while ( line != null ) {
            line = line.trim();
            if ( isComment(line) ) {
                // log?
            } else if ( getLineType(line).equals(OUTPUT) ) {
                key = line.substring(OUTPUT_MARKER.length()).trim();
                results.put(key, new ArrayList<String>());
            } else if ( getLineType(line).equals(INPUT) ) {
                List<String> files = results.get(key);
                files.add(line.substring(INPUT_MARKER.length()).trim());
            }
        }
    }
}
