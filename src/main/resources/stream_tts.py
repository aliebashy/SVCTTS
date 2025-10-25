import os
import wave
import sys
from piper import PiperVoice
from piper import SynthesisConfig

with open("python_debug.txt", "w") as f:
    f.write("Python CWD: {}\n".format(os.getcwd()))

# Things to do: 
# Download voice upon Selection if not present
# set default values for arguments
# 

message = sys.argv[1]
voiceChoice = sys.argv[2]                       # Path to .onnx File
voiceVolume = float(sys.argv[3])                # How Loud?
voiceLength = float(sys.argv[4])                # How Slow?
voiceVariation = float(sys.argv[5])             # Audio Variation
voiceWVariation = float(sys.argv[6])            # Speaking Variation
voiceNormalize = sys.argv[7].lower() == "true"  # Normalize Audio?


voice = PiperVoice.load(voiceChoice)

syn_config = SynthesisConfig(
        volume=1.0,     # voiceVolume,  # half as loud
        length_scale=1.0,     # voiceLength,  # twice as slow
        noise_scale=1.0,     # voiceVariation,  # more audio variation
        noise_w_scale=1.0,     # voiceWVariation,  # more speaking variation
        normalize_audio=False     # voiceNormalize, # use raw audio from voice
)

#set_audio_format(
#voice.sample_rate,  # from model
#2,                  # 16 bit audio -> 2 bytes / sample
#1                   # mono
#)

sample_rate = getattr(voice, "sample_rate", 22050)
# header_line = f"RATE:{sample_rate}\n"
# sys.stdout.write(header_line)
# sys.stdout.flush()

for chunk in voice.synthesize(message, syn_config=syn_config):
    # set_audio_format(chunk.sample_rate, chunk.sample_width, chunk.sample_channels)
    # print(f"Chunk size: {len(chunk.audio_int16_bytes)}", file=sys.stderr)
    sys.stdout.buffer.write(chunk.audio_int16_bytes)
    sys.stdout.buffer.flush()  

# Test Region to Ensure the full TTS is generating

#with wave.open("test_output1.wav", "wb") as wf:
#    wf.setnchannels(1)          # mono
#    wf.setsampwidth(2)          # 16-bit
#    wf.setframerate(sample_rate)

#    for chunk in voice.synthesize(message, syn_config=syn_config):
#        wf.writeframes(chunk.audio_int16_bytes)
