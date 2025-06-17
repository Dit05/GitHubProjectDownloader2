package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;


/**
 * Filter that matches files containing any of the provided keywords. Matching
 * is case-sensitive!
 */
public class KeywordFilter extends FileFilter {

	private final String[] keywords;

	public KeywordFilter(String... keywords) {
		this.keywords = keywords;
	}

	@Override
	public boolean checkMatch(String fileContent) {
		for (String keyword : keywords) {
			if (fileContent.contains(keyword)) return true;
		}
		return false;
	}

}
