package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;

/**
 * Matches a file only when every inner filter matches it.
 */
public class AndFilter extends FileFilter {

	private final FileFilter[] innerFilters;

	public AndFilter(FileFilter... innerFilters) {
		this.innerFilters = innerFilters;
	}

	@Override
	public boolean checkMatch(String fileContent) {
		for (FileFilter filter : innerFilters) {
			if (!filter.checkMatch(fileContent)) return false;
		}
		return true;
	}
}
