/*
 * The akquinet maven-latex-plugin project
 *
 * Copyright (c) 2011 by akquinet tech@spree GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.m2latex.core;

import java.io.File;

import java.util.SortedSet;
import java.util.HashSet;
import java.util.TreeSet;


/**
 * The settings for a maven plugin and for an ant task. 
 * These are the elements of the maven pom in element <code>settings</code> 
 * and accordingly for the ant build file. 
 */
public class Settings
{

    // static initializer 

    /**
     * On unix <code>src/site/tex</code>, 
     * on other operating systems accordingly. 
     */
    final static String SST;

    static {
	String fs = System.getProperty("file.separator");
	SST = "src" + fs + "site" + fs + "tex";
    }

    // readonly parameters 

    /**
     * The base directory of this maven project. 
     *
     * @see AbstractLatexMojo#baseDirectory
     */
    private File baseDirectory;

    /**
     * The target directory of this maven project. 
     * By default this is <code>{@link #baseDirectory}/target</code> 
     * on Unix systems. 
     *
     * @see AbstractLatexMojo#targetDirectory
     */
    private File targetDirectory;

    /**
     * The target site directory of this maven project. 
     * By default this is <code>{@link #targetDirectory}/site</code> 
     * on Unix systems. 
     *
     * @see AbstractLatexMojo#targetSiteDirectory
     */
    private File targetSiteDirectory;

    // read/write parameters and related. 
    // If a parameter represents a relative path, this is a string 
    // and there is an according field of type File. 

    /**
     * The tex source directory as a string, containing 
     * all tex main documents (including subfolders) to be processed
     * relative to {@link #baseDirectory}. 
     * The default value is {@link #SST}. 
     * The according file is given by {@link #texSrcDirectoryFile}. 
     *
     * @parameter
     */
    private String texSrcDirectory = SST;

    /**
     * File for {@link #texSrcDirectory} based on {@link #baseDirectory}. 
     */
    private File texSrcDirectoryFile = new File(this.baseDirectory, 
						this.texSrcDirectory);

    /**
     * The working directory as a string, 
     * for temporary files and LaTeX processing 
     * relative to {@link #targetDirectory}. 
     * <p>
     * First, the tex-souces are copied recursively 
     * from {@link #texSrcDirectory} to this directory, 
     * then they are processed, the results stored in this directory 
     * and finally, the resulting files are copied to {@link #outputDirectory}.
     * <p>
     * The default value is <code>m2latex</code>. 
     * The according file is given by {@link #tempDirectoryFile}. 
     *
     * @parameter
     */
    private String tempDirectory = "m2latex";

    /**
     * File for {@link #tempDirectory} based on {@link #targetDirectory}. 
     */
    private File tempDirectoryFile = new File(this.targetDirectory,
					      this.tempDirectory);

    /**
     * The artifacts generated by {@link #texCommand} 
     * will be copied to this folder 
     * which is given relative to {@link #targetSiteDirectory}. 
     * The default value is <code>.</code>. 
     * The according file is given by {@link #outputDirectoryFile}. 
     *
     * @parameter
     */
    private String outputDirectory = ".";

    /**
     * File for {@link #outputDirectory} based on {@link #targetSiteDirectory}. 
     */
    private File outputDirectoryFile = new File(this.targetSiteDirectory, 
						this.outputDirectory);

    /**
     * A comma separated list of targets 
     * returned as a set by {@link #getTargetSet()}. 
     * For allowed values see {@link Targets}. 
     * The default value is <code>pdf, html</code>. 
     *
     * @parameter
     */
    private String targets = "pdf, html";


    /**
     * The pattern which identifies a latex main file. 
     * The default value is <code>\s*\\(documentstyle|documentclass).*</code>. 
     * If this is not appropriate, please modify 
     * and notify the developer of this plugin. 
     *
     * @parameter
     */
    private String patternLatexMainFile = 
	"\\s*\\\\(documentstyle|documentclass).*";


