package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;

/**
 * Negates the inner filter.
 */
public class NotFilter extends FileFilter {

	private final FileFilter innerFilter;

	public NotFilter(FileFilter innerFilter) {
		this.innerFilter = innerFilter;
	}

	@Override
	public boolean checkMatch(String fileContent) {
		return !innerFilter.checkMatch(fileContent);
	}

}
