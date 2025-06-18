package org.w1ljid.projectdownloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.zip.CRC32;


public class FileLibrary {

	private static final char SEPARATOR = '-';
	private static final String LABEL_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789";
	private static final String UNDERSCORIFY_CHARS = " \n-/\\";
	private static final String SALT_CHARS = "0123456789abcdef";

	private final Random rand = new Random();
	private final File root;
	private final int saltLength = 16; // 8 bytes of salt should be enough for everyone.


	public FileLibrary(String rootPath) {
		root = new File(rootPath);
		root.mkdirs();
	}


	/**
	 * May return File.getAbsolutePath instead if getCanonicalPath decides to throw
	 * an IOException.
	 */
	public String getCanonicalRoot() {
		try {
			return root.getCanonicalPath();
		} catch (IOException ioe) {
			return root.getAbsolutePath();
		}
	}


	private static String cleanLabel(String label) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < Math.min(256, label.length()); i++) {
			char ch = label.charAt(i);
			if (UNDERSCORIFY_CHARS.indexOf(ch) >= 0) {
				sb.append('_');
			} else if (LABEL_CHARS.indexOf(ch) >= 0) {
				sb.append(ch);
			}
		}

		return sb.toString();
	}

	private String saltLabel(String label) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < saltLength; i++) {
			sb.append(SALT_CHARS.charAt(rand.nextInt(SALT_CHARS.length())));
		}
		sb.append(SEPARATOR);
		sb.append(label);

		return sb.toString();
	}

	public String suggestLabel(String path) {
		Object nameObj = Paths.get(path).getFileName(); // This can be null!
		if (nameObj == null) return "null";
		String name = nameObj.toString();

		int periodI = name.indexOf('.');
		if (periodI == 0) {
			return "empty";
		} else if (periodI > 0) {
			name = name.substring(0, periodI);
		}
		return name;
	}

	public String store(String category, String label, ByteArrayOutputStream bytes, String fileExtension) throws IOException {
		label = cleanLabel(label);

		File categoryDir = new File(root, category);
		categoryDir.mkdirs();

		File file;
		int tries = 0;
		while (true) {
			if (tries > 100) {
				System.err.println("Failed to create an unique file 100 times");
			}

			String saltedLabel = saltLabel(label);
			if (fileExtension != null) saltedLabel += fileExtension;
			file = new File(categoryDir, saltedLabel);
			if (file.createNewFile()) break; // Stop when we managed to create an unique file
			else tries++;
		}

		try (FileOutputStream fos /* XDDDDDDDD */ = new FileOutputStream(file)) {
			bytes.writeTo(fos);
		} catch (FileNotFoundException e) {
			throw new IOException("The file we just created did not exist, this should never happen", e);
		}

		return file.getPath();
	}

	public File getCustomLocation(String key, String extension) {
		StringBuilder sb = new StringBuilder();

		sb.append(cleanLabel(key));
		sb.append(SEPARATOR);

		CRC32 crc = new CRC32();
		crc.update(key.getBytes());

		sb.append(Long.toHexString(crc.getValue()));
		
		sb.append('.');
		sb.append(extension);

		return new File(root, sb.toString());
	}

}
