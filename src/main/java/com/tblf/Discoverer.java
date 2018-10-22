package com.tblf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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

            new ProcessBuilder()
                    .command("sh", discoverylauncher.getAbsolutePath(), project.getAbsolutePath())
                    .redirectOutput(File.createTempFile("modisco", ".log"))
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
