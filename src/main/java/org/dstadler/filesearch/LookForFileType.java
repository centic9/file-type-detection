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
            FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
            walker.execute(new File(location));
        }
    }
}
