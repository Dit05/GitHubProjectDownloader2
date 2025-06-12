package org.w1ljid.projectdownloader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.*;
import org.w1ljid.projectdownloader.git.GitHubCredentials;

public class Main {
	static final String SOURCE_URI = "https://github.com/DanisovszkyMark/Example-Project-For-AST-Analysis";
	static String copyToDirectory;

	static public void main(String[] args) throws GitAPIException, IOException, InterruptedException {
		System.out.print("Started at: ");
		System.out.println(formatTime(new Date()));

		GitHubCredentials creds = GitHubCredentials.acquireOrPrompt();
		if (creds == null) System.out.println("No credentials provided.");

		GitHub github;
		try {
			github = connectToGitHub(creds);
		} catch (java.io.IOException ioe) {
			Throwable cause = ioe.getCause();
			if (
				cause != null
				&& cause instanceof org.kohsuke.github.HttpException
				&& ((org.kohsuke.github.HttpException) cause).getMessage().contains("Bad credentials")
			) {
				System.err.println(
				"GitHub says invalid credentials. Try generating a valid key for your account at [https://github.com/settings/personal-access-tokens].");
				return;
			} else {
				throw ioe;
			}
		}
		System.out.println("Succesfully connected to GitHub!");

		int i = 10; // HACK
		for (GHRepository repo : github.searchRepositories().language("java").list()) {
			System.out.println(repo.getFullName());
			if (i-- < 0) break;
		}

		// TODO stuff
	}


	static private String formatTime(Date when) {
		return new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z").format(when);
	}

	static GitHub connectToGitHub(GitHubCredentials creds) throws IOException {
		GitHubBuilder builder = new GitHubBuilder();

		if (creds != null) builder = creds.apply(builder);

		GitHub github = builder.build();

		github.checkApiUrlValidity();
		return github;
	}
}