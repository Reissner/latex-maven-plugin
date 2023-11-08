package eu.simuline.m2latex.core;

import java.io.File;

/**
 * Container which comprises, besides the latex main file
 * also several files creation of which shall be done once for ever.
 */
class LatexMainDesc implements Comparable<LatexMainDesc> {
    final File texFile;
    final File pdfFile;
    final File dviFile;
    final File xdvFile;

    final File logFile;

    final File idxFile;
    final File indFile;
    final File ilgFile;

    final File glsFile;
    final File gloFile;
    final File glgFile;

    final File xxxFile;

    final File parentDir;

    LatexMainDesc(File texFile) {
        this.texFile = texFile;
        this.xxxFile = TexFileUtils.replaceSuffix(texFile, LatexProcessor.SUFFIX_VOID);
        this.pdfFile = withSuffix(LatexProcessor.SUFFIX_PDF);
        this.dviFile = withSuffix(LatexProcessor.SUFFIX_DVI);
        this.xdvFile = withSuffix(LatexProcessor.SUFFIX_XDV);
        this.logFile = withSuffix(LatexProcessor.SUFFIX_LOG);

        this.idxFile = withSuffix(LatexProcessor.SUFFIX_IDX);
        this.indFile = withSuffix(LatexProcessor.SUFFIX_IND);
        this.ilgFile = withSuffix(LatexProcessor.SUFFIX_ILG);

        this.glsFile = withSuffix(LatexProcessor.SUFFIX_GLS);
        this.gloFile = withSuffix(LatexProcessor.SUFFIX_GLO);
        this.glgFile = withSuffix(LatexProcessor.SUFFIX_GLG);
        this.parentDir = this.texFile.getParentFile();
    }

    File withSuffix(String suffix) {
        return TexFileUtils.appendSuffix(this.xxxFile, suffix);
    }

    public int compareTo(LatexMainDesc other) {
      return this.texFile.compareTo(other.texFile);
    }

    public String toString() {
      return "<LatexMainDesc>" + this.texFile.getName() + "</LatexMainDesc>";
    }
} // class LatexMainDesc