package org.w1ljid.projectdownloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.util.FileUtils;
import org.w1ljid.projectdownloader.git.GitHubCredentials;

public class Main {

	// Repositories listed here will be visited before searching all of GitHub.
	public static final String[] processTheseFirst = new String[] {
		"https://github.com/Dit05/GitHubProjectDownloader2"
	};

	public static int repositoryLimit = -1; // Non-negative: stop after this many repos.


	public static void main(String[] args) throws GitAPIException, IOException, InterruptedException, Exception {

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
				System.err.println("GitHub says invalid credentials. Try generating a valid key for your account at [https://github.com/settings/personal-access-tokens].");
				return;
			} else {
				throw ioe;
			}
		}
		System.out.println("Succesfully connected to GitHub!");

		Instant started = Instant.now();
		System.out.print("### Started at: ");
		System.out.println(formatTime(started));

		for (String url : processTheseFirst) {
			processRepository(url, creds);
		}


		int reposLeft = repositoryLimit;
		if (repositoryLimit >= 0) {
			System.out.print("Processing at most " + reposLeft + " repositories");
		}

		for (GHRepository repo : github.searchRepositories().language("java").list()) {
			if (repositoryLimit >= 0) {
				System.out.println("### " + reposLeft + " left");
				if (reposLeft-- <= 0) break;
			}

			System.out.println("Now processing: \"" + repo.getFullName() + "\"");

			processRepository(repo.getHttpTransportUrl(), creds);
		}


		Instant finished = Instant.now();
		System.out.println("### Finished at: " + formatTime(finished) + " (took " + formatTimeDifference(started, finished) + ")");
	}


	static private String formatTime(Instant when) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
			.withZone(ZoneId.systemDefault())
			.format(when);
	}

	static private String formatTimeDifference(Instant from, Instant to) {
		long s = Duration.between(from, to).toSeconds();
		return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
	}

	static GitHub connectToGitHub(GitHubCredentials creds) throws IOException {
		GitHubBuilder builder = new GitHubBuilder();

		if (creds != null) builder = creds.apply(builder);

		GitHub github = builder.build();

		github.checkApiUrlValidity();
		return github;
	}

	static Ref selectMainRef(Collection<Ref> refs) {
		for (Ref ref : refs) {
			String name = ref.getName();
			if (name.equals("refs/heads/main") || name.equals("refs/heads/master")) return ref;
		}
		return null;
	}

	static void processRepository(String remoteUrl, GitHubCredentials creds) throws Exception {

		CredentialsProvider credentialsProvider = creds != null ? creds.toCredentialsProvider() : null; // No credentials for public repo

		// All the stuff we'll have to close later
		File tempGitDir = null;
		Repository repository = null;
		Git git = null;
		RevWalk revWalk = null;
		TreeWalk treeWalk = null;

		try {
			// This is deleted in the finally block.
			tempGitDir = Files.createTempDirectory("jgit-temp-repo").toFile();

			repository = new FileRepositoryBuilder()
				.setGitDir(tempGitDir)
				.setBare() // Bare repository (no working directory)
				.build();
			repository.create(true); // Important: Make sure to actually create the repository.

			System.out.println("Fetching repository: " + remoteUrl);
			git = new Git(repository);

			// Find main or master
			Ref mainRef = selectMainRef(Git.lsRemoteRepository().setRemote(remoteUrl).call());
			System.out.print("Main ref: ");
			if (mainRef != null) {
				System.out.println(mainRef);
			} else {
				System.out.println("not found, skipping repository");
				return;
			}

			// Fetch the target branch
			git.fetch()
				.setRemote(remoteUrl)
				.setRefSpecs(new RefSpec(mainRef.getName()))
				.setCredentialsProvider(credentialsProvider).call();

			System.out.println("Fetch complete.");

			revWalk = new RevWalk(repository);
			RevCommit commit = revWalk.parseCommit(mainRef.getObjectId());
			RevTree revTree = commit.getTree();

			// Walk the tree
			treeWalk = new TreeWalk(repository);
			treeWalk.addTree(revTree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(".java")); // Filter for .java files

			// TODO actually do stuff with found files
			while (treeWalk.next()) {
				// Read file
				ObjectLoader loader = repository.open(treeWalk.getObjectId(0));

				// TODO reject unprintable chars
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					loader.copyTo(out);
					//String fileContent = out.toString("UTF-8"); // Assuming UTF-8 encoding

					System.out.println("--- File: " + treeWalk.getPathString() + " ---");
					//System.out.println(fileContent.substring(0, Math.min(fileContent.length(), 200)) + "..."); // Print first 200 chars
					//System.out.println("------------------------------------------");
				}
			}

		} catch (NoRemoteRepositoryException e) {
			System.err.println("Remote repository not found at \"" + remoteUrl + "\"");
			e.printStackTrace();
		} finally {
			// Cleanup
			if (tempGitDir != null && tempGitDir.exists()) {
				try {
					FileUtils.delete(tempGitDir, FileUtils.RECURSIVE);
					System.out.println("Deleted temporary directory \"" + tempGitDir.getAbsolutePath() + "\"");
				} catch (IOException e) {
					System.err.println("Error deleting temporary directory\"" + tempGitDir.getAbsolutePath() + "\"");
					e.printStackTrace();
				}
			}
			if (repository != null) {
				try {
					repository.close();
				} catch (Exception e) {
					System.err.println("Error closing Repository object");
					e.printStackTrace();
				}
			}
			if (git != null) {
				try {
					git.close();
				} catch (Exception e) {
					System.err.println("Error closing Git object");
					e.printStackTrace();
				}
			}
			if (revWalk != null) {
				try {
					revWalk.dispose();
				} catch (Exception e) {
					System.err.println("Error disposing RevWalk object");
					e.printStackTrace();
				}
			}
			if (treeWalk != null) {
				try {
					treeWalk.close();
				} catch (Exception e) {
					System.err.println("Error closing TreeWalk object");
					e.printStackTrace();
				}
			}
		}
	}
}