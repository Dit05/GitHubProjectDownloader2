package org.w1ljid.projectdownloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
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
import org.w1ljid.projectdownloader.filters.*;

public class Main {

	static final long U_KiB = 1024;
	static final long U_MiB = 1024 * U_KiB;
	static final long U_GiB = 1024 * U_MiB;

	// --- Configuration ---
	static final String outputDirectory = "downloaded"; // Absolute or relative directory where downloaded files will go. Note: this is not cleared between runs!
	static final String downloadedFileExtension = ".java"; // Appended to downloaded file names.

	// Repositories listed here will be visited before searching all of GitHub.
	static final String[] processTheseFirst = new String[] {
		"https://github.com/Dit05/GitHubProjectDownloader2"
	};


	// Filter for git paths, before file content is even looked at.
	static final TreeFilter treeFilter = PathSuffixFilter.create(".java");

	// This catch-all filter, if provided, runs on file contents before category filters.
	static final FileFilter preFilter = new AndFilter(new SketchyCharactersFilter(), new NotFilter(new KeywordFilter("Test", "junit"))); // Exclude files that seem like unit tests.

	// Stop after this many samples have been found for a category. -1 means no limit.
	static final int categoryLimit = -1;

	// Categories. (filters all found under org.w1ljid.projectdownloader.filters)
	static final LabeledFilter[] categoryFilters = new LabeledFilter[] {
		new LabeledFilter(new LimitedTotalMatchesFilter(new RegexFilter("class [A-z]*Singleton"), categoryLimit), "Singleton"),
		new LabeledFilter(new LimitedTotalMatchesFilter(new RegexFilter("class [A-z]*Adapter", "class [A-z]*2[A-z]+", "class [A-z]+To[A-Z][A-z]*"), categoryLimit), "Adapter"),
		new LabeledFilter(new LimitedTotalMatchesFilter(new RegexFilter("class [A-z]*Factory"), categoryLimit), "AbstractFactory"),
		new LabeledFilter(new LimitedTotalMatchesFilter(new RegexFilter("class [A-z]*Builder"), categoryLimit), "Builder"),
		new LabeledFilter(new LimitedTotalMatchesFilter(new RegexFilter("class [A-z]*Command[A-z]*"), categoryLimit), "Command"),
	};

	// Whether to print a line for every file found.
	static final boolean reportIndividualFiles = true;

	// When non-negative: stop after this many repos.
	static final int repositoryLimit = -1;

	// Don't download repositories that are larger than this.
	static final long maxRepositorySize = 1 * U_GiB;
	// Don't process files that are larger than this.
	static final long maxFileSize = 10 * U_MiB;
	// -------



	public static void main(String[] args) throws GitAPIException, IOException, InterruptedException, Exception {

		FileLibrary fileLib = new FileLibrary(outputDirectory);
		System.out.println("Created file library at \"" + fileLib.getCanonicalRoot() + "\"");

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
		System.out.println("### Started at: " + formatTime(started));

		for (String url : processTheseFirst) {
			System.out.println("Now processing: \"" + url + "\" (time: " + formatTime(Instant.now()) + ")");
			processRepository(url, creds, fileLib, url);
			System.out.println("\n---\n");
		}


		int reposLeft = repositoryLimit;
		if (repositoryLimit >= 0) {
			System.out.println("Processing at most " + reposLeft + " repositories");
		}

		for (GHRepository repo : github.searchRepositories().language("java").list()) {
			if (repositoryLimit >= 0) {
				System.out.println("### " + reposLeft + " left");
				if (reposLeft-- <= 0) break;
			}

			long repoSize = repo.getSize() * U_KiB;
			System.out.println("Now processing: \"" + repo.getFullName() + "\" (size: " + formatBytes(repoSize) + ", time: " + formatTime(Instant.now()) + ")");

			if (repoSize > maxRepositorySize) {
				System.out.println("Too big, skipping.");
				continue;
			}

			processRepository(repo.getHttpTransportUrl(), creds, fileLib, repo.getFullName());
			System.out.println("\n---\n");
		}


		Instant finished = Instant.now();
		System.out.println("### Finished at: " + formatTime(finished) + " (took " + formatTimeDifference(started, finished) + ")");
	}


	static private String formatTime(Instant when) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
			.withZone(ZoneId.systemDefault())
			.format(when);
	}

	static private String formatBytes(long bytes) {
		final String[] UNITS = new String[] { "B", "KiB", "MiB", "GiB", "TiB", "EiB" /* Hopefully not */ };

		double size = bytes;
		int u = 0;
		while (size > 1024.0 && u < UNITS.length) {
			size /= 1024.0;
			u++;
		}

		DecimalFormat nf = new DecimalFormat();
		int decimals = u;
		nf.setMinimumFractionDigits(decimals);
		nf.setMaximumFractionDigits(decimals);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf.format(size) + " " + UNITS[u];
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

	static String checkAgainstCategories(String fileContent) {
		for (LabeledFilter filter : categoryFilters) {
			if (filter.checkMatch(fileContent)) return filter.getLabel();
		}
		return null;
	}

	static int processRepository(String remoteUrl, GitHubCredentials creds, FileLibrary fileLib, String labelPrefix) throws Exception {

		CredentialsProvider credentialsProvider = creds != null ? creds.toCredentialsProvider() : null; // No credentials for public repo

		// All the stuff we'll have to close later
		File tempGitDir = null;
		Repository repository = null;
		Git git = null;
		RevWalk revWalk = null;
		TreeWalk treeWalk = null;
		int filesFound = 0;
		long bytesFound = 0;

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
				return 0;
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
			treeWalk.setFilter(treeFilter);

			while (treeWalk.next()) {
				// Read file
				ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
				String filePathString = treeWalk.getPathString();

				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					loader.copyTo(out);
					if (out.size() > maxFileSize) {
						continue;
					}

					String fileContent = out.toString("UTF-8"); // Assuming UTF-8 encoding
					if (preFilter != null && !preFilter.checkMatch(fileContent)) continue;

					String category = checkAgainstCategories(fileContent);
					if (category == null) continue;

					filesFound++;
					bytesFound += out.size();

					if (reportIndividualFiles) {
						String label = labelPrefix + " " + fileLib.suggestLabel(filePathString);
						String storedPath = fileLib.store(category, label, out, downloadedFileExtension);
						System.out.println("Found \"" + filePathString + "\" -> " + storedPath + " (" + formatBytes(out.size()) + ")");
					}
				}
			}
			System.out.println("Acquired a total of " + filesFound + " file" + (filesFound == 1 ? "" : "s") + " (" + formatBytes(bytesFound) + ")");

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

		return filesFound;
	}
}