
package eu.simuline.m2latex.core;

/**
 * Very bad name: 
 * Represents some aspect of postprocessing. 
 */
enum Auxiliary {

  Bib {
    // CAUTION: it is not .bib, it is 
    String extension() {
      return ".aux";
    }
  },
  Idx {
    String extension() {
      return ".idx";
    }
  },
  Glo {
    String extension() {
      return ".glo";
    }
  },
  Pyt {
    String extension() {
      return ".pytxcode";
    }
  };

  /**
   * The file extension <code>.ext</code> triggering the action. 
   * If <code>xxx.tex</code> is the latex main file, 
   * then <code>xxx.ext</code> is the triggering file. 
   * This file is created fromt the TEX file, 
   * in some cases due to some package. 
   * 
   * @return
   *    The extension (with dot) as a string. 
   */
  // TBD: without dot would be more elegant 
  abstract String extension();

}
