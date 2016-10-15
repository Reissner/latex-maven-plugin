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

import org.m2latex.mojo.MavenLogWrapper;
import org.m2latex.mojo.PdfMojo;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import org.easymock.MockControl;

import org.junit.Test;

public class LatexProcessorTest
{
 
    private MockControl executorCtrl = MockControl
	.createStrictControl( CommandExecutor.class );

    private CommandExecutor executor = (CommandExecutor) executorCtrl.getMock();

    private MockControl fileUtilsCtrl = MockControl
	.createStrictControl( TexFileUtils.class );

    private TexFileUtils fileUtils = (TexFileUtils) fileUtilsCtrl.getMock();

    private Settings settings = new Settings();

    private LogWrapper log = new MavenLogWrapper(new SystemStreamLog());

    private LatexProcessor processor = new LatexProcessor
	( settings, executor, log, fileUtils, new PdfMojo() );

    private File texFile = new File(System.getProperty("tmp.dir"), "test.tex");
    private File auxFile = new File(System.getProperty("tmp.dir"), "test.aux");
    private File logFile = new File(System.getProperty("tmp.dir"), "test.log");

    private File blgFile = new File(System.getProperty("tmp.dir"), "test.blg");
    private File idxFile = new File(System.getProperty("tmp.dir"), "test.idx");
    private File ilgFile = new File(System.getProperty("tmp.dir"), "test.ilg");

    private File tocFile = new File(System.getProperty("tmp.dir"), "test.toc");
    private File lofFile = new File(System.getProperty("tmp.dir"), "test.lof");
    private File lotFile = new File(System.getProperty("tmp.dir"), "test.lot");


    private String[] latexArgsExpected = new String[] {
	 "-interaction=nonstopmode", 
	 "-src-specials", 
	 //"-interaction=nonstopmode -src-specials", 
	this.texFile.getName() 
    };

    private String[] tex2htmlArgsExpected = new String[] {
        this.texFile.getName(),
        "html,2",
        "",
        "",
        "-interaction=nonstopmode -src-specials"
    };

