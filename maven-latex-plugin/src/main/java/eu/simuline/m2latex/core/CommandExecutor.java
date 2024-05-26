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

package eu.simuline.m2latex.core;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import        org.codehaus.plexus.util.cli.CommandLineException;
import        org.codehaus.plexus.util.cli.Commandline;// constructor
import static org.codehaus.plexus.util.cli.CommandLineUtils.executeCommandLine;
import        org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;

/**
 * Execution of an executable with given arguments 
 * in a given working directory logging on {@link #log}. 
 * Sole interface to <code>org.codehaus.plexus.util.cli</code>. 
 */
class CommandExecutor {

  /**
   * Represents the result of the invocation of a command 
   * consisting of the {@link #output} and its {@link #returnCode}. 
   * In addition the {@link CommandExecutor.ReturnCodeChecker} 
   * given by {@link #checker} plays a role to determine 
   * whether the return code signifies success, 
   * which is returned by {@link #getSuccess()}. 
   */
  static class CmdResult {
    final String output;
    final private ReturnCodeChecker checker;
    final int returnCode;

    CmdResult(String output, ReturnCodeChecker checker, int returnCode) {
      this.output = output;
      this.checker = checker;
      this.returnCode = returnCode;
    }

    boolean getSuccess() {
      return !this.checker.hasFailed(this.returnCode);
    }

    public String toString() {
      StringBuilder res = new StringBuilder();
      res.append("<CmdResult>");
      res.append("\nreturnCode" + this.returnCode);
      res.append("\nsuccess" + this.getSuccess());
      res.append("\n</CmdResult>");
      return res.toString();
    }
  } // class CmdResult 

  /**
   * The way return codes are checked: Not at all, if nonzero and special treatments. 
   * This is used in {@link CommandExecutor#execute(File, File, String, ReturnCodeChecker, String[])} 
   * to decide whether the return code shall indicate that execution failed. 
   * TBD: shall be part of category 
   */
  enum ReturnCodeChecker {
    /**
     * Never detect fail of execution. 
     */
    Never {
      boolean hasFailed(int returnCode) {
        return false;
      }
    },
    /**
     * Detect fail of execution if return code is nonzero. 
     */
    IsNonZero {
      boolean hasFailed(int returnCode) {
        return returnCode != 0;
      }
    },
    /**
     * Detect fail of execution only if return code is 1. 
     * <p>
     * Currently used for chk only. 
     * Its return values are really strange: 
     * <ul>
     * <li>1 if an error in execution occurs, 
     *     e.g. option -neee although -n requires a number. </li>
     * <li>3 if an error was found except if case 1 occurs. 
     *     Note that all findings are warnings 
     *     if not configured as errors with -exx, xx a number. </li>
     * <li>2 if a warning was found, except if one of the above cases occur. 
     *     one can deactivate always. </li>
     * <li>0 if neither of the above occurred. 
     *     Note that still warnings could be given but deactivated, 
     *     e.g. excluded linewise. </li>
     * </ul>
     */
    IsOne {
      boolean hasFailed(int returnCode) {
        return returnCode == 1;
      }
    },
    /**
     * Detect fail of execution if return code is neither 0 nor 1. 
     * <p>
     * Currently used for diff only. 
     * It is applicable to the diff tool: 0 same, 1 difference, 2 trouble. 
     * Unfortunately diff-pdf-visually has encoding 0 same, 2 difference, 1 trouble. 
     * Thus it is not directly usable, only via a wrapper exchanging 1 and 3
     */
    IsNotZeroOrOne {
      boolean hasFailed(int returnCode) {
        return returnCode == 1;
      }
    };

    abstract boolean hasFailed(int returnCode);
  } // enum ReturnCodeChecker 


  /**
   * Maps names of environment variables to values. 
   * Currently sets <code>SOURCE_DATE_EPOCH</code> 
   * to the number of seconds since 1970-01-01 midnight 
   * and <code>FORCE_SOURCE_DATE=1</code> 
   * forcing that setting. 
   * <p>
   * This is used to run commands at system time 
   * given by the target file which is the base for reproducibility. 
   */
  private static final Map<String, String> TIMESTAMP_ENV;

  /**
   * The key for {@link #TIMESTAMP_ENV} 
   * to set <code>SOURCE_DATE_EPOCH</code> 
   */
  private static final String DATE_EPOCH = "SOURCE_DATE_EPOCH";

  static {
    TIMESTAMP_ENV = new TreeMap<String, String>();
    TIMESTAMP_ENV.put("FORCE_SOURCE_DATE","1");
  }

  /**
   * Indicates the optional timestamp for the next invocation of 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[])}. 
   * If a timestamp is given, 
   * the next invocation is done with environment given by {@link #TIMESTAMP_ENV}; 
   * else without this environment. 
   * Initially, no timestamp is given. 
   */
  private Optional<Long> timestampOpt;

  private final LogWrapper log;

  /**
   * Creates an executor with the given logger 
   * and initial invocation without given timestamp 
   * (see {@link #timestampOpt}). 
   *
   * @param log
   *    the current logger. 
   */
  CommandExecutor(LogWrapper log) {
    this.timestampOpt = Optional.empty();
    this.log = log;
  }

  /**
   * Executes <code>command</code> in <code>workingDir</code>
   * with list of arguments given by <code>args</code> 
   * and logs if one of the expected target files 
   * given by <code>resFile</code> is not newly created, 
   * i.e. if it does not exist or is not updated. 
   * This is a convenience method of 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[], File... )}, 
   * where the boolean signifies whether the return code is checked. 
   * This is set to <code>true</code> in this method. 
   * <p>
   * Supports timeless execution as described in 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[], File...)}. 
   * <p>
   * Logging: 
   * <ul>
   * <li> EEX01: return code other than 0. 
   * <li> EEX02: no target file
   * <li> EEX03: target file not updated
   * <li> WEX04: cannot read target file
   * <li> WEX05: may emit false warnings
   * </ul>
   *
   * @param workingDir
   *    the working directory or <code>null</code>. 
   *    The shell changes to that directory 
   *    before invoking <code>command</code> 
   *    with arguments <code>args</code> if this is not <code>null</code>. 
   *    Argument <code>null</code> is allowed only 
   *    if no result files are given by <code>resFile</code>. 
   *    Essentially this is just needed to determine the version. 
   * @param pathToExecutable
   *    the path to the executable <code>command</code>. 
   *    This may be <code>null</code> if <code>command</code> 
   *    is on the execution path 
   * @param command
   *    the name of the program to be executed 
   * @param args
   *    the list of arguments, 
   *    each containing a blank enclosed in double quotes. 
   * @param resFiles
   *    optional result files, i.e. target files which shall be updated 
   *    by this command. 
   * @return
   *    the output of the command which comprises the output stream 
   *    and whether the return code is nonzero, i.e. the command succeeded. 
   *    The io stream is used in tests only whereas the return code is used for pdfdiffs. 
   * @throws BuildFailureException
   *    TEX01 if invocation of <code>command</code> fails very basically: 
   *    <ul>
   *    <li><!-- see Commandline.execute() -->
   *    the file expected to be the working directory 
   *    does not exist or is not a directory. 
   *    <li><!-- see Commandline.execute() -->
   *    {@link Runtime#exec(String, String[], File)} fails 
   *    throwing an {@link java.io.IOException}. 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemOut parser occurs 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemErr parser occurs 
   *    <li> Wrapping an {@link InterruptedException} 
   *    on the process to be executed thrown by {@link Process#waitFor()}. 
   *    </ul>
   */
  CmdResult execute(File workingDir,
                    File pathToExecutable,
                    String command,
                    String[] args,
                    File... resFiles) throws BuildFailureException {
    return execute(workingDir, pathToExecutable, command, 
          ReturnCodeChecker.IsNonZero, args, resFiles);
  }


  /**
   * Executes <code>command</code> in <code>workingDir</code>
   * with list of arguments given by <code>args</code> 
   * and logs if after execution the result file <code>resFile</code> does not exist. 
   * CAUTION: In contrast to 
   * {@link #execute(File,File,String,String[],File...)}, 
   * It is not checked that the result files are updated 
   * and it is just one result file neglecting log files and that like. 
   * This method is suited to build tools updateing only by need 
   * and currently it is only used for <code>latexmk</code> like tools. 
   * <p>
   * Logging: 
   * <ul>
   * <li> EEX01: return code other than 0. 
   * <li> EEX02: no target file
   * </ul>
   *
   * @param workingDir
   *    the working directory or <code>null</code>. 
   *    The shell changes to that directory 
   *    before invoking <code>command</code> 
   *    with arguments <code>args</code> if this is not <code>null</code>. 
   *    Argument <code>null</code> is allowed only 
   *    if no result files are given by <code>resFile</code>. 
   *    Essentially this is just needed to determine the version. 
   * @param pathToExecutable
   *    the path to the executable <code>command</code>. 
   *    This may be <code>null</code> if <code>command</code> 
   *    is on the execution path 
   * @param command
   *    the name of the program to be executed 
   * @param args
   *    the list of arguments, 
   *    each containing a blank enclosed in double quotes. 
   * @param resFile
   *    a result file which must exist after this command has been processed. 
   *    It need  which shall be updated 
   *    by this command. 
   * @return
   *    the output of the command which comprises the output stream 
   *    and whether the return code is nonzero, i.e. the command succeeded. 
   *    The io stream is used in tests only whereas the return code is used for pdfdiffs. 
   * @throws BuildFailureException
   *    TEX01 if invocation of <code>command</code> fails very basically: 
   *    <ul>
   *    <li><!-- see Commandline.execute() -->
   *    the file expected to be the working directory 
   *    does not exist or is not a directory. 
   *    <li><!-- see Commandline.execute() -->
   *    {@link Runtime#exec(String, String[], File)} fails 
   *    throwing an {@link java.io.IOException}. 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemOut parser occurs 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemErr parser occurs 
   *    <li> Wrapping an {@link InterruptedException} 
   *    on the process to be executed thrown by {@link Process#waitFor()}. 
   *    </ul>
   */
  CmdResult executeBuild(File workingDir,
                         File pathToExecutable,
                         String command,
                         String[] args,
                         File resFile) throws BuildFailureException {
    CmdResult res = execute(workingDir, pathToExecutable, command, args);
    existsOrErr(command, resFile);
    return res;
  }

