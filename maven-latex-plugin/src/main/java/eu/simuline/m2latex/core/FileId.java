package eu.simuline.m2latex.core;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an immutable identifier for a file 
 * consisting of the number of relevant lines lines 
 * and a hash computed from these lines. 
 * The the hash is computed with fixed algorithm {@link #ALGORITHM}. 
 * <p>
 * The relevant parts of a file is given by an {@link #Auxiliary}, 
 * So, the constructor has signature {@link #FileId(File, Auxiliary)}. 
 * <p>
 * If two files coincide in their relevant lines, 
 * their {@link FileId}s coincide, 
 * i.e. they are equal according to {@link #equals(Object)}. 
 * If they don't coincide, then it is conceivable but still unlikely, 
 * that their {@link FileId}s coincide. 
 * We call this a collision. 
 * So the {@link FileId} may be used to detect file changes quite safely. 
 * <p>
 * This class is applied in rerun file check, 
 * i.e. to trigger the run of a tool 
 * if a file changed. 
 * In case of a collision, 
 * the execution of the tool is not triggered although needed. 
 */
public class FileId {

  /**
   * The algorithm used to compute the hash {@link #hash} 
   * from lines of a file determined by an {@link Auxiliary}. 
   * 
   * @see #FileId(File, Auxiliary)
   */
  private static final String ALGORITHM = "MD5";

  /**
   * The number of lines written in a file relevant for the {@link Auxiliary} 
   * given by the constructor {@link #FileId(File, Auxiliary)}. 
   */
  private final int numLines;

  /**
   * The hash of lines the number of which is given by {@link #numLines}. 
   */
  private final String hash;

  FileId(File file, Auxiliary aux) {
    assert !file.isDirectory();

    try {
      MessageDigest md = MessageDigest.getInstance(ALGORITHM);
      // The number of relevant lines decided by method Auxiliary.update(...) 
      AtomicInteger numLines = new AtomicInteger(0);
      try {
        aux.update(file, md, numLines);
        this.hash = new String(md.digest());
        this.numLines = numLines.get();
      } catch (IOException ioe) {
        // TBD: add warning 
        System.out.println("Risk to drop necessary rerun is augmented. ");
        this.hash = "";
        this.numLines = 0;
      }
    } catch (NoSuchAlgorithmException nsae) {
      throw new IllegalStateException(
          "Algorithm " + ALGORITHM + " should be known. ");
    }
  }

  /**
   * The given object equals this one if it is an instance of {@link FileId} 
   * and both fields are equal. 
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof FileId)) {
      return false;
    }
    FileId other = (FileId) obj;
    return this.numLines == other.numLines && this.hash.equals(other.hash);
  }

  public String toString() {
    return this.hash + " " + this.numLines;
  }
}

