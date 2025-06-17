package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;

/**
 * This filter approves anything.
 */
public final class AlwaysFilter extends FileFilter {

	/**
	 * Since this class has no state, a single instance of it is provided for
	 * convenience.
	 */
	public static final AlwaysFilter INSTANCE = new AlwaysFilter();

	@Override
	public boolean checkMatch(String fileContent) {
		return true;
	}

}