    @Test public void testProcessLatexSimple()
	throws BuildExecutionException {

	// run latex 
        mockRunLatex();

	// run bibtex by need: no 
        mockRunBibtexByNeed(false);

	// run makeIndex by need: no 
	mockRunMakeIndexByNeed(false);

	// determine from presence of toc, lof, lot (and idx and other criteria)
	// whether to rerun latex: no 
	fileUtils.replaceSuffix( texFile, "toc" );
	fileUtilsCtrl.setReturnValue( tocFile );
	fileUtils.replaceSuffix( texFile, "lof" );
	fileUtilsCtrl.setReturnValue( lofFile );
	fileUtils.replaceSuffix( texFile, "lot" );
	fileUtilsCtrl.setReturnValue( lotFile );

	// determine whether to rerun latex: no 
	fileUtils.replaceSuffix( texFile, "log" );
	fileUtilsCtrl.setReturnValue( logFile );
        mockNeedAnotherLatexRun( false );

	// detect bad boxes and warnings: none 
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_OUFULL_HVBOX);
	fileUtilsCtrl.setReturnValue( false );
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_WARNING);
	fileUtilsCtrl.setReturnValue( false );

        replay();

        processor.processLatex2pdf( this.texFile );

        verify();
    }

    @Test public void testProcessLatexWithBibtex()
	throws BuildExecutionException
    {
	// run latex 
        mockRunLatex();

	// run bibtex by need: yes 
        mockRunBibtexByNeed(true);

	// run makeIndex by need: no 
	mockRunMakeIndexByNeed(false);

	// determine from presence of toc, lof, lot (and idx and bibtex)
	// whether to rerun latex: no 
	fileUtils.replaceSuffix( texFile, "toc" );
	fileUtilsCtrl.setReturnValue( tocFile );
	fileUtils.replaceSuffix( texFile, "lof" );
	fileUtilsCtrl.setReturnValue( lofFile );
	fileUtils.replaceSuffix( texFile, "lot" );
	fileUtilsCtrl.setReturnValue( lotFile );

	// to run latex because bibtex had been run 
        mockRunLatex();

	// determine whether to rerun latex and run until no 
	fileUtils.replaceSuffix( texFile, "log" );
	fileUtilsCtrl.setReturnValue( logFile );
        mockNeedAnotherLatexRun( true );
        mockRunLatex();
        mockNeedAnotherLatexRun( false );

	// detect bad boxes and warnings: none 
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_OUFULL_HVBOX);
	fileUtilsCtrl.setReturnValue( false );
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_WARNING);
	fileUtilsCtrl.setReturnValue( false );

        replay();

        processor.processLatex2pdf( this.texFile );

        verify();
    }

   @Test public void testProcessLatex2html() throws BuildExecutionException {

 	// run latex 
        mockRunLatex();

	// run bibtex by need: no 
	mockRunBibtexByNeed(false);

	// run makeIndex by need: no 
	mockRunMakeIndexByNeed(false);

	// determine from presence of toc, lof, lot (and idx and other criteria)
	// whether to rerun latex: no 
	fileUtils.replaceSuffix( texFile, "toc" );
	fileUtilsCtrl.setReturnValue( tocFile );
	fileUtils.replaceSuffix( texFile, "lof" );
	fileUtilsCtrl.setReturnValue( lofFile );
	fileUtils.replaceSuffix( texFile, "lot" );
	fileUtilsCtrl.setReturnValue( lotFile );

	// determine whether to rerun latex: no 
	fileUtils.replaceSuffix( texFile, "log" );
	fileUtilsCtrl.setReturnValue( logFile );
        mockNeedAnotherLatexRun( false );

	// detect bad boxes and warnings: none 
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_OUFULL_HVBOX);
	fileUtilsCtrl.setReturnValue( false );
	fileUtils.matchInFile(logFile, LatexProcessor.PATTERN_WARNING);
	fileUtilsCtrl.setReturnValue( false );

        mockRunLatex2html();

        replay();

        processor.processLatex2html(this. texFile );

        verify();
    }

    private void mockNeedAnotherLatexRun(boolean retVal)
        throws BuildExecutionException
    {
        fileUtils.matchInFile(logFile, 
			      this.settings.getPatternNeedLatexReRun());
        fileUtilsCtrl.setReturnValue( retVal );
    }

    // private void mockNeedBibtexRun(boolean retVal) 
    // 	throws BuildExecutionException
    // {
    //     fileUtils.matchInFile(auxFile, LatexProcessor.PATTERN_NEED_BIBTEX_RUN);
    //     fileUtilsCtrl.setReturnValue( retVal );
    // }

    private void mockRunBibtexByNeed(boolean runBibtex) 
	throws BuildExecutionException {

        fileUtils.replaceSuffix( texFile, "aux" );
        fileUtilsCtrl.setReturnValue( auxFile );
        fileUtils.matchInFile(auxFile, LatexProcessor.PATTERN_NEED_BIBTEX_RUN);
        fileUtilsCtrl.setReturnValue( runBibtex );

	if (!runBibtex) {
	    return;
	}

        executor.execute(texFile.getParentFile(),
			 settings.getTexPath(),
			 settings.getBibtexCommand(),
			 new String[] { auxFile.getPath() } );
        executorCtrl.setMatcher( MockControl.ARRAY_MATCHER );
        executorCtrl.setReturnValue( null );

	fileUtils.replaceSuffix( texFile, "blg" );
	fileUtilsCtrl.setReturnValue( blgFile );

	// fileUtils.matchInFile(blgFile, "Error");
	// fileUtilsCtrl.setReturnValue( false );

	// fileUtils.matchInFile(blgFile, LatexProcessor.PATTERN_WARNING);
	// fileUtilsCtrl.setReturnValue( false );
    }

    private void mockRunMakeIndexByNeed(boolean runMakeIndex) 
	throws BuildExecutionException {

        fileUtils.replaceSuffix( texFile, "idx" );
        fileUtilsCtrl.setReturnValue( idxFile );

	if (!runMakeIndex) {
	    return;
	}


        executor.execute(texFile.getParentFile(),
			 settings.getTexPath(),
			 settings.getMakeIndexCommand(),
			 new String[] { idxFile.getPath() } );
	executorCtrl.setMatcher( MockControl.ARRAY_MATCHER );
        executorCtrl.setReturnValue( null );

	fileUtils.replaceSuffix( idxFile, "ilg" );
	fileUtilsCtrl.setReturnValue( ilgFile );

    }


    private void mockRunLatex() throws BuildExecutionException {
        executor.execute(texFile.getParentFile(),
			 settings.getTexPath(),
			 settings.getTexCommand(),
			 latexArgsExpected );
        executorCtrl.setMatcher( MockControl.ARRAY_MATCHER );
        executorCtrl.setReturnValue( null );

	fileUtils.replaceSuffix( texFile, "log" );
	fileUtilsCtrl.setReturnValue( logFile );
    }

    private void mockRunLatex2html() throws BuildExecutionException {
        executor.execute(texFile.getParentFile(),
			 settings.getTexPath(),
			 settings.getTex4htCommand(),
			 tex2htmlArgsExpected );
        executorCtrl.setMatcher( MockControl.ARRAY_MATCHER );
        executorCtrl.setReturnValue( null );

	fileUtils.replaceSuffix( texFile, "log" );
	fileUtilsCtrl.setReturnValue( logFile );

	fileUtils.matchInFile(logFile, this.settings.getPatternErrLatex());
	fileUtilsCtrl.setReturnValue( false );
    }

    private void replay()
    {
        executorCtrl.replay();
        fileUtilsCtrl.replay();
    }

    private void verify()
    {
        executorCtrl.verify();
        fileUtilsCtrl.verify();
    }
}
