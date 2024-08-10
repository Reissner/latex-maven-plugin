
package eu.simuline.m2latex.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;


/**
 * Very bad name: 
 * Represents some aspect of postprocessing. 
 * No it is auxiliary processing, 
 * because it is alternating auxiliary tools and latex compiler. 
 * <p>
 * Note that bibliography created by bibtex may be altered also 
 * by subsequent calls of a latex compiler or of an auxiliary program 
 * as shown by https://tex.stackexchange.com/questions/724138/is-backreference-possible-with-bibtex-maybe-in-conjunction-with-biblatex. 
 */
enum Auxiliary {

  /**
   * Bibliography processing with bibtex or related. 
   * TBC: in conjunction with biblatex package also? 
   * Maybe this shall not be included, because no rerun needed. 
   * Then the process we want to introduce is superluous. 
   */
  BibTex {
    // CAUTION: it is not .bib, it is .aux. 
    // TBD: it is only part of .aux and also: 
    // for includes the files referenced therein are also to be taken into account. 
    String extension() {
      return ".aux";
    }

    boolean doesFitAuxiliary(File file) {
      boolean exists = file.exists();
      if (!exists) {
        return false;
      }

      Pattern pattern = Pattern.compile(LatexProcessor.PATTERN_NEED_BIBTEX_RUN,
                                        Pattern.MULTILINE);//
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
        for (String line = bufferedReader.readLine(); line != null;
            // readLine may thr. IOException
            line = bufferedReader.readLine()) {
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
            return true;
          }
        } // for 
        return false;
      } catch (IOException e) {
        // TBD: add warning 
        return true;
      }
    }

    int numRunsAfter() {
      return 2;
    }

    boolean process(LatexMainDesc desc, LatexProcessor proc)
        throws BuildFailureException {
      return proc.runBibtex(desc);
    }
  },
  // /**
  //  * Bibliography processing with biblatex and biber. 
  //  * TBC: is this really the sole case with backrefence 
  //  * which may force rerun? 
  //  */
  // BibEr {
  //   // CAUTION: it is not .bib, it is 
  //   String extension() {
  //     return ".bcf";
  //   }
  //},
  // TBD: index with xindy 

  /**
   * Index processing with makeindex and possibly splitindex. 
   */
  Idx {
    String extension() {
      return ".idx";
    }

    boolean process(LatexMainDesc desc, LatexProcessor proc)
        throws BuildFailureException {
      return proc.runMakeSplitIndex(desc);
    }
  },
  Glo {
    String extension() {
      return ".glo";
    }

    boolean process(LatexMainDesc desc, LatexProcessor proc)
        throws BuildFailureException {
      return proc.runMakeGlossary(desc);
    }
  },
  Pyt {
    String extension() {
      return ".pytxcode";
    }

    boolean mayBeEntryInToc() {
      return false;
    }

    boolean process(LatexMainDesc desc, LatexProcessor proc)
        throws BuildFailureException {
      return proc.runPythontex(desc);
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

  boolean doesFitAuxiliary(File file) {
    return file.exists();
  }

  // currently true except for Pyt 
  boolean mayBeEntryInToc() {
    return true;
  }

  // must be non-negative 
  int numRunsAfter() {
    return 1;
  }

  /**
   * Try to process the file associated with this auxiliary. 
   * @return
   *    whether a processor has been invoked. 
   */
  abstract boolean process(LatexMainDesc desc, LatexProcessor proc)
      throws BuildFailureException;

}
