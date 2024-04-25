package eu.simuline.m2latex.core;

/**
 * Enumerates the various degrees of usage of the latexmk command 
 * specified by {@link ConverterCategory#Latexmk}. 
 */
public enum LatexmkUsage {

  /**
   * The command related with {@link ConverterCategory#Latexmk} is never invoked. 
   * Refraining from latexmk has the advantage that this builder software controls all log files 
   * and all return values. 
   * As a consequence, the user can check that the build process passed without warning or error. 
   * This is the default value. 
   */
  NotAtAll,

  /**
   * Graphic files needed to compile latex main files are created in a traditional way 
   * before invoking {@link ConverterCategory#Latexmk}, so that they are present 
   * when latexmk is used to convert the latex main files. 
   * Unlike for {@link #NotAtAll} this builder software cannot check log files in this mode. 
   * Still, unlike for {@link #Fully}, 
   * excessive reruns because of missing graphic files, i.e. graphic files to be created are avoided. 
   */
  AsBackend,

  /**
   * All build processes are delegated to the tool {@link ConverterCategory#Latexmk}. 
   * In particular, this builder cannot check log files 
   * and also the builder does not create graphic files a priori. 
   * Instead latexmk decides on the needed graphic files. 
   * This may cause excessive rerun of the latex compiler, 
   * because the compiler run is stopped for each missing graphic file, 
   * then it is created and the compiler is rerun. 
   * <p>
   * Even in this mode, this builder decides on which tex files are to be compiled, 
   * although latexmk could do this as well 
   * and latexmk is applied to individual latex files only. 
   * Note also that latexmk is not used to cleanup. 
   */
  // TBD: well: cleanup shall be done by latexmk in this case also. 
  Fully;

}
