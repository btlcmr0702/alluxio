/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.shell.command;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.client.file.FileSystem;
import alluxio.client.file.FileSystemUtils;
import alluxio.client.file.URIStatus;
import alluxio.exception.AlluxioException;

import com.google.common.base.Joiner;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Persists files or directories currently stored only in Alluxio to the UnderFileSystem.
 */
@ThreadSafe
public final class PersistCommand extends AbstractShellCommand {

  /**
   * @param conf the configuration for Alluxio
   * @param fs the filesystem of Alluxio
   */
  public PersistCommand(Configuration conf, FileSystem fs) {
    super(conf, fs);
  }

  @Override
  public String getCommandName() {
    return "persist";
  }

  @Override
  protected int getNumOfArgs() {
    return 1;
  }

  @Override
  public boolean validateArgs(String... args) {
    boolean valid = args.length >= getNumOfArgs();
    if (!valid) {
      System.out.println(getCommandName() + " takes " + getNumOfArgs() + " argument at least\n");
    }
    return valid;
  }

  @Override
  public void run(CommandLine cl) throws IOException {
    String[] args = cl.getArgs();
    for (String path : args) {
      AlluxioURI inputPath = new AlluxioURI(path);
      persist(inputPath);
    }
  }

  /**
   * Persists a file or directory currently stored only in Alluxio to the UnderFileSystem.
   *
   * @param filePath the {@link AlluxioURI} path to persist to the UnderFileSystem
   * @throws IOException when an Alluxio or I/O error occurs
   */
  private void persist(AlluxioURI filePath) throws IOException {
    try {
      URIStatus status = mFileSystem.getStatus(filePath);
      if (status.isFolder()) {
        List<URIStatus> statuses = mFileSystem.listStatus(filePath);
        List<String> errorMessages = new ArrayList<>();
        for (URIStatus uriStatus : statuses) {
          AlluxioURI newPath = new AlluxioURI(uriStatus.getPath());
          try {
            persist(newPath);
          } catch (IOException e) {
            errorMessages.add(e.getMessage());
          }
        }
        if (errorMessages.size() != 0) {
          throw new IOException(Joiner.on('\n').join(errorMessages));
        }
      } else if (status.isPersisted()) {
        System.out.println(filePath + " is already persisted");
      } else {
        long size = FileSystemUtils.persistFile(mFileSystem, filePath, status, mConfiguration);
        System.out.println("persisted file " + filePath + " with size " + size);
      }
    } catch (AlluxioException e) {
      throw new IOException(e.getMessage());
    }
  }

  @Override
  public String getUsage() {
    return "persist <alluxioPath1> [alluxioPath2] ... [alluxioPathn]";
  }

  @Override
  public String getDescription() {
    return "Persists files or directories currently stored only in Alluxio to the "
        + "UnderFileSystem.";
  }
}
