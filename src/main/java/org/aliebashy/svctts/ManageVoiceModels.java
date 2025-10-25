package org.aliebashy.svctts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ManageVoiceModels {

    /**
     * Returns the absolute path to the ONNX model for the given voice.
     * If the model is not found, it will attempt to download it using
     * Piper's built-in downloader.
     *
     * @param voiceName Example: "en_US-lessac-medium"
     * @return Absolute path to model.onnx
     * @throws IOException If download fails or path cannot be resolved
     * @throws InterruptedException If Python process is interrupted
     */
    public static String getVoiceModelPath(String voiceName) throws IOException, InterruptedException {
        String osName = System.getProperty("os.name").toLowerCase();

        File voiceRoot;
        if (osName.contains("win")) {
            voiceRoot = Paths.get(System.getenv("APPDATA"), "piper", "voices").toFile();
        } else {
            voiceRoot = Paths.get(System.getProperty("user.home"), ".local", "share", "piper", "voices").toFile();
        }

        File modelFile = new File(voiceRoot, voiceName+".onnx");

        // Check if the model already exists
        if (modelFile.exists()) {
            return modelFile.getAbsolutePath();
        }

        // Model not found, attempt to download using Piper CLI
        System.out.println("[SVCTTS] Voice model not found. Downloading: " + voiceName);
        String pythonExe = osName.contains("win") ? "python" : "python3";
        

        ProcessBuilder pb = new ProcessBuilder(
                pythonExe,
                "-m", 
                "piper.download_voices",
                voiceName,
                "--download-dir", voiceRoot.getAbsolutePath()
        );
        pb.inheritIO(); // Show download progress in console
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to download voice model: " + voiceName);
        }

        if (!modelFile.exists()) {
            throw new IOException("Voice model still not found after download: " + modelFile.getAbsolutePath());
        }

        System.out.println("[SVCTTS] Voice model ready: " + modelFile.getAbsolutePath());
        return modelFile.getAbsolutePath();
    }
}