    // texPath, commands and arguments 

    /**
     * Path to the TeX scripts or <code>null</code>. 
     * In the latter case, the scripts must be on the system path. 
     * Note that in the pom, <code>&lt;texPath/&gt;</code> 
     * and even <code>&lt;texPath&gt;    &lt;/texPath&gt;</code> 
     * represent the <code>null</code>-File. 
     * The default value is <code>null</code>. 
     *
     * @parameter
     */
    private File texPath = null;

    /**
     * The fig2dev command for conversion of fig-files 
     * into various formats. 
     * Currently only pdf combined with pdf_t is supported. 
     * The default value is <code>fig2dev</code>. 
     *
     * @parameter
     */
    private String fig2devCommand = "fig2dev";

    /**
     * The LaTeX command to create a pdf-file. 
     * Possible values are e.g. <code>pdflatex</code>, <code>lualatex</code> 
     * and <code>xelatex</code>. 
     * The default value (for which this software is also tested) 
     * is <code>pdflatex</code>. 
     * FIXME: unintuitive name. 
     *
     * @parameter
     */
    private String texCommand = "pdflatex";

    /**
     * The arguments string to use when calling latex via {@link #texCommand}. 
     * Leading and trailing blanks are ignored. 
     * The setter method {@link #setTexCommandArgs(String)} ensures, 
     * that exactly one blank separate the proper options. 
     * The default value is 
     * <code>-interaction=nonstopmode -src-specials -recorder</code>. 
     *
     * @parameter
     */
    private String texCommandArgs = 
	"-interaction=nonstopmode -src-specials -recorder -shell-escape ";

    /**
     * The pattern in the <code>log</code> file 
     * indicating a failure when running {@link #texCommand}. 
     * The default value is <code>(^! )</code> (note the space). 
     * If this is not appropriate, please modify 
     * and notify the developer of this plugin. 
     *
     * @parameter
     */
    private String patternErrLatex = "(^! )";

    /**
     * The pattern in the <code>log</code> file 
     * indicating a warning when running {@link #texCommand}. 
     * The default value is 
     * <pre>
     *(^LaTeX Warning: |
     *^LaTeX Font Warning: |
     *^(Package|Class) .+ Warning: |
     *^Missing character: There is no .* in font .*!$|
     *^pdfTeX warning (ext4): destination with the same identifier|
     *^* Font .+ does not contain script |
     *^A space is missing. (No warning).)
     * </pre>. 
     * It is designed to be as complete as possible 
     * while not indicating a warning where not appropriate. 
     * If the current default value is not appropriate, 
     * please overwrite and notify the developer of this plugin. 
     *
     * @parameter
     */
    // Note that there are warnings not indicated by a pattern containing 
    // '[Ww]arning' and that there are warnings declared as 'no warning'. 
    // a few of them I did take into account, some not: 
    // Too much space after a point. (No warning).
    // Bad line break. (No warning).
    // Bad page break. (No warning).
    private String patternWarnLatex = 
	"(^LaTeX Warning: |" +
	"^LaTeX Font Warning: |" + 
	"^(Package|Class) .+ Warning: |" + 
	"^Missing character: There is no .* in font .*!$|" +
	"^pdfTeX warning (ext4): destination with the same identifier|" +
	"^* Font .+ does not contain script |" +
	"^A space is missing. (No warning).)";

    /**
     * Whether debugging of overfull/underfull hboxes/vboxes is on: 
     * If so, a bad box occurs in the last LaTeX run, a warning is displayed. 
     * For details, set $cleanUp to false, 
     * rerun LaTeX and have a look at the log-file. 
     * The default value is <code>true</code>. 
     *
     * @parameter
     */
    private boolean debugBadBoxes = true;

    /**
     * Whether debugging of warnings is on: 
     * If so, a warning in the last LaTeX run is displayed. 
     * For details, set $cleanUp to false, 
     * rerun LaTeX and have a look at the log-file. 
     * The default value is <code>true</code>. 
     *
     * @parameter
     */
    private boolean debugWarnings = true;

