package com.tblf;

import com.tblf.utils.FileUtils;
import org.apache.commons.cli.ParseException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Char;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class CLITest {

    private File project;

    @Before
    public void setUp() throws IOException {
        File projectZip = new File("src/test/resources/ProjectForTestingAppClass.zip");
        FileUtils.unzip(projectZip);
        project = new File("src/test/resources/ProjectForTestingAppClass");
    }

    @Test
    public void testHelp() throws ParseException {
        App.main(new String[]{"-help"});
    }

    @Test
    public void testNoOptions() throws ParseException {
        App.main(new String[]{});
    }

    @Test
    public void testModel() throws ParseException, IOException {
        Assert.assertTrue(Files.walk(project.toPath()).noneMatch(path -> path.endsWith(".xmi")));
        App.main(new String[]{"-model", "-project", project.getAbsolutePath()});
        Assert.assertTrue(Files.walk(project.toPath()).anyMatch(path -> path.toString().endsWith(".xmi")));
    }

    @Test
    public void testRtsNoOption() throws ParseException, IOException {
        App.main(new String[]{"-model", "-project", project.getAbsolutePath()});
        File file = new File(project, "src/main/java/com/tblf/App.java");
        new App().buildImpactAnalysisModel(project);
        if (file.exists()) {
            String content = org.apache.commons.io.FileUtils.readFileToString(file, Charset.defaultCharset());
            content = content.replace("Hello World", "Hello Warld");
            org.apache.commons.io.FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = System.out;
        System.setOut(new PrintStream(byteArrayOutputStream));
        App.main(new String[]{"-rts", "-project", project.getAbsolutePath()});
        String[] strings = byteArrayOutputStream.toString().split("\n");
        Assert.assertEquals("com.tblf.AppTest$shouldAnswerWithTrue", strings[strings.length-1]);
        System.setOut(out);
    }

    @Test
    public void testRtsWithCommit() throws ParseException, IOException, GitAPIException {
        App.main(new String[]{"-model", "-project", project.getAbsolutePath()});
        File file = new File(project, "src/main/java/com/tblf/App.java");
        new App().buildImpactAnalysisModel(project);

        if (file.exists()) {
            String content = org.apache.commons.io.FileUtils.readFileToString(file, Charset.defaultCharset());
            content = content.replace("Hello World", "Hello Warld");
            org.apache.commons.io.FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
        }

        Git git = Git.open(project);
        RevCommit revCommit = git.log().call().iterator().next();
        Git.open(project).commit().setAll(true).setMessage("committed").call();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = System.out;
        System.setOut(new PrintStream(byteArrayOutputStream));

        App.main(new String[]{"-rts", revCommit.getName(), "-project", project.getAbsolutePath()});

        String[] strings = byteArrayOutputStream.toString().split("\n");
        Assert.assertEquals("com.tblf.AppTest$shouldAnswerWithTrue", strings[strings.length-1]);
        System.setOut(out);
    }

    @After
    public void tearDown() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(project);
    }
}
