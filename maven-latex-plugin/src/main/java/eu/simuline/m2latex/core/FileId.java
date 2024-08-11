package eu.simuline.m2latex.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides an identifier for a file consisting of the size and a hash. 
 * We used md5. 
 * This is used to decide whether a file changed 
 * without storing the complete old file. 
 * This is done by a hash: a function that assigns an identifier to a file. 
 * Of course, there must be 'collisions', 
 * i.e. cases where different files have the same identifier, 
 * unless it is at least the file itself that serves as an identifier, 
 * but this is quite unlikely and we accept that in these rare cases, 
 * the identification fails. 
 * <p>
 * This class is applied in rerun file check, i.e. to trigger the run of a tool 
 * if a file changed. 
 * In case of a collision, 
 * the execution of the tool is not triggered although needed. 
 */
public class FileId {

  final long length;
  final String hash;

  FileId(File file) {
    assert !file.isDirectory();
    this.length = file.length();

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      if (update(file, md)) {
        this.hash = new String(md.digest());
      } else {
        // TBD: add warning 
        System.out.println("EMPTY HASH IO");
        this.hash = "";
      }
      // try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      //   for (String line = bufferedReader.readLine(); line != null;
      //       // readLine may thr. IOException
      //       line = bufferedReader.readLine()) {
      //     md.update(line.getBytes());
      //   }
      //   firstHash = new String(md.digest());
      // } catch (IOException e) {
      //   // TBD: add warning 
      //   System.out.println("EMPTY HASH IO");
      //   firstHash = "";
      // }
    } catch (NoSuchAlgorithmException nsae) {
      // TBD: emit warning 
      throw new IllegalStateException("Algorithm should be known. ");
    }
  }

  boolean update(File file, MessageDigest md) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      for (String line = bufferedReader.readLine(); line != null;
          // readLine may thr. IOException
          line = bufferedReader.readLine()) {
        md.update(line.getBytes());
      }
      return true;
      //firstHash = new String(md.digest());
    } catch (IOException e) {
      return false;
    }
  }



  public boolean equals(Object obj) {
    if (!(obj instanceof FileId)) {
      return false;
    }
    FileId other = (FileId)obj;
    return this.length == other.length
      && this.hash.equals(other.hash);
  }

  public String toString() {
    return this.hash + " " + this.length;
  }
}

