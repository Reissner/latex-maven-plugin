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

package org.m2latex.mojo;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Build documents in msword formats, above all docx from LaTeX sources.
 *
 * @goal txt
 * @phase site
 */
//@Mojo( name = "txt")
public class TxtMojo extends AbstractLatexMojo {

    private final static String[] TXT_OUTPUT_FILES = new String[] {".txt"};

    // implements AbstractLatexMojo#processSource(File)
    void processSource(File texFile) throws MojoExecutionException {
	this.latexProcessor.processLatex2txt(texFile);
    }

    // implements AbstractLatexMojo#getOutputFileSuffixes()
    String[] getOutputFileSuffixes() {
	return TXT_OUTPUT_FILES;
    }
}
