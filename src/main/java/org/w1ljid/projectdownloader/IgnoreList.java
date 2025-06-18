package org.w1ljid.projectdownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class IgnoreList implements AutoCloseable {

	private final File file;
	private final HashSet<String> ignored = new HashSet<String>();
	private FileWriter writer;


	public IgnoreList(File backingFile) {
		this.file = backingFile;
	}


	public void initialize() throws IOException {
		ignored.clear();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				ignored.add(line);
			}
		} catch (FileNotFoundException _e) {
			file.createNewFile();
		}

		writer = new FileWriter(file, true);
	}

	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}


	public int size() {
		return ignored.size();
	}

	public boolean isIgnored(String string) {
		return ignored.contains(string);
	}

	public void ignore(String string) throws IOException {
		ignored.add(string);
		writer.write(string);
		writer.write(System.lineSeparator());
	}

}