  /**
   * Executes <code>command</code> in <code>workingDir</code>
   * with list of arguments given by <code>args</code> 
   * checking the return value via <code>checker</code> 
   * and logs a warning if one of the expected target files 
   * given by <code>resFiles</code> is not guaranteed to be newly created, 
   * or be updated. 
   * <p>
   * Supports timeless execution as described in 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[], File...)}. 
   * <p>
   * Logging: 
   * <ul>
   * <li> EEX01: proper execution failed: 
   *      return code other than 0 and <code>checkReturnCode</code> is set. 
   * <li> EEX02: a target file after execution missing 
   * <li> EEX03: a target file is not updated, 
   *      i.e. timestamp after not later than before. 
   *      This implies the file existed 
   *      before and after execution of <code>command</code> 
   *      and that timestamps were readable before and after. 
   * <li> WEX04: cannot read timestamp of a target file, 
   *      either before execution or after. 
   *      Readability of the timestamp before execution is checked only 
   *      if the file exists and also readability of the timestamp after 
   *      is checked only if the file existed before and after execution. 
   * <li> WEX05: may emit false warnings: if modification times are too close 
   *      and this cannot be corrected by sleeping. 
   * </ul>
   *
   * @param workingDir
   *    the working directory or <code>null</code>. 
   *    The shell changes to that directory 
   *    before invoking <code>command</code> 
   *    with arguments <code>args</code> if this is not <code>null</code>. 
   *    Argument <code>null</code> is allowed only 
   *    if no result files are given by <code>resFile</code>. 
   *    Essentially this is just needed to determine the version. 
   * @param pathToExecutable
   *    the path to the executable <code>command</code>. 
   *    This may be <code>null</code> if <code>command</code> 
   *    is on the execution path 
   * @param command
   *    the name of the program to be executed 
   * @param args
   *    the list of arguments, 
   *    each containing a blank enclosed in double quotes. 
   * @param checker
   *    the checker for the return code 
   *    which decides whether an execution error EEX01 has to be logged. 
   * @param resFiles
   *    optional result files, i.e. target files which shall be updated 
   *    by this command. 
   * @return
   *    the output of the command which comprises the output stream 
   *    and whether the return code is nonzero, i.e. the command succeeded. 
   *    The io stream is used in tests only whereas the return code is used for pdfdiffs. 
   * @throws BuildFailureException
   *    TEX01 if invocation of <code>command</code> fails very basically: 
   *    <ul>
   *    <li><!-- see Commandline.execute() -->
   *    the file expected to be the working directory 
   *    does not exist or is not a directory. 
   *    <li><!-- see Commandline.execute() -->
   *    {@link Runtime#exec(String, String[], File)} fails 
   *    throwing an {@link java.io.IOException}. 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemOut parser occurs 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemErr parser occurs 
   *    <li> Wrapping an {@link InterruptedException} 
   *    on the process to be executed thrown by {@link Process#waitFor()}. 
   *    </ul>
   */
  CmdResult execute(File workingDir,
                    File pathToExecutable,
                    String command,
                    ReturnCodeChecker checker,
                    String[] args,
                    File... resFiles) throws BuildFailureException {
    // analyze old result files 
    //assert resFile.length > 0;

    // determine target files and their timestamps before execution 
    boolean[] existsTarget = new boolean[resFiles.length];
    Long[] lastModifiedTargetMs = new Long[resFiles.length];
    long currentTimeMs = System.currentTimeMillis();
    long minTimePastMs = Long.MAX_VALUE;
    Long modTimeOrNull;
    for (int idx = 0; idx < resFiles.length; idx++) {
      existsTarget[idx] = resFiles[idx].exists();
      if (existsTarget[idx]) {
        // if modification time undetermined: null and emit warning WEX04 
        modTimeOrNull = modTimeOrNull(resFiles[idx]);
        lastModifiedTargetMs[idx] = modTimeOrNull;
        if (modTimeOrNull == null) {
          // Here, already a warning WEX04 has been emitted 
          // also lastModifiedTargetMs[idx] == null 
          continue;
        }
        assert modTimeOrNull <= currentTimeMs;
        // correct even if lastModifiedTarget[idx]==0 
        minTimePastMs = Math.min(minTimePastMs, currentTimeMs - modTimeOrNull);
      }
    }

    // FIXME: this is based on a file system 
    // with modification time in steps of seconds, i.e. 1000ms 
    if (minTimePastMs < 1001) {
      try {
        // 1001 is the minimal span of time to change modification time 
        Thread.sleep(1001 - minTimePastMs);// for update control of target 
      } catch (InterruptedException ie) {
        this.log.warn("WEX05: Update control may emit false warnings. ");
      }
    }

    if (workingDir == null && resFiles.length != 0) {
      throw new IllegalStateException(
          "Working directory shall be determined but was null. ");
    }

    // Proper execution 
    // may throw BuildFailureException TEX01, log warning EEX01 
    CmdResult res =
        execute(workingDir, pathToExecutable, command, checker, args);

    // may log EEX02, EEX03, WEX04 
    for (int idx = 0; idx < resFiles.length; idx++) {
      isUpdatedOrWarn(command, resFiles[idx], existsTarget[idx],
          lastModifiedTargetMs[idx]);
    }

    return res;
  }

  /**
   * Returns the time of modification of this file or <code>null</code> if not readable. 
   * 
   * Warnings: WEX04
   * 
   * @param file
   *    The file to be checked. 
   * @return
   *    the time of modification of this file or <code>null</code> if not readable. 
   */
  Long modTimeOrNull(File file) {
    try {
      // may throw IOException 
      FileTime fTime =
          Files.getLastModifiedTime(file.toPath(), LinkOption.NOFOLLOW_LINKS);
      return fTime.to(TimeUnit.MILLISECONDS);
    } catch (IOException ioe) {
      this.log.warn("WEX04: Cannot read target file '" + file.getName()
      + "'; may be outdated. ");
      return null;
    }
  }

