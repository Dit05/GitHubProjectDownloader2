package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;


/**
 * Filter that stops matching after a given number of successful matches over
 * its lifetime.
 */
public class LimitedTotalMatchesFilter extends FileFilter {

	private final FileFilter innerFilter;
	private int matchesLeft;


	public LimitedTotalMatchesFilter(FileFilter innerFilter, int matchesLeft) {
		this.innerFilter = innerFilter;
		this.matchesLeft = matchesLeft;
	}


	@Override
	public boolean checkMatch(String fileContent) { // TODO Auto-generated method stub
		if (matchesLeft == 0) {
			return false;
		}

		if (innerFilter.checkMatch(fileContent)) {
			if(matchesLeft > 0) matchesLeft--;
			return true;
		} else {
			return false;
		}
	}

}
