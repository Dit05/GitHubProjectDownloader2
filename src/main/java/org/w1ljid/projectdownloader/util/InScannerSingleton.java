package org.w1ljid.projectdownloader.util;

import java.util.Scanner;

public class InScannerSingleton {

	private static Scanner scanner;
	
	/**
	 * Gets a Scanner for System.in. Do not close!
	 */
	public static Scanner getStdin() {
		if(scanner == null) {
			scanner = new Scanner(System.in);
		}
		return scanner;
	}

}