  /**
   * If the given file <code>target</code> does not exist 
   * logs an error EEX02: no target file mentioning <code>command</code>. 
   * Note that no error means only that the file exists, 
   * not that it has been updated. 
   * 
   * @param command
   *    the command that should build <code>target</code> by need. 
   * @param target
   * @return
   *    whether <code>target</code> exists. 
   *    Equivalently, whether no error is logged. 
   */
  private boolean existsOrErr(String command, File target) {
    if (!target.exists()) {
      this.log.error("EEX02: Running " + command + " failed: No target file '"
          + target.getName() + "' written. ");
      return false;
    }
    return true;
  }

  // FIXME: return value nowhere used 
  /**
   * Returns whether the file <code>target</code> could be checked to be updated 
   * by the command named <code>command</code> and 
   * emits a warning <code>EEX03</code> if it has not been updated. 
   * It is invoked only by 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[], File[])} 
   * after the command has been invoked. 
   * The file <code>target</code> is updated if it exists and 
   * either did not exist before according to <code>existedBefore</code> 
   * or has a readable modification time 
   * later than the former modification time <code>lastModifiedBefore</code> 
   * which implies that this has been readable also, i.e. is not <code>null</code>. 
   *
   * Logging: 
   * <ul>
   * <li> EEX02: no target file 
   * <li> EEX03: target file not updated 
   * <li> WEX04: cannot read target file 
   * </ul>
   * 
   * @param command
   *    the name of the program to be executed 
   * @param target
   *    The file to be supervised. 
   * @param existedBefore
   *    Whether the file existed before invoking <code>command</code>. 
   * @param lastModifiedBefore
   *    The time of last modification before invoking <code>command</code> if known; 
   *    else <code>null</code>. 
   * @return
   *    whether <code>target</code> has been updated. 
   */
  private boolean isUpdatedOrWarn(String command,
          File target,
          boolean existedBefore,
          Long lastModifiedBefore) {
    // may emit EEX02
    if (!existsOrErr(command, target)) {
      return false;
    }
    assert target.exists();
    if (!existedBefore) {
      // Here target was updated 
      return true;
    }
    assert existedBefore && target.exists();

    if (lastModifiedBefore == null) {
      // Here, orignal modification time was not readable  
      // warning already emitted 
      return false;
    }

    // if modification time undetermined: null and emit warning WEX04 
    Long lastModifiedAfter = modTimeOrNull(target);//target.lastModified();
    if (lastModifiedAfter == null) {
      // modification time not readable; warning already emitted 
      return false;
    }

    if (lastModifiedAfter <= lastModifiedBefore) {
      assert lastModifiedAfter == lastModifiedBefore;
      this.log.error("EEX03: Running " + command + " failed: Target file '"
          + target.getName() + "' is not updated. ");
      return false;
    }
    return true;
  }

  // TBD: rework: whether present or not. 
  /**
   * Selects a timestamp in <em>milliseconds</em> since 1970-01-01 for the next command execution 
   * by invoking with environment variables according to {@link #TIMESTAMP_ENV} 
   * which is in <em>seconds</em> since  since 1970-01-01. 
   * The execution happens by 
   * direct or indirect invocation of 
   * {@link #execute(File, File, String, ReturnCodeChecker, String[])}. 
   * After executing a command, 
   * the execution method reverts to execution in system time, i.e. without specified timestamp. 
   * <p>
   * The method {@link #setTimestamp(Optional)} is used to execute a latex compiler 
   * a DVI/XDV to PDF converter or a tool like <code>latexmk</code>. 
   * Invocation is always via 
   * {@link #execute(File, File, String, String[], File...)}. 
   * 
   * @param timestampOpt
   *    an optional which is either empty or wraps a timestamp in milliseconds since 1970-01-01. 
   * 
   * @see LatexProcessor#runLatex2dev(LatexMainDesc, LatexDev, Optional)
   * @see LatexProcessor#runLatexmk(LatexMainDesc, Optional)
   * @see LatexProcessor#runDvi2pdf(LatexMainDesc, Optional)
   */
  void setTimestamp(Optional<Long> timestampOpt) {
    this.timestampOpt =timestampOpt;
  }

