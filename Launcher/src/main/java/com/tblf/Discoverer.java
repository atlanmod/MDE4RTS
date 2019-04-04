package com.tblf;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Discoverer implements Runnable {

    private File project;

    public Discoverer(File project) {
        this.project = project;
    }

    @Override
    public void run() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("discoverer.sh");

        try {

            if (!project.exists()) {
                throw new FileNotFoundException("could not run the discovery on "+project.getAbsolutePath()+" : project could not be found");
            }

            String command = IOUtils.toString(inputStream, Charset.defaultCharset());

            File file = File.createTempFile("discoverer", ".sh");
            IOUtils.write(command, new FileOutputStream(file), Charset.defaultCharset());

            Process process =new ProcessBuilder()
                    .command("sh", file.getAbsolutePath(), project.getAbsolutePath())
                    .inheritIO()
                    .start();

            process.waitFor();

            file.delete();
        } catch (IOException | InterruptedException e) {
            Logger.getLogger("Discoverer").log(Level.WARNING, "Could not discover the source code", e);
        }
    }
}
