package org.w1ljid.projectdownloader.filters;

import org.w1ljid.projectdownloader.FileFilter;

/**
 * Rejects control characters and other obscure codepoints.
 */
public class SketchyCharactersFilter extends FileFilter {

	@Override
	public boolean checkMatch(String fileContent) {
		for (int i = 0; i < fileContent.length(); i++) {
			char ch = fileContent.charAt(i);
			if (" \n\r\t".indexOf(ch) >= 0) continue; // Approved characters

			if (Character.isHighSurrogate(ch)) {
				if (i + 1 > fileContent.length()) {
					return false; // Unpaired surrogate
				}
				char low = fileContent.charAt(i + 1);

				int codepoint = Character.toCodePoint(ch, low);
				if(!Character.isValidCodePoint(codepoint)) return false; // Not real
				if(!Character.isBmpCodePoint(codepoint)) return false; // Not in BMP
				if(Character.isWhitespace(codepoint)) return false; // If this were well-meaning whitespace, it would've been in the whitelist earlier.
			} else {
				if(Character.isISOControl(ch)) return false; // Control character
				if(Character.isWhitespace(ch)) return false; // If this were well-meaning whitespace, it would've been in the whitelist earlier.
			}
		}

		return true;
	}

}
