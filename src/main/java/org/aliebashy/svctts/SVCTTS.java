package org.aliebashy.svctts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


import javax.sound.sampled.*;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;






@Mod("svctts")
@Mod.EventBusSubscriber(modid = "svctts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SVCTTS {

    //private static final String STREAM_TTS_PATH = new File("stream_tts.py").getAbsolutePath();





    public SVCTTS() {
        System.out.println("[SVCTTS] Mod Initialized.");
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

        // Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonProcess != null) {
                System.out.println("[SVCTTS] Shutting down persistent Python process...");
                pythonProcess.destroy();
            }
        }));
    }

    private static synchronized void ModInitialization() {
        // Initialize Python if not already
        if (pythonExe == null) {
            ensurePython();
            if (pythonExe == null) {
                System.err.println("[SVCTTS] Cannot run TTS: Python not found");
                return;
            }
        }

        // Load config values if not already loaded
        if (voice == null || modelPath == null) {
            refreshConfig();
            if (modelPath == null) {
                System.err.println("[SVCTTS] Cannot run TTS: modelPath is null");
                return;
            }
        }

        // Start persistent Python if not already running
        if (pythonProcess == null) {
            startPersistentPython(modelPath);
        }
    }

    private static String pythonExe;

    private static void ensurePython() {
        if (pythonExe != null) return; // already detected

        // 1. User-configured path
        if (!Config.PYTHON_PATH.get().isEmpty()) {
            pythonExe = Config.PYTHON_PATH.get();
        } else {
            // 2. Try python3, then python
            pythonExe = getValidPython("python3");
            if (pythonExe == null) pythonExe = getValidPython("python");
        }

        // 3. No valid Python found
        if (pythonExe == null) {
            System.err.println("[SVCTTS] Python not found! TTS will not work.");

            Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    LocalPlayer player = mc.player;
                    if (player != null) {
                        player.displayClientMessage(
                            Component.literal("[SVCTTS] Python not found! Install Python 3.11+ for TTS."),
                        false // false = not an action bar, just chat
                        );
                    }
                });
            return;
        }  

        // 4. Debug print
        System.out.println("[SVCTTS] Python detected: " + pythonExe);

        // 5. Ensure Piper installed
        if (!isPythonPackageInstalled("piper-tts")) {
            installPythonPackage("piper-tts");
        }
    }

    private static String getValidPython(String cmd) {
        /**
        * Checks if a Python executable is usable and returns its full path.
        * Returns null if not valid.
        */
        try {
            // Get full path
            Process p = new ProcessBuilder(cmd, "-c", "import sys; print(sys.executable)").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String fullPath = reader.readLine();
            p.waitFor();

            // Not found or empty output
            if (fullPath == null || fullPath.isEmpty()) return null;

            // Ignore Microsoft Store stub
            if (fullPath.toLowerCase().contains("windowsapps\\pythonsoftwarefoundation")) return null;

            // Verify --version works
            Process versionCheck = new ProcessBuilder(fullPath, "--version").start();
            versionCheck.waitFor();
            if (versionCheck.exitValue() != 0) return null;

            return fullPath; // valid Python path
        } catch (Exception e) {
            return null;
        }   
    }

    private static boolean isPythonPackageInstalled(String packageName) {
    try {
        Process p = new ProcessBuilder(pythonExe, "-m", "pip", "show", packageName)
                .redirectErrorStream(true)
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        boolean installed = reader.readLine() != null;
        p.waitFor();
        return installed;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void installPythonPackage(String packageName) {
    try {
        System.out.println("[SVCTTS] Installing Python package: " + packageName);
        Process p = new ProcessBuilder(pythonExe, "-m", "pip", "install", packageName)
                .inheritIO() // optional: shows installation output
                .start();
        p.waitFor();
        } catch (Exception e) {
            System.err.println("[SVCTTS] Failed to install " + packageName);
            e.printStackTrace();
        }
    }

    private static File getPythonScriptFile() throws IOException {
        // Try to find the resource in the jar or classpath
        InputStream resourceStream = SVCTTS.class.getClassLoader().getResourceAsStream("stream_tts.py");
        if (resourceStream == null) {
            throw new IOException("stream_tts.py resource not found in jar/classpath!");
        }

        // Copy to a temporary file
        File tempFile = File.createTempFile("stream_tts", ".py");
        tempFile.deleteOnExit(); // optional: deletes on JVM exit

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }

        return tempFile;
    }

    
    private static String voice;
    private static double volume;
    private static double length;
    private static double variation;
    private static double wVariation;
    private static boolean normalize;
    private static String modelPath;

    private static void refreshConfig() {
        voice = Config.VOICE.get();
        volume = Config.VOLUME.get();
        length = Config.LENGTH.get();
        variation = Config.VARIATION.get();
        wVariation = Config.W_VARIATION.get();
        normalize = Config.NORMALIZE.get();

        try {
            modelPath = ManageVoiceModels.getVoiceModelPath(voice);
        } catch (IOException | InterruptedException e) { // catch both exceptions
            e.printStackTrace();
            modelPath = null; // fallback
        }
    }

    private static Process pythonProcess;
    private static BufferedWriter pythonWriter;
    private static InputStream pythonAudioStream;

    private static void startPersistentPython(String voiceModelPath) {
        if (pythonProcess != null) { // Already Running Process
            System.out.println("[SVCTTS] Persistent Python already running with PID: " + pythonProcess.pid());
            return; 
        } 
        if (voiceModelPath == null) {
            System.err.println("[SVCTTS] Persistent Python not started: modelPath is null");
            return;
        }

        try {
            File scriptFile = getPythonScriptFile();
            ProcessBuilder pb = new ProcessBuilder(
                pythonExe, 
                scriptFile.getAbsolutePath(),
                voiceModelPath,
                String.valueOf(Config.VOLUME.get()),
                String.valueOf(Config.LENGTH.get()),
                String.valueOf(Config.VARIATION.get()),
                String.valueOf(Config.W_VARIATION.get()),
                String.valueOf(Config.NORMALIZE.get()),
                "--persistent"
            );
            pb.redirectErrorStream(false);
            pythonProcess = pb.start();
            pythonWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            pythonAudioStream = pythonProcess.getInputStream();

            // start a thread to log stderr / debug info
            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[Python] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            pythonProcess = null;
        }

        // Start Audio Playback thread
        startAudioPlayback();
    }

    private static void startAudioPlayback() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(22050, 16, 1, true, false); // PCM 16-bit LE, mono
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;

                while (pythonProcess != null && pythonProcess.isAlive()) {
                    if ((bytesRead = pythonAudioStream.read(buffer)) != -1) {
                        // Ensure only complete frames are written
                        int frameSize = format.getFrameSize(); // 2 bytes
                        int bytesToWrite = bytesRead - (bytesRead % frameSize);
                        if (bytesToWrite > 0) {
                            line.write(buffer, 0, bytesToWrite);
                        }
                    } else {
                        Thread.sleep(10); // no data, avoid busy loop
                    }
                }

                line.drain();
                line.stop();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void reloadPersistentPython() {
        if (pythonProcess != null) {
            pythonProcess.destroy(); // kill old process
            pythonProcess = null;
        }
        refreshConfig();
        startPersistentPython(modelPath);
    }

    private static void sendMessageToPython(String message) {
        if (pythonProcess == null || !pythonProcess.isAlive()) {
            System.err.println("[SVCTTS] Persistent Python process not running. Restarting...");
            pythonProcess = null; // clear stale reference
            refreshConfig();
            startPersistentPython(modelPath);
        }
        if (pythonProcess == null || !pythonProcess.isAlive()) {
            System.err.println("[SVCTTS] Failed to start persistent Python process.");
            return;
        }

        try {
            pythonWriter.write(message);
            pythonWriter.newLine();
            pythonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            pythonProcess = null; // mark process as gone
        }
    }


    @SubscribeEvent
    public static void onChatMessage(ClientChatEvent event) {
        String message = event.getMessage();

        System.out.println("[SVCTTS] Message captured: " + message);

        // prevent message from going to chat
        event.setCanceled(true);


        // Run TTS async so MC doesn't freeze
        new Thread(() -> speakText(message)).start();
    }

    private static void speakText(String message) {
        ModInitialization(); // make sure everything is ready
        if (pythonExe == null || modelPath == null) return;

        if (pythonProcess != null) {
            sendMessageToPython(message);
        } else {
            runOneShotPython(message);
        }
    }

    private static void runOneShotPython(String message) {
        if (modelPath == null) {
            System.err.println("[SVCTTS] Cannot run one-shot TTS: modelPath is null");
            return;
        }
        
        try {
            File scriptFile = getPythonScriptFile();
            ProcessBuilder pb = new ProcessBuilder(
                pythonExe,
                scriptFile.getAbsolutePath(),
                message,
                modelPath,
                String.valueOf(volume),
                String.valueOf(length),
                String.valueOf(variation),
                String.valueOf(wVariation),
                String.valueOf(normalize)
            );
            //pb.directory(new File("src/main/resources"));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            InputStream rawAudio = process.getInputStream();

            // Playback PCM audio (same as before)
            AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            int bytesRead;
            int frameSize = format.getFrameSize();

            while ((bytesRead = rawAudio.read(buffer)) != -1) {
                int bytesToWrite = bytesRead - (bytesRead % frameSize);
                if (bytesToWrite > 0) line.write(buffer, 0, bytesToWrite);
            }

            line.drain();
            line.stop();
            line.close();

        } catch (Exception e) {
            System.err.println("[SVCTTS] Error during one-shot TTS:");
            e.printStackTrace();
        }
    }
}