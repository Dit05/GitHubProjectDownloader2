package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;

/**
 * Matches a file when any inner filter matches it.
 */
public class OrFilter extends FileFilter {

	private final FileFilter[] innerFilters;

	public OrFilter(FileFilter... innerFilters) {
		this.innerFilters = innerFilters;
	}

	@Override
	public boolean checkMatch(String fileContent) {
		for (FileFilter filter : innerFilters) {
			if (filter.checkMatch(fileContent)) return true;
		}
		return false;
	}
}
