package org.dstadler.filesearch;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class ExtractText {
	private static final Tika tika = new Tika();

	public static void main(String[] args) throws TikaException, IOException {
		for (String file : args) {
			System.out.println("Fetching text of " + file);
			try (InputStream str = new BufferedInputStream(new FileInputStream(file), 1024*1024)) {
				System.out.println(tika.parseToString(str));
			}
		}
	}
}
