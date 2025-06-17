package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;
import java.util.regex.*;


/**
 * Filter that matches files containing any of the provided regices.
 */
public class RegexFilter extends FileFilter {

	private final Pattern[] patterns;

	public RegexFilter(Pattern... patterns) {
		this.patterns = patterns;
	}

	public RegexFilter(String... patternStrings) {
		this.patterns = new Pattern[patternStrings.length];
		for (int i = 0; i < patternStrings.length; i++) {
			patterns[i] = Pattern.compile(patternStrings[i]);
		}
	}

	public RegexFilter(int compileFlags, String... patternStrings) {
		this.patterns = new Pattern[patternStrings.length];
		for (int i = 0; i < patternStrings.length; i++) {
			patterns[i] = Pattern.compile(patternStrings[i], compileFlags);
		}
	}

	@Override
	public boolean checkMatch(String fileContent) {
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(fileContent);
			if (matcher.find()) return true;
		}
		return false;
	}

}
