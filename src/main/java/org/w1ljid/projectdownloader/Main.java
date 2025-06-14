package org.w1ljid.projectdownloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.*;
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
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.util.FileUtils;
import org.w1ljid.projectdownloader.git.GitHubCredentials;

public class Main {

	static public void main(String[] args) throws GitAPIException, IOException, InterruptedException, Exception {

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

		System.out.print("Started at: ");
		System.out.println(formatTime(new Date()));

		processRepository("https://github.com/Dit05/GitHubProjectDownloader2", creds);
		//if (1 == 1) return;


		int reposLeft = 10;
		System.out.print("Processing at most ");
		System.out.print(reposLeft);
		System.out.println(" repositories.");

		for (GHRepository repo : github.searchRepositories().language("java").list()) {
			System.out.print("### ");
			System.out.print(reposLeft);
			System.out.println(" left");
			if (reposLeft-- <= 0) break;

			System.out.print("Now processing: \"");
			System.out.print(repo.getFullName());
			System.out.println("\"");

			processRepository(repo.getHttpTransportUrl(), creds);
		}


		System.out.print("Finished at: ");
		System.out.println(formatTime(new Date()));
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

	static Ref selectMainRef(Collection<Ref> refs) {
		for (Ref ref : refs) {
			String name = ref.getName();
			if (name.equals("refs/heads/main") || name.equals("refs/heads/master")) return ref;
		}
		return null;
	}

	static void processRepository(String remoteUrl, GitHubCredentials creds) throws Exception {

		CredentialsProvider credentialsProvider = creds != null ? creds.toCredentialsProvider() : null; // No credentials for public repo

		File tempGitDir = null;

		try {
			tempGitDir = Files.createTempDirectory("jgit-temp-repo").toFile();

			// 1. Create a temporary in-memory or bare repository to fetch into
			// This avoids creating a full working directory clone
			//Repository repository = new FileRepositoryBuilder().setGitDir(tempGitDir).setBare().build(); // Important: create a bare repository
			Repository repository = new FileRepositoryBuilder().setGitDir(tempGitDir).setBare().build(); // Important: create a bare repository
			repository.create(true); // Important: Make sure to actually create the repository.

			// 2. Fetch the remote repository's refs (branches, tags)
			// This fetches the object data into our temporary repository
			System.out.println("Fetching repository: " + remoteUrl);
			Git git = new Git(repository);

			Ref mainRef = selectMainRef(Git.lsRemoteRepository().setRemote(remoteUrl).call());
			System.out.print("Main ref: ");
			if (mainRef != null) {
				System.out.println(mainRef);
			} else {
				System.out.println("not found, skipping repository");
				return;
			}

			git.fetch()
				.setRemote(remoteUrl)
				.setRefSpecs(new RefSpec(mainRef.getName()))
				.setCredentialsProvider(credentialsProvider).call(); // Fetch only the target branch

			System.out.println("Fetch complete.");

			// FIXME ai generated from here on
			// 3. Find the latest commit on the desired branch
			/*
			 * Ref head = repository.findRef(mainRef.getName()); if (head == null) {
			 * System.err.println("Ref " + mainRef.getName() + " not found."); for(Ref ref :
			 * repository.getRefDatabase().getRefs()) { System.err.println(ref.toString());
			 * } return; }
			 */

			RevWalk revWalk = new RevWalk(repository);
			RevCommit commit = revWalk.parseCommit(mainRef.getObjectId());
			RevTree tree = commit.getTree();

			// 4. Walk the tree to find .java files
			TreeWalk treeWalk = new TreeWalk(repository);
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true); // Traverse subdirectories
			treeWalk.setFilter(PathSuffixFilter.create(".java")); // Filter for .java files

			System.out.println("\nFound .java files and their contents:");
			while (treeWalk.next()) {
				// Get the ObjectId of the current blob (file)
				ObjectId objectId = treeWalk.getObjectId(0);

				// Open the object loader to read the blob content
				ObjectLoader loader = repository.open(objectId);

				// Read the content into a byte array or process with an InputStream
				// TODO reject unprintable chars
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					loader.copyTo(out);
					String fileContent = out.toString("UTF-8"); // Assuming UTF-8 encoding

					System.out.println("--- File: " + treeWalk.getPathString() + " ---");
					System.out.println(fileContent.substring(0, Math.min(fileContent.length(), 200)) + "..."); // Print
																												// first
																												// 200
																												// chars
					System.out.println("------------------------------------------");
				}
			}
			revWalk.dispose(); // Close the RevWalk

			repository.close(); // Close the temporary repository

		} catch (NoRemoteRepositoryException e) {
			System.err.println("Error: Remote repository not found at " + remoteUrl);
			e.printStackTrace();
			/*
			 * } catch (Exception e) { System.err.println("An error occurred: " +
			 * e.getMessage()); e.printStackTrace();
			 */
		} finally {
			// Clean up the temporary directory
			if (tempGitDir != null && tempGitDir.exists()) {
				try {
					System.out.println("Deleting temporary directory: " + tempGitDir.getAbsolutePath());
					FileUtils.delete(tempGitDir, FileUtils.RECURSIVE);
					System.out.println("Temporary directory deleted.");
				} catch (IOException e) {
					System.err.println("Error deleting temporary directory: " + e.getMessage());
				}
			}
		}
	}
}