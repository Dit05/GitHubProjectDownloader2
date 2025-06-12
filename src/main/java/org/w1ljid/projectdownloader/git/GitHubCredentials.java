package org.w1ljid.projectdownloader.git;

import java.util.NoSuchElementException;
import java.util.Scanner;
import org.kohsuke.github.GitHubBuilder;
import org.w1ljid.projectdownloader.util.InScannerSingleton;

public class GitHubCredentials {

	public static final String ENV_KEY = "PROJECTDOWNLOADER_GITHUB_KEY";

	private final String key;


	private GitHubCredentials(String key) {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("key must not be null or blank");

		this.key = key;
	}


	public GitHubBuilder apply(GitHubBuilder builder) {
		return builder.withOAuthToken(key);
	}


	/**
	 * Attempts to retrieve credentials from environment variables.
	 * 
	 * @see ENV_USER
	 * @see ENV_KEY
	 * @return A new instance, or null if the API key environment variable is empty.
	 */
	public static GitHubCredentials acquire() {
		String key = System.getenv(ENV_KEY);
		if (key != null && !key.isBlank()) return new GitHubCredentials(key);
		else return null;
	}

	/**
	 * Prompts for a username and API key, reading from System.in.
	 * 
	 * @return A new instance, or null if credentials weren't given.
	 */
	public static GitHubCredentials prompt() {
		try {
			Scanner scanner = InScannerSingleton.getStdin();

			System.out.print("GitHub API Key: ");
			String key;
			try {
				key = scanner.nextLine();
			} catch (NoSuchElementException _e) {
				return null;
			}

			if (key == null || key.isBlank()) {
				System.out.println("No API key key provided.");
				return null;
			}

			return new GitHubCredentials(key);
		} finally {
			System.out.println();
		}
	}

	public static GitHubCredentials acquireOrPrompt() {
		GitHubCredentials creds = acquire();
		if (creds != null) {
			System.out.println("Got GitHub credentials from environment.");
			return creds;
		}

		System.out.println("Enter your GitHub credentials for more favorable rate limits.");
		System.out.println("Leaving the API key key blank will continue without authentication.");
		System.out.println("(these credentials can also be provided by setting the "
		+ ENV_KEY + " environment variable.)");

		return prompt();
	}

}
