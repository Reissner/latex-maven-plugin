package eu.simuline.m2latex.core;

import java.io.File;
import java.io.FileInputStream;
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

    String firstHash = null;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      try (FileInputStream fis = new FileInputStream(file)) {
        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        }
        firstHash = new String(md.digest());
      } catch (IOException e) {
        // TBD: add warning 
        firstHash = "";
      }
    } catch (NoSuchAlgorithmException nsae) {
      // TBD: emit warning 
      firstHash = "";
    }
    this.hash = firstHash;
  }

}