    /**
     * The BibTeX command to create a bbl-file 
     * from an aux-file and a bib-file (using a bst-style file). 
     * The default value is <code>bibtex</code>. 
     *
     * @parameter
     */
    private String bibtexCommand = "bibtex";

    // FIXME: Any parameters for bibtex? 
// Usage: bibtex [OPTION]... AUXFILE[.aux]
//   Write bibliography for entries in AUXFILE to AUXFILE.bbl,
//   along with a log file AUXFILE.blg.
// -min-crossrefs=NUMBER  include item after NUMBER cross-refs; default 2
// -terse                 do not print progress reports
// -help                  display this help and exit
// -version               output version information and exit

// how to detect errors/warnings??? 
//Process exited with error(s)

    /**
     * The Pattern in the blg-file 
     * indicating that {@link #bibtexCommand} failed. 
     * The default value is chosen 
     * according to the <code>bibtex</code> documentation. 
     *
     * @parameter
     */
    private String patternErrBibtex = "error message";

    /**
     * The Pattern in the blg-file 
     * indicating a warning {@link #bibtexCommand} emitted. 
     * The default value is chosen 
     * according to the <code>bibtex</code> documentation. 
     *
     * @parameter
     */
    private String patternWarnBibtex = "Warning--";

    /**
     * The MakeIndex command to create an ind-file 
     * from an idx-file logging on an ilg-file. 
     * The default value is <code>makeindex</code>. 
     *
     * @parameter
     */
    private String makeIndexCommand = "makeindex";

    /**
     * The options for the MakeIndex command. 
     * Note that the option <code>-t xxx.ilg</code> 
     * to specify the logging file is not allowed, 
     * because this software uses the standard logging file. 
     * The default value is the empty string. 
     *
     * @parameter
     */
    private String makeIndexOptions = "";

    /**
     * The Pattern in the ilg-file 
     * indicating that {@link #makeIndexCommand} failed. 
     * The default value is chosen 
     * according to the <code>makeindex</code> documentation. 
     *
     * @parameter
     */
    private String patternErrMakeindex = "!! Input index error ";

    /**
     * The Pattern in the ilg-file 
     * indicating a warning {@link #makeIndexCommand} emitted. 
     * The default value is chosen 
     * according to the <code>makeindex</code> documentation. 
     *
     * @parameter
     */
    private String patternWarnMakeindex = "## Warning ";


    /**
     * The tex4ht command. 
     * Possible values are e.g. 
     * <code>htlatex</code> and <code>htxelatex</code>. 
     * The default value (for which this software is also tested) 
     * is <code>htlatex</code>. 
     *
     * @parameter
     */
    private String tex4htCommand = "htlatex";

    /**
     * The options for the <code>tex4ht</code>-style 
     * which creates a dvi-file or a pdf-file 
     * with information to create sgml, 
     * e.g. html or odt or something like that. 
     *
     * @parameter
     */
    private String tex4htStyOptions = "html,2";

    /**
     * The options for <code>tex4ht</code> which extracts information 
     * from a dvi-file or from a pdf-file 
     * into the according lg-file and idv-file producing html-files 
     * and by need and if configured accordingly 
     * svg-files, 4ct-files and 4tc-files and a css-file and a tmp-file.
     * The former two are used by <code>t4ht</code> 
     * which is configured via {@link #t4htOptions}. 
     *
     * @parameter
     */
    private String tex4htOptions = "";

    /**
     * The options for <code>t4ht</code> which converts idv-file and lg-file 
     * into css-files, tmp-file and, 
     * by need and if configured accordingly into png files. 
     * The value <code>-p</code> prevents creation of png-pictures.
     * The default value is the empty string. 
     *
     * @parameter
     */
    private String t4htOptions = "";

