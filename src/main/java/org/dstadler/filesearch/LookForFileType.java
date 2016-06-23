package org.dstadler.filesearch;

import java.io.File;
import java.io.IOException;

public class LookForFileType {
    public static void main(String[] args) throws IOException {
        // walk all arguments
        for(String location : args) {
            FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
            walker.execute(new File(location));
        }
    }
}
