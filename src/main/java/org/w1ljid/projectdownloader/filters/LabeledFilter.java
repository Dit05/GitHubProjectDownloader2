package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;


/**
 * This filter delegates everything to its inner filter, however, it has a
 * single immutable string member.
 * 
 * @see #getLabel()
 */
public class LabeledFilter extends FileFilter {

	private final String label;
	private final FileFilter innerFilter;


	public LabeledFilter(FileFilter innerFilter, String label) {
		this.innerFilter = innerFilter;
		this.label = label;
	}


	public String getLabel() { return label; }

	@Override
	public boolean checkMatch(String fileContent) { // TODO Auto-generated method stub
		return innerFilter.checkMatch(fileContent);
	}

}
