/*
The MIT License (MIT)

Copyright (c) 2015 Sanketh P B

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.etas.jenkins.plugins.VersionPatcher;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import jenkins.security.Roles;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.jenkinsci.remoting.RoleChecker;

public class PatchFilesTask implements Callable<Boolean, IOException> {

	private final String searchPath;
	private final String fileExtension;
	private final String fileVersion;
	private final String productVersion;
	private final String copyrightString;
	BuildListener listener;

	private static final long serialVersionUID = 1L;

	PatchFilesTask(String searchPath, String fileExtension, String fileVersion,
			String productVersion, String copyrightString,
			BuildListener listener) {

		this.searchPath = searchPath;
		this.fileExtension = fileExtension;
		this.fileVersion = fileVersion;
		this.productVersion = productVersion;
		this.copyrightString = copyrightString;
		this.listener = listener;
	}

	public Boolean call() throws IOException {

		try {

			File searchDir = new File(searchPath);

			Collection<File> resourceFiles = FileUtils.listFiles(searchDir,
					new RegexFileFilter(fileExtension),
					DirectoryFileFilter.DIRECTORY);

			if ((resourceFiles.size() <= 0)) {
				listener.getLogger().println("No files to process.");
				return true;
			}

			String fVersion = fileVersion.replace(".", ",");
			String prVersion = productVersion.replace(".", ",");

			for (File rcfile : resourceFiles) {

				String detectedEncoding = FileEncodingDetector
						.getFileEncoding(rcfile);
				String content = FileUtils.readFileToString(rcfile,
						detectedEncoding);

				if (rcfile.getName().endsWith(".rc")) {
					content = content.replaceAll("(.*?FILEVERSION.*.)",
							" FILEVERSION " + fVersion);
					content = content.replaceAll("\"FileVersion.*.",
							"\"FileVersion\", \"" + fileVersion + "\"");
					content = content.replaceAll("(.*?PRODUCTVERSION.*.)",
							" PRODUCTVERSION " + prVersion);
					content = content.replaceAll("\"ProductVersion.*.",
							"\"ProductVersion\", \"" + productVersion + "\"");
					content = content.replaceAll("\"LegalCopyright.*.",
							"\"LegalCopyright\", \"" + copyrightString + "\"");
					listener.getLogger()
					.println(
							String.format(
									"Updating resource file: '%s'. Encoding: %s",
									rcfile.getAbsolutePath(),
									detectedEncoding));
				} else if (rcfile.getName().equals("AssemblyInfo.cs")) {
					content = content.replaceAll("AssemblyFileVersion.*.",
							"AssemblyFileVersion(\"" + fileVersion + "\")]");
					content = content.replaceAll("AssemblyVersion.*.",
							"AssemblyVersion(\"" + productVersion + "\")]");
					content = content.replaceAll("AssemblyCopyright.*.",
							"AssemblyCopyright(\"" + copyrightString + "\")]");
					listener.getLogger()
					.println(
							String.format(
									"Updating AssemblyInfo.cs file: '%s'. Encoding: %s",
									rcfile.getAbsolutePath(),
									detectedEncoding));

				} else if (rcfile.getName().equals("AssemblyInfo.cpp")) {
					content = content.replaceAll(
							"AssemblyFileVersionAttribute.*.",
							"AssemblyFileVersionAttribute(\"" + fileVersion
							+ "\")]");
					content = content.replaceAll("AssemblyVersionAttribute.*.",
							"AssemblyVersionAttribute(\"" + productVersion
							+ "\")]");
					content = content.replaceAll(
							"AssemblyCopyrightAttribute.*.",
							"AssemblyCopyrightAttribute(\"" + copyrightString
							+ "\")]");
					listener.getLogger()
					.println(
							String.format(
									"Updating AssemblyInfo.cpp file: '%s'. Encoding: %s",
									rcfile.getAbsolutePath(),
									detectedEncoding));
				} else {

					listener.getLogger().println(
							String.format("Error: Invalid file: '%s'",
									rcfile.getAbsolutePath()));
				}

				FileUtils.writeStringToFile(rcfile, content, detectedEncoding);
			}

		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
			e.printStackTrace(listener.getLogger());
			return false;
		}
		return true;
	}

	@Override
	public void checkRoles(RoleChecker checker) throws SecurityException {
		checker.check(this, Roles.SLAVE);		
	}

}