    /**
     * The latex2rtf command to create rtf from latex directly. 
     * The default value is <code>latex2rtf</code>. 
     *
     * @parameter
     */
    private String latex2rtfCommand = "latex2rtf";

    // FIXME: provide parameters for latex2rtf 

    /**
     * The odt2doc command to create MS word-formats from otd-files. 
     * The default value is <code>odt2doc</code>; 
     * equivalent here is <code>unoconv</code>. 
     * Note that <code>odt2doc</code> just calls <code>unoconv</code> 
     * with odt-files as input and doc-file as default output. 
     *
     * @see #odt2docOptions
     * @parameter
     */
    private String odt2docCommand = "odt2doc";

    /**
     * The options of the odt2doc command. 
     * Above all specification of output format via the option <code>-f</code>. 
     * Invocation is <code>odt2doc -f&lt;format> &lt;file>.odt</code>. 
     * All output formats are shown by <code>odt2doc --show</code> 
     * but the formats interesting in this context 
     * are <code>doc, doc6, doc95,docbook, docx, docx7, ooxml, rtf</code>. 
     * Interesting also the verbosity options <code>-v, -vv, -vvv</code> 
     * the timeout <code>-T=secs</code> and <code>--preserve</code> 
     * to keep permissions and timestamp of the original document. 
     * The default value is <code>-fdocx</code>. 
     *
     * @see #odt2docCommand
     * @parameter
     */
    private String odt2docOptions = "-fdocx";

    /**
     * The pdf2txt command converting pdf into plain text. 
     * The default value is <code>pdftotext</code>. 
     *
     * @see #pdf2txtOptions
     * @parameter
     */
    private String pdf2txtCommand = "pdftotext";

    /**
     * The options of the pdf2txt command. 
     * The default value is the empty string. 
     *
     * @see #pdf2txtCommand
     * @parameter
     */
    private String pdf2txtOptions = "";


    // rerunning latex 

    /**
     * The pattern in the log file which triggers rerunning latex. 
     * This pattern may never be ensured to be complete, 
     * because any new package may break completeness. 
     * Nevertheless, the default value aims completeness 
     * while be restrictive enough not to trigger another latex run 
     * if not needed. 
     * Note that the log file may contain text from the tex file, 
     * e.g. if warning for an overfull hbox. 
     * Thus the pattern must be restrictive enough 
     * to avoid false rerun warning 
     * e.g. caused by occurrence of the word 'rerun'. 
     * The default value is quite complex. 
     * <p>
     * To ensure termination, let {@link #maxNumReruns} 
     * specify the maximum number of latex runs. 
     * If the user finds an extension, (s)he is asked to contribute 
     * and to notify the developer of this plugin. 
     * Then the default value will be extended. 
     * FIXME: default? to be replaced by an array of strings? **** 
     *
     * @parameter
     */
   private String patternLatexNeedsReRun = 
       // general message 
       "(^LaTeX Warning: Label\\(s\\) may have changed. " 
       + "Rerun to get cross-references right.$|" +
       // default message in one line for packages 
       "^Package \\w+ Warning: .*Rerun .*$|" +
       // works for 
       // Package totcount Warning: Rerun to get correct total counts
       // Package longtable Warning: Table widths have changed. Rerun LaTeX ...
       // Package hyperref Warning: Rerun to get outlines right (old hyperref)
       // ... 
       // default message in two lines for packages 
       "^Package \\w+ Warning: .*$"
       + "^\\(\\w+\\) .*Rerun .*$|" +
       // works for 
       // Package natbib Warning: Citation\\(s\\) may have changed.
       // (natbib)                Rerun to get citations correct.
       // Package Changebar Warning: Changebar info has changed.
       // (Changebar)                Rerun to get the bars right
       //
       // messages specific to various packages 
       "^LaTeX Warning: Etaremune labels have changed.$|" +
       // 'Rerun to get them right.' is on the next line
       //
       // from package rerunfilecheck used by other packages like new hyperref 
       // Package rerunfilecheck Warning: File `foo.out' has changed.
       "^\\(rerunfilecheck\\)                Rerun to get outlines right$)";
       //  (rerunfilecheck)                or use package `xxx'.

