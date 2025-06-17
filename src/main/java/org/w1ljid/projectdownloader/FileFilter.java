package org.w1ljid.projectdownloader;

public abstract class FileFilter {
	public abstract boolean checkMatch(String fileContent);
}
