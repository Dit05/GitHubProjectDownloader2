package org.w1ljid.projectdownloader;

/**
 * Base class for all file content filters.
 */
public abstract class FileFilter {
	/**
	 * Checks the entire contents of a file.
	 * @param fileContent Contents of the file.
	 * @return Whether the file passes this filter.
	 */
	public abstract boolean checkMatch(String fileContent);
}