    /**
     * The maximal allowed number of reruns of the latex process. 
     * This is to avoid endless repetitions. 
     * The default value is 5. 
     * This shall be non-negative 
     * or <code>-1</code> which signifies that there is no threshold. 
     *
     * @parameter
     */
    private int maxNumReruns = 5;


    // cleanup 

    /**
     * Clean up the working directory in the end? 
     * May be used for debugging when setting to <code>false</code>. 
     * The default value is <code>true</code>. 
     *
     * @parameter
     */
    private boolean cleanUp = true;

    // errors and warnings 



    // getter methods partially implementing default values. 


    private File getBaseDirectory() {
        return this.baseDirectory;
    }

    private File getTargetDirectory() {
        return this.targetDirectory;
    }

    private File getTargetSiteDirectory() {
        return this.targetSiteDirectory;
    }


    public File getTexSrcDirectoryFile() {
	return this.texSrcDirectoryFile;
    }

    public File getTempDirectoryFile() {
	return this.tempDirectoryFile;
    }

    public File getOutputDirectoryFile() {
       return this.outputDirectoryFile;
    }

    public SortedSet<Target> getTargetSet() {
	String[] targetSeq = this.targets.split(" *, *");
	// TreeSet is sorted. maybe this determines ordering of targets. 
	SortedSet<Target> targetSet = new TreeSet<Target>();
	for (int idx = 0; idx < targetSeq.length; idx++) {
	    targetSet.add(Target.valueOf(targetSeq[idx]));
	}
	return targetSet;
    }

    public String getPatternLatexMainFile() {
	return this.patternLatexMainFile;
    }


    // texPath, commands and arguments 

    public File getTexPath() {
        return this.texPath;
    }

    public String getFig2devCommand() {
        return this.fig2devCommand;
    }

    public String getTexCommand() {
        return this.texCommand;
    }

    public String getTexCommandArgs() {
        return this.texCommandArgs;
    }

    public String getPatternErrLatex() {
	return this.patternErrLatex;
    }

    public String getPatternWarnLatex() {
	return this.patternWarnLatex;
    }

    public boolean getDebugBadBoxes() {
	return this.debugBadBoxes;
    }

    public boolean getDebugWarnings() {
	return this.debugWarnings;
    }



    public String getBibtexCommand() {
        return this.bibtexCommand;
    }

    public String getPatternErrBibtex() {
	return this.patternErrBibtex;
    }

    public String getPatternWarnBibtex() {
	return this.patternWarnBibtex;
    }




    public String getMakeIndexCommand() {
	return this.makeIndexCommand;
    }

    public String getMakeIndexOptions() {
	return this.makeIndexOptions;
    }

    public String getPatternErrMakeindex() {
	return this.patternErrMakeindex;
    }

    public String getPatternWarnMakeindex() {
	return this.patternWarnMakeindex;
    }

    public String getTex4htCommand() {
        return this.tex4htCommand;
    }

    public String getTex4htOptions() {
        return this.tex4htOptions;
    }

    public String getTex4htStyOptions() {
        return this.tex4htStyOptions;
    }

    public String getT4htOptions() {
        return this.t4htOptions;
    }

    public String getLatex2rtfCommand() {
        return this.latex2rtfCommand;
    }

    public String getOdt2docCommand() {
        return this.odt2docCommand;
    }

    public String getOdt2docOptions() {
	return this.odt2docOptions;
    }

    public String getPdf2txtCommand() {
        return this.pdf2txtCommand;
    }

    public String getPdf2txtOptions() {
        return this.pdf2txtOptions;
    }

    public boolean isCleanUp() {
        return this.cleanUp;
    }


    public String getPatternLatexNeedsReRun() {
	return this.patternLatexNeedsReRun;
    }

