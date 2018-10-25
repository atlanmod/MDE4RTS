package com.tblf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Discoverer implements Runnable {

    private File project;

    public Discoverer(File project) {
        this.project = project;
    }

    @Override
    public void run() {
        File discoverylauncher = new File("src/main/resources/discoverer.sh");

        try {

            if (!project.exists()  || !discoverylauncher.exists()) {
                throw new FileNotFoundException("could not run the discovery on "+project.getAbsolutePath());
            }

            Process process =new ProcessBuilder()
                    .command("sh", discoverylauncher.getAbsolutePath(), project.getAbsolutePath())
                    .inheritIO()
                    .start();

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            Logger.getLogger("Discoverer").log(Level.WARNING, "Could not discover the source code", e);
        }
    }
}