  /**
   * Execute <code>command</code> with arguments <code>args</code> 
   * in the working directory <code>workingDir</code> 
   * and return the output. 
   * Here, <code>pathToExecutable</code> is the path 
   * to the executable. It may be null. 
   * <p>
   * If {@link #timestampOpt} is set by {@link #setTimestamp(Optional)}, 
   * before invocation of this method, 
   * the environment given by {@link #TIMESTAMP_ENV} is set 
   * ensuring timeless execution. 
   * As a side effect, {@link #timestampOpt} is reset again. 
   * <p>
   * Logging: 
   * EEX01 for return code other than 0. 
   *
   * @param workingDir
   *    the working directory or <code>null</code>.
   *    The shell changes to that directory 
   *    before invoking <code>command</code> 
   *    with arguments <code>args</code> if this is not <code>null</code>.
   * @param pathToExecutable
   *    the path to the executable <code>command</code>. 
   *    This may be <code>null</code> if <code>command</code> 
   *    is on the execution path 
   * @param command
   *    the name of the program to be executed 
   * @param checker
   *    the checker for the return code 
   *    which decides whether an execution error EEX01 has to be logged. 
   * @param args
   *    the list of arguments, 
   *    each containing a blank enclosed in double quotes. 
   * @return
   *    the output of the command which comprises the output stream 
   *    and whether the return code is nonzero, i.e. the command succeeded. 
   * @throws BuildFailureException
   *    TEX01 if invocation of <code>command</code> fails very basically: 
   *    <ul>
   *    <li><!-- see Commandline.execute() -->
   *    the file expected to be the working directory 
   *    does not exist or is not a directory. 
   *    <li><!-- see Commandline.execute() -->
   *    {@link Runtime#exec(String, String[], File)} fails 
   *    throwing an {@link java.io.IOException}. 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemOut parser occurs 
   *    <li> <!-- see CommandLineCallable.call() -->
   *    an error inside systemErr parser occurs 
   *    <li> Wrapping an {@link InterruptedException} 
   *    on the process to be executed thrown by {@link Process#waitFor()}. 
   *    </ul>
   */
  private CmdResult execute(File workingDir,
                            File pathToExecutable,
                            String command,
                            ReturnCodeChecker checker,
                            String[] args) throws BuildFailureException {
    // prepare execution 
    String executable = new File(pathToExecutable, command).getPath();
    Commandline cl = new Commandline(executable);
    cl.getShell().setQuotedArgumentsEnabled(true);
    if (this.timestampOpt.isPresent()) {
      this.log.info("Run with timestamp "
        + new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date(timestampOpt.get()))
        + "(" + this.timestampOpt.get()+ "ms)");
      // the epoch time in timeStampOpt is in ms, 
      // whereas in environment variable DATE_EPOCH it is in s 
      // Note that division is implicitly rounded to 0 
      TIMESTAMP_ENV.put(DATE_EPOCH, Long.toString(this.timestampOpt.get()/1000));
      for (Map.Entry<String, String> entry : TIMESTAMP_ENV.entrySet()) {
        cl.addEnvironment(entry.getKey(), entry.getValue());
      }
      this.timestampOpt = Optional.empty();
    }
    cl.addArguments(args);
    if (workingDir != null) {
      cl.setWorkingDirectory(workingDir.getPath());
    }
    StringStreamConsumer output = new StringStreamConsumer();
    log.debug("Executing: " + cl + " in: " + workingDir + ". ");

    // perform execution and collect results 
    int returnCode = -1;
    try {
      // may throw CommandLineException 
      returnCode = executeCommandLine(cl, output, output);
      if (checker.hasFailed(returnCode)) {
          this.log.error("EEX01: Running " + command + " failed with return code "
        + returnCode + ". ");
      }
    } catch (CommandLineException e) {
      throw new BuildFailureException("TEX01: Error running " + command + ". ",
          e);
    }
    // TBD: what if returnCode=-1 is not overwritten? 
    // how to distinguish from real return code -1? 
    // replace above by Integer returnCode = null;
    // and insert here assert returnCode != null;

    log.debug("Output:\n" + output.getOutput() + "\n");
    // TBD: fix bug: return code based on checker. 
    // also not success but store return code itself 
    return new CmdResult(output.getOutput(), checker, returnCode);
  }
}
