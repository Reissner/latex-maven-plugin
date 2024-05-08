package eu.simuline.m2latex.core;

/**
 * Enumerates the names of the parameters of a latex main file. 
 * All these must occur as names 
 * of named capturing groups in {@link Settings#patternLatexMainFile}. 
 * Currently, only one, {@link #docClass} must be matched by the pattern. 
 * Part of the names match parameters in a magic comment. 
 * These have the ending <code>Magic</code>. 
 * Currently, there are two examples for this, 
 * {@link #programMagic} and {@link #targetsMagic}. 
 */
public enum LatexMainParameterNames {

  /**
   * The name of the capturing group 
   * indicating whether after creating the artifact 
   * and copying it to the output directory {@link Settings#outputDirectoryFile} 
   * the artifact is checked by diffing against a preexisting artifact 
   * as described for {@link Settings#chkDiff}. 
   * Essentially, this overwrites the settings in the pom 
   * for individual latex main files. 
   */
  chkDiffMagic,

  /**
   * The name of the capturing group 
   * indicating whether <code>latexmk</code> or 
   * to be more precise the latexmk like-tool 
   * given by {@link Settings#getLatexmkCommand()} shall be used 
   * bypassing the direct invocation of more basic tools. 
   * Essentially, this overwrites {@link Settings#getLatexmkUsage()} 
   * but this has influence on when graphic files are created: 
   * In case of {@link LatexmkUsage#Fully}, these are not preprocessed. 
   */
  latexmkMagic,

  /**
   * The name of the capturing group 
   * representing the document class specified by the commands 
   * <code>documentclass</code> or <code>documentstyle</code>. 
   */
  docClass,

  /**
   * The name of the capturing group 
   * representing the target set 
   * specified by the magic comment <code>% !LMP targets=...</code>. 
   */
  targetsMagic,

  /**
   * The name of the capturing group 
   * representing the target set 
   * specified by the magic comment <code>% !TEX program=...</code>. 
   */
  programMagic;
}
