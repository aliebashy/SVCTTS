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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;




@Mod("svctts")
@Mod.EventBusSubscriber(modid = "svctts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SVCTTS {

    public SVCTTS() {
        System.out.println("[SVCTTS] Mod Initialized.");
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

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



    @SubscribeEvent
    public static void onChatMessage(ClientChatEvent event) {
        String message = event.getMessage();

        System.out.println("[SVCTTS] Message captured: " + message);

        // prevent message from going to chat
        event.setCanceled(true);


        String voice = Config.VOICE.get();
        double volume = Config.VOLUME.get();
        double length = Config.LENGTH.get();
        double variation = Config.VARIATION.get();
        double wVariation = Config.W_VARIATION.get();
        boolean normalize = Config.NORMALIZE.get();
        // Run TTS async so MC doesn't freeze
        new Thread(() -> speakText(message, voice, volume, length, variation, wVariation, normalize)).start();
    }

    private static void speakText(String message, String voice, double volume, double length, double variation, double wVariation, boolean normalize) {
        if (pythonExe == null) { //Lazy Initialization for first time
            ensurePython();
        }
        if (pythonExe == null) { // If Still not found after ensurePython, there's an issue
            System.err.println("[SVCTTS] Python not found; skipping TTS.");
            return;
        } 
        
        try {
            
            // 1: Resolve Model Path, download if needed
            String modelPath = ManageVoiceModels.getVoiceModelPath(voice);
            
            // 2: Run Piper TTS with set parameters
            ProcessBuilder pb = new ProcessBuilder(
                pythonExe,
                "stream_tts.py",
                message,
                modelPath,
                String.valueOf(volume),
                String.valueOf(length),
                String.valueOf(variation),
                String.valueOf(wVariation),
                String.valueOf(normalize)
            );
            pb.directory(new File("C:/Users/Alie/Documents/Streaming/4. Games/Minecraft - Moding/SVCTTS_2/src/main/resources"));
            pb.redirectErrorStream(true);
            //pb.redirectOutput(new File("python_log.txt"));


            Process process = pb.start();
            InputStream rawAudio = process.getInputStream();        // stdout = audio
            InputStream debugStream = process.getErrorStream();     // stderr = prints


            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(debugStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[Python] " + line);
                }
                } catch (IOException e) {
                e.printStackTrace();
                }
            }).start();


            // 3: Playback PCM Audio from Piper in realtime
            // Matching Piper model format (adjust sample rate if needed)
            AudioFormat format = new AudioFormat(
                    22050, // sample rate of your Piper model
                    16,    // bits per sample
                    1,     // mono
                    true,  // signed
                    false  // little-endian
            );

            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            int bytesRead;
            int frameSize = format.getFrameSize(); // 2 bytes

            int totalBytes = 0;
            while ((bytesRead = rawAudio.read(buffer)) != -1) {
                int bytesToWrite = bytesRead - (bytesRead % frameSize);
                System.out.println("[Java] Read Bytes: " + bytesRead + " Writing Bytes: " + bytesToWrite);
                totalBytes += bytesToWrite;
                if (bytesToWrite > 0){
                    line.write(buffer, 0, bytesToWrite);
                }
            }
            System.out.println("[Java] Total bytes received: " + totalBytes);

            line.drain();
            line.stop();
            line.close();

        } catch (Exception e) {
            System.err.println("[SVCTTS] Error during TTS playback:");
            e.printStackTrace();
        }
    }
}