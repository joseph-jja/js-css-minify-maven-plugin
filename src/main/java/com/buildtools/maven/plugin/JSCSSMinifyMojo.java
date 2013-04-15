package com.buildtools.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Echos an object string to the output screen.
 * @execute phase="generate-sources"
 * @goal minify
 * @requiresProject false
 */
public class JSCSSMinifyMojo extends AbstractMojo
{

    /**
     * Flag to disable minification for development
     * @parameter expression="${minify.srcDir}" default-value="src/main/webapp"
     */
    private String srcDir;

    /**
     * Target directory for output of the combo files
     * @parameter expression="${minify.targetDir}" default-value="target"
     */
    private String targetDir;

    /**
     * Base directory of the project.
     * @parameter expression="${basedir}"
     */
    private File baseDirectory;

    /**
     * @parameter
     */
    private List jsConfigFiles;

    /**
     * @parameter
     */
    private List cssConfigFiles;

    /**
     * Paths to exclude from the minifier.  For example: releaseVersion.
     * this can be a release number like 11E or R75 
     * this can be the word timestamp(YYYYMMDDHHMM) or the word datestamp(YYYYMMDD) 
     * this can be an artifact version like ${version} or 2.5.0-SNAPSHOT or any other string 
     * @parameter expression="${minify.releaseVersion}" default-value=""
     */
    private String releaseVersion;

    private static final String CSS = "css";
    private static final String JS = "js";
    private static final String OUTPUT_MARKER = "-output:";
    private static final String INPUT_MARKER = "+";

    public void execute() throws MojoExecutionException, MojoFailureException {
        
        targetDir = targetDir.trim();

        getLog().debug( "Begin..." );

        List<String> inputJSConfigFiles = (List<String>)jsConfigFiles; 
        List<String> inputCSSConfigFiles = (List<String>)cssConfigFiles; 


        boolean useReleaseVersion = false;
        if ( releaseVersion != null && releaseVersion.trim().length() > 0 ) {
            getLog().info( "Version number is: " + releaseVersion);
            useReleaseVersion = true;
            releaseVersion = releaseVersion.trim().replace(" ", "").toLowerCase();
            // calender month is 0 based so we add 1 to it
            if ( releaseVersion.indexOf("datestamp") != -1 ) {
                final Calendar today = Calendar.getInstance();
                final int iMonth = today.get(Calendar.MONTH) + 1;
                final String month = ( iMonth < 10 ) ? "0" + Integer.toString(iMonth) : Integer.toString(iMonth);
                final int iDay = today.get(Calendar.DAY_OF_MONTH);
                final String day = ( iDay < 10 ) ? "0" + Integer.toString(iDay) : Integer.toString(iDay);
                releaseVersion = releaseVersion.replace("datestamp", Integer.toString(today.get(Calendar.YEAR)) + month + day);
            }
            if ( releaseVersion.indexOf("timestamp") != -1 ) {
                final Calendar today = Calendar.getInstance();
                final int iMonth = today.get(Calendar.MONTH) + 1;
                final String month = ( iMonth < 10 ) ? "0" + Integer.toString(iMonth) : Integer.toString(iMonth);
                final int iDay = today.get(Calendar.DAY_OF_MONTH);
                final String day = ( iDay < 10 ) ? "0" + Integer.toString(iDay) : Integer.toString(iDay);
                final int iHour = today.get(Calendar.HOUR);
                final String hour = ( iHour < 10 ) ? "0" + Integer.toString(iHour) : Integer.toString(iHour);
                final int iMinute = today.get(Calendar.MINUTE);
                final String minute = ( iMinute < 10 ) ? "0" + Integer.toString(iMinute) : Integer.toString(iMinute);
                releaseVersion = releaseVersion.replace("timestamp", Integer.toString(today.get(Calendar.YEAR)) + month + day + hour + minute);
            }
        }        

        if ( releaseVersion == null ) { 
            releaseVersion = "";
        }
        
        if ( inputCSSConfigFiles != null && inputCSSConfigFiles.size() > 0 ) { 
            inputJSConfigFiles.addAll(inputCSSConfigFiles);
        }
        
        String cssOrJS = "";
        
        // loop through config files 
        for ( String config : inputJSConfigFiles ) {

            try {
                File input = new File(config);
                BufferedReader reader = new BufferedReader(new FileReader(input));

                String line = reader.readLine();

                OutputStreamWriter outputFile = null;
                while ( line != null ) {
                    final String tline = line.trim();
                    if ( ! tline.startsWith("#") && ! tline.startsWith("//") ) {
                        // next line
                        if ( tline.startsWith(OUTPUT_MARKER) ) {
                            if ( tline.endsWith(".js") ) {
                                cssOrJS = JS;
                            } else if ( tline.endsWith(".css") ) {
                                cssOrJS = CSS;
                            }
                            // start output file
                            outputFile = getOutputFile(tline, cssOrJS);
                        } else if ( tline.startsWith(INPUT_MARKER) ) {
                            // process file to output file
                            final String inputFilename = tline.substring(tline.indexOf(INPUT_MARKER) + INPUT_MARKER.length()).trim();
                            final String realFilename = new File(baseDirectory, srcDir + inputFilename).getAbsolutePath();
                            getLog().debug("Input file: " + realFilename);
                            if ( outputFile != null ) { 
                                if ( cssOrJS.equals(JS) ) {
                                    JavaScriptCompressor compressor = new JavaScriptCompressor(new FileReader(new File(realFilename)), new JSCSSErrorReporter());
                                    compressor.compress(outputFile, 10000, true, false, false, false);
                                } else if ( cssOrJS.equals(CSS) ) {
                                    CssCompressor compressor = new CssCompressor(new FileReader(new File(realFilename)));
                                    compressor.compress(outputFile, -1);
                                }
                            } else {
                                throw new Exception("Malformed configuration file! " + config);
                            }
                        }
                    }
                    line = reader.readLine();
                }
                reader.close();
            } catch(Exception e) { 
                throw new MojoFailureException(e.getMessage());
            }
        }

        getLog().info( "all done. ");
    }
    
    private OutputStreamWriter getOutputFile(String tline, String cssOrJS) throws FileNotFoundException { 
        OutputStreamWriter outputFile = null;
        String outputFilename = tline.substring(tline.indexOf(OUTPUT_MARKER) + OUTPUT_MARKER.length()).trim();
        String appendTo = "";
        if ( releaseVersion != null && releaseVersion.length() > 0 ) {
            appendTo = "-" + releaseVersion;
        }
        if ( cssOrJS.equals(JS) ) {
            outputFilename = outputFilename.replace(".js", "");
            outputFilename = outputFilename + appendTo + ".js";
        } else if ( cssOrJS.equals(CSS) ) {
            outputFilename = outputFilename.replace(".css", "");
            outputFilename = outputFilename + appendTo + ".css";
        }
        final String fileName = new File(baseDirectory, targetDir + outputFilename).getAbsolutePath();
        getLog().debug("The full path of the output file is: " + fileName);
        
        outputFile = new OutputStreamWriter(new FileOutputStream(fileName) );
        
        return outputFile;
    }

    private static class JSCSSErrorReporter implements ErrorReporter {

        public void warning(String message, String sourceName,
                int line, String lineSource, int lineOffset) {
            if (line < 0) {
                System.err.println("\n[COMPRESSOR_WARNING] " + message);
            } else {
                System.err.println("\n[COMPRESSOR_WARNING] " + line + ':' + lineOffset + ':' + message);
            }
        }

        public void error(String message, String sourceName,
                int line, String lineSource, int lineOffset) {
            if (line < 0) {
                System.err.println("\n[COMPRESSOR_ERROR] " + message);
            } else {
                System.err.println("\n[COMPRESSOR_ERROR] " + line + ':' + lineOffset + ':' + message);
            }
        }

        public EvaluatorException runtimeError(String message, String sourceName,
                int line, String lineSource, int lineOffset) {
            error(message, sourceName, line, lineSource, lineOffset);
            return new EvaluatorException(message);
        }
    }

}