    public int getMaxNumReruns() {
	return this.maxNumReruns;
    }

    // setter methods 

   /**
     * Setter method for {@link #baseDirectory} 
     * influencing also {@link #texSrcDirectoryFile}. 
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
	this.texSrcDirectoryFile = new File(this.baseDirectory, 
					    this.texSrcDirectory);
    }

    /**
     * Setter method for {@link #targetDirectory} 
     * influencing also {@link #tempDirectoryFile}. 
     */
    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
	this.tempDirectoryFile = new File(this.targetDirectory,
					  this.tempDirectory);
    }

    /**
     * Setter method for {@link #targetSiteDirectory} 
     * influencing also {@link #outputDirectoryFile}. 
     */
    public void setTargetSiteDirectory(File targetSiteDirectory) {
        this.targetSiteDirectory = targetSiteDirectory;
	this.outputDirectoryFile = new File(this.targetSiteDirectory, 
					    this.outputDirectory);
    }

    /**
     * Sets {@link #texSrcDirectory} and updates {@link #texSrcDirectoryFile}. 
     */
    public void setTexSrcDirectory(String texSrcDirectory) {
        this.texSrcDirectory = texSrcDirectory;
	this.texSrcDirectoryFile = new File(this.baseDirectory, 
					    this.texSrcDirectory);
    }

    /**
     * Sets {@link #tempDirectory} and updates {@link #tempDirectoryFile}. 
     */
    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
	this.tempDirectoryFile = new File(this.targetDirectory,
					  this.tempDirectory);
    }

    /**
     * Sets {@link #outputDirectory} and updates {@link #outputDirectoryFile}. 
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
	this.outputDirectoryFile = new File(this.targetSiteDirectory, 
					    this.outputDirectory);
    }

    public void setTargets(String targets) {
	this.targets = targets.trim();
    }

    // setter method for patternLatexMainFile in maven 
    public void setPatternLatexMainFile(String patternLatexMainFile) {
	this.patternLatexMainFile = patternLatexMainFile;
    }

    // method introduces patternLatexMainFile in ant 
    public PatternLatexMainFile createPatternLatexMainFile() {
   	return new PatternLatexMainFile();
    }

    // defines patternLatexMainFile element with text in ant 
    public class PatternLatexMainFile {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternLatexMainFile(pattern);
   	}
    }

    public void setTexPath(File texPath) {
        this.texPath = texPath;
    }

    public void setFig2devCommand(String fig2devCommand) {
        this.fig2devCommand = fig2devCommand;
    }

    public void setTexCommand(String texCommand) {
        this.texCommand = texCommand;
    }

    /**
     * Sets the argument string of the latex command 
     * given by {@link #texCommand}. 
     * It is ensured that {@link #texCommandArgs} 
     * consist of proper options separated by a single blank. 
     *
     * @param args
     *    The arguments string to use when calling LaTeX 
     *    via {@link #texCommand}. 
     *    Leading and trailing blank and newline are ignored. 
     *    Proper arguments are separated by blank and newline. 
     */
    public void setTexCommandArgs(String args) {
        this.texCommandArgs = args.replace("( \n)+", " ").trim();
    }



    public void setBibtexCommand(String bibtexCommand) {
        this.bibtexCommand = bibtexCommand;
    }

    // setter method for patternErrBibtex in maven 
    public void setPatternErrBibtex(String patternErrBibtex) {
        this.patternErrBibtex = patternErrBibtex;
    }

    // method introduces patternErrBibtex in ant 
    public PatternErrBibtex createPatternErrBibtex() {
   	return new PatternErrBibtex();
    }

    // defines patternErrBibtex element with text in ant 
    public class PatternErrBibtex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternErrBibtex(pattern);
   	}
    }

    // setter method for patternWarnBibtex in maven 
    public void setPatternWarnBibtex(String patternWarnBibtex) {
        this.patternWarnBibtex = patternWarnBibtex;
    }

    // method introduces patternWarnBibtex in ant 
    public PatternWarnBibtex createPatternWarnBibtex() {
   	return new PatternWarnBibtex();
    }

    // defines patternWarnBibtex element with text in ant 
    public class PatternWarnBibtex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternWarnBibtex(pattern);
   	}
    }





    public void setMakeIndexCommand(String makeIndexCommand) {
        this.makeIndexCommand = makeIndexCommand;
    }

    public void setMakeIndexOptions(String makeIndexOptions) {
	this.makeIndexOptions = makeIndexOptions.replace("( \n)+", " ").trim();
    }

    // setter method for patternErrMakeindex in maven 
    public void setPatternErrMakeindex(String patternErrMakeindex) {
        this.patternErrMakeindex = patternErrMakeindex;
    }

    // method introduces patternErrMakeindex in ant 
    public PatternErrMakeindex createPatternErrMakeindex() {
   	return new PatternErrMakeindex();
    }

    // defines patternErrMakeindex element with text in ant 
    public class PatternErrMakeindex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternErrMakeindex(pattern);
   	}
    }

    // setter method for patternWarnMakeindex in maven 
    public void setPatternWarnMakeindex(String patternWarnMakeindex) {
        this.patternWarnMakeindex = patternWarnMakeindex;
    }

    // method introduces patternWarnMakeindex in ant 
    public PatternWarnMakeindex createPatternWarnMakeindex() {
   	return new PatternWarnMakeindex();
    }

    // defines patternWarnMakeindex element with text in ant 
    public class PatternWarnMakeindex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternWarnMakeindex(pattern);
   	}
    }


    public void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    // setter method for patternErrLatex in maven 
    public void setPatternErrLatex(String patternErrLatex) {
	this.patternErrLatex = patternErrLatex;
    }

    // method introduces patternErrLatex in ant 
    public PatternErrLatex createPatternErrLatex() {
   	return new PatternErrLatex();
    }

    // defines patternErrLatex element with text in ant 
    public class PatternErrLatex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternErrLatex(pattern);
   	}
    }


    // setter method for patternWarnLatex in maven 
    public void setPatternWarnLatex(String patternWarnLatex) {
	this.patternWarnLatex = patternWarnLatex;
    }

    // method introduces patternWarnLatex in ant 
    public PatternWarnLatex createPatternWarnLatex() {
   	return new PatternWarnLatex();
    }

    // defines patternWarnLatex element with text in ant 
    public class PatternWarnLatex {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternWarnLatex(pattern);
   	}
    }




    public void setDebugBadBoxes(boolean debugBadBoxes) {
	this.debugBadBoxes = debugBadBoxes;
    }

    public void setDebugWarnings(boolean debugWarnings) {
	this.debugWarnings = debugWarnings;
    }


    public void setLatex2rtfCommand(String latex2rtfCommand) {
        this.latex2rtfCommand = latex2rtfCommand;
    }

    public void setOdt2docCommand(String odt2docCommand) {
        this.odt2docCommand = odt2docCommand;
     }

    public void setOdt2docOptions(String odt2docOptions) {
        this.odt2docOptions = odt2docOptions.replace("( \n)+", " ").trim();
     }

    public void setPdf2txtCommand(String pdf2txtCommand) {
        this.pdf2txtCommand = pdf2txtCommand;
    }

    public void setPdf2txtOptions(String pdf2txtOptions) {
        this.pdf2txtOptions = pdf2txtOptions.replace("( \n)+", " ").trim();
    }

    public void setTex4htCommand(String tex4htCommand) {
        this.tex4htCommand = tex4htCommand;
    }

    public void setTex4htStyOptions(String tex4htStyOptions) {
	this.tex4htStyOptions = tex4htStyOptions;
   }

     public void setTex4htOptions(String tex4htOptions) {
	this.tex4htOptions = tex4htOptions;
    }

     public void setT4htOptions(String t4htOptions) {
	this.t4htOptions = t4htOptions;
    }

    // setter method for patternLatexNeedsReRun in maven 
    public void setPatternLatexNeedsReRun(String pattern) {
	this.patternLatexNeedsReRun = pattern;
    }

    // method introduces patternLatexNeedsReRun in ant 
    public PatternLatexNeedsReRun createPatternLatexNeedsReRun() {
   	return new PatternLatexNeedsReRun();
    }

    // defines patternNeedAnotherLatexRun element with text in ant 
    public class PatternLatexNeedsReRun {
	// FIXME: this is without property resolution. 
	// to add this need  pattern = getProject().replaceProperties(pattern)
	// with Task.getProject() 
   	public void addText(String pattern) {
   	    Settings.this.setPatternLatexNeedsReRun(pattern);
   	}
    }

    public void setMaxNumReruns(int maxNumReruns) {
	assert maxNumReruns >= -1;
	this.maxNumReruns = maxNumReruns;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
	// directories 
	sb.append("[ baseDirectory=")   .append(this.baseDirectory);
	sb.append(", targetDirectory=") .append(this.targetDirectory);
	sb.append(", targetSiteDirectory=") .append(this.targetSiteDirectory);
	sb.append(", texSrcDirectory=") .append(this.texSrcDirectory);
	sb.append(", tempDirectory=")   .append(this.tempDirectory);
 	sb.append(", outputDirectory=") .append(this.outputDirectory);

 	sb.append(", targets=")         .append(this.targets);
 	sb.append(", =patternLatexMainFile").append(this.patternLatexMainFile);

        sb.append(", texPath=")         .append(this.texPath);
        sb.append(", fig2devCommand=")  .append(this.fig2devCommand);
        sb.append(", texCommand=")      .append(this.texCommand);
	sb.append(", texCommandArgs=")  .append(this.texCommandArgs);
	sb.append(", patternErrLatex=") .append(this.patternErrLatex);
	sb.append(", patternWarnLatex=").append(this.patternWarnLatex);
 	sb.append(", debugBadBoxes=")   .append(this.debugBadBoxes);
 	sb.append(", debugWarnings=")   .append(this.debugWarnings);

        sb.append(", bibtexCommand=")   .append(this.bibtexCommand);
        sb.append(", patternErrBibtex=").append(this.patternErrBibtex);
        sb.append(", patternWarnBibtex=").append(this.patternWarnBibtex);

        sb.append(", makeIndexCommand=").append(this.makeIndexCommand);
        sb.append(", makeIndexOptions=").append(this.makeIndexOptions);
        sb.append(", patternErrMakeindex=").append(this.patternErrMakeindex);
        sb.append(", patternWarnMakeindex=").append(this.patternWarnMakeindex);

        sb.append(", tex4htCommand=")   .append(this.tex4htCommand);
        sb.append(", tex4htStyOptions=").append(this.tex4htStyOptions);
        sb.append(", tex4htOptions=")   .append(this.tex4htOptions);
	sb.append(", t4htOptions=")     .append(this.t4htOptions);

        sb.append(", latex2rtfCommand=").append(this.latex2rtfCommand);
        sb.append(", odt2docCommand=")  .append(this.odt2docCommand);
        sb.append(", odt2docOptions=")  .append(this.odt2docOptions);
        sb.append(", pdf2txtCommand=")  .append(this.pdf2txtCommand);
        sb.append(", pdf2txtOptions=")  .append(this.pdf2txtOptions);
	sb.append(", patternLatexNeedsReRun=")
	    .append(this.patternLatexNeedsReRun);
	sb.append(", maxNumReruns=").append(this.maxNumReruns);
	sb.append(", cleanUp=").append(this.cleanUp);
        sb.append(']');
        return sb.toString();
    }

    public static void main(String[] args) {
	System.out.println("texpath: "+new Settings().getTexPath());
	
    }

}
