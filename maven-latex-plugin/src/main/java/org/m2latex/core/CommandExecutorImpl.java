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

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;

/**
 * Execution of an executable with given arguments 
 * in a given working directory logging on {@link #log}. 
 */
class CommandExecutorImpl implements CommandExecutor {

    private final LogWrapper log;

    CommandExecutorImpl( LogWrapper log )
    {
        this.log = log;
    }

    /**
     * Execute <code>executable</code> with arguments <code>args</code> 
     * in the working directory <code>workingDir</code>. 
     * Here, <code>pathToExecutable</code> is the path 
     * to the executable. May be null? 
     *
     * @param workingDir
     *    the working directory. 
     *    The shell changes to that directory 
     *    before invoking <code>executable</code> 
     *    with arguments <code>args</code>. 
     * @param pathToExecutable
     *    the path to the executable <code>executable</code>. 
     *    This may be <code>null</code> if <code>executable</code> 
     *    is on the execution path 
     * @param executable
     *    the name of the program to be executed 
     * @param args
     *    the list of arguments, 
     *    each containing a blank enclosed in double quotes. 
     * @throws BuildExecutionException
     *    if invocation of <code>executable</code> fails. 
     */
    public final String execute(File workingDir, 
				File pathToExecutable, 
				String executable, 
				String[] args)throws BuildExecutionException {
	String command = new File(pathToExecutable, executable).getPath();
	Commandline cl = new Commandline(command);
	cl.getShell().setQuotedArgumentsEnabled(false);
	cl.addArguments(args);
	cl.setWorkingDirectory(workingDir.getPath());
	StringStreamConsumer output = new StringStreamConsumer();
	log.debug("Executing: " + cl + " in: " + workingDir );

        try {
	    // may throw CommandLineException 
	    CommandLineUtils.executeCommandLine(cl, output, output);
	} catch (CommandLineException e) {
	    throw new BuildExecutionException("Error executing command", e);
        }

	log.debug("Output:\n" + output.getOutput() + "\n");
	return output.getOutput();
    }
}
