import os
import wave
import sys
from piper import PiperVoice
from piper import SynthesisConfig

print("Args:", sys.argv, file=sys.stderr)
persistent_mode = "--persistent" in sys.argv
print("Persistent mode:", persistent_mode, file=sys.stderr)


def generate_audio(message, voice, volume, length, variation, w_variation, normalize):
    # Generates Audio for a single message using Piper

    syn_config = SynthesisConfig(
        volume=volume,  
        length_scale=length,  
        noise_scale=variation,  
        noise_w_scale=w_variation,  
        normalize_audio=normalize, 
    )

    for chunk in voice.synthesize(message, syn_config=syn_config):
        # set_audio_format(chunk.sample_rate, chunk.sample_width, chunk.sample_channels)
        # print(f"Chunk size: {len(chunk.audio_int16_bytes)}", file=sys.stderr)
        sys.stdout.buffer.write(chunk.audio_int16_bytes)
        sys.stdout.buffer.flush()

# --- Main ---

voiceChoice = sys.argv[1]                       # Path to .onnx File
voiceVolume = float(sys.argv[2])                # How Loud?
voiceLength = float(sys.argv[3])                # How Slow?
voiceVariation = float(sys.argv[4])             # Audio Variation
voiceWVariation = float(sys.argv[5])            # Speaking Variation
voiceNormalize = sys.argv[6].lower() == "true"  # Normalize Audio?
persistent_mode = "--persistent" in sys.argv    # Keeps Process Open for Speed Optimization

voice = PiperVoice.load(voiceChoice)

if persistent_mode:
    while True:
        message = sys.stdin.readline().strip()
        generate_audio(message, voice, voiceVolume, voiceLength, voiceVariation, voiceWVariation, voiceNormalize)

# --- First Load or One-shot ---
else:
    message = sys.argv[7]
    generate_audio(message, voice, voiceVolume, voiceLength, voiceVariation, voiceWVariation, voiceNormalize)





