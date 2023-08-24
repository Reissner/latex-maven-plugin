package eu.simuline.m2latex.core;

// import java.io.File;

/**
 * Represents a file to be injected in goal <code>inj</code>. 
 * Injection means that it is a resource of this software 
 * but can be inserted (injected) at {@link Settings#texSrcDirectory}. 
 * It is not injected if there is already a file with this name 
 * and it cannot be ensured that it is this software which wrote it. 
 * The mechanism to find out whether this is true is: reading the headline. 
 * To that end, a specific headline is inserted. 
 * It is preceeded with the comment sign {@link #commentStr()} 
 * appropriate for the format of the file, mostly a hash sign. 
 * The files for which {@link #doFilter()} return true, 
 * as e.g. for <code>.latexmkrc</code>, are filtered 
 * replacing parameter names of this software by current values. 
 * This allows to synchronize the settings of this software 
 * with the settings of <code>latexmk</code>. 
 * Finally, if {@link #setExecutable()} tells so, 
 * the resulting file is set executable, 
 * as is appropriate for scripts like <code>instVScode4tex.sh</code>. 
 */
public enum Injection {
  /**
   * The record file of latexmk. 
   * This must be filtered 
   * to be synchronized with the current settings 
   * of this piece of software. 
   */
  latexmkrc {
    String getFileName() {
      return ".latexmkrc";
    }

    boolean doFilter() {
      return true;
    }
    boolean hasShebang() {
      return true;
    }
  },
  /**
   * The record file of chktex. 
   * This is adapted to some use cases
   * of this piece of software. 
   */
  chktexrc {
    String getFileName() {
      return ".chktexrc";
    }
    boolean hasShebang() {
      return false;
    }
  },
  /**
   * The installation script for extensions of VS Code 
   * used for development of latex documents. 
   */
  vscodeExt {
    String getFileName() {
      return "instVScode4tex.sh";
    }

    boolean setExecutable() {
      return true;
    }

    boolean hasShebang() {
      return true;
    }
  };

  /**
   * Returns the filename of the resource. 
   * It is injected under this file name 
   * in the folder given by {@link Settings#texSrcDirectory}. 
   * 
   * @return
   *    the filename of the resource. 
   */
  abstract String getFileName();

  /**
   * Returns the character indicating a comment. 
   * 
   * @return
   *    the character indicating a comment. 
   *    By default this is <code>#</code> and is overwritten by need. 
   *    It is appropriate for bash, perl and also as chktexrc. 
   *    For TEX filtes it must be overwritten. 
   */
  String commentStr() {
    return "#";
  }

  /**
   * Returns whether this file must be filtered before being injected. 
   * @return
   *   whether this file must be filtered before being injected. 
   *   This is <code>false</code>, except for {@link Injection#latexmkrc}. 
   */
  boolean doFilter() {
    return false;
  }

  boolean setExecutable() {
    return false;
  }

  abstract boolean hasShebang();
}