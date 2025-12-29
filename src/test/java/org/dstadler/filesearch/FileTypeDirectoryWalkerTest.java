package org.dstadler.filesearch;

import org.dstadler.commons.collections.MappedCounter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeDirectoryWalkerTest {
    @Test
    void testLocalFiles() throws IOException, InterruptedException {
        FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
        File startDir = new File(".");
        MappedCounter<String> stats = walker.execute(startDir);

        assertTrue(stats.sortedMap().size() >= 10,
                "Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap());
        assertTrue(stats.sortedMap().values().stream().mapToLong(value -> value).sum() > 40,
                "Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap());
        assertTrue(stats.get("text/x-java-source") >= 3,
                "Had: " + startDir.getAbsolutePath() + " and " + stats.sortedMap().keySet() + " and " + stats.sortedMap());

        assertEquals(1, walker.getErrorCount(),
                "The invalid symbolic link should lead to an error");
    }

    @Test
    void testInvalidDirectory() {
        FileTypeDirectoryWalker walker = new FileTypeDirectoryWalker();
        File startDir = new File("./notexisting");
        assertThrows(IllegalStateException.class, () -> walker.execute(startDir));
    }
}
