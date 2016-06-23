package org.dstadler.filesearch;

import java.io.File;

public class LookForFileType {
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            System.err.println("Usage: " + LookForFileType.class.getName() + " <directory> [<directory> ...]");
            return;
        }

        // walk all arguments
        for(String location : args) {
            long start = System.currentTimeMillis();
            System.err.println("Processing directory " + location);

            FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
            long count = walker.execute(new File(location));

            System.err.println("Found " + count + " files in directory '" + location + "', took " + (System.currentTimeMillis() - start) + "ms");
        }
    }
}
