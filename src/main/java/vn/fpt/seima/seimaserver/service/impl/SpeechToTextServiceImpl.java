package vn.fpt.seima.seimaserver.service.impl;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.service.SpeechToTextService;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class SpeechToTextServiceImpl implements SpeechToTextService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceImpl.class);

    @Autowired
    private SpeechClient speechClient;

    private static final int TARGET_SAMPLE_RATE = 16000;

    @Override
    public String transcribeAudio(MultipartFile file) throws IOException {
        Path originalTempFile = null;
        File convertedAudioFile = null;
        try {
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
            originalTempFile = Files.createTempFile(UUID.randomUUID().toString(), originalFileName);
            file.transferTo(originalTempFile.toFile());

            convertedAudioFile = File.createTempFile(UUID.randomUUID().toString(), ".wav");

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setChannels(1);
            audio.setSamplingRate(TARGET_SAMPLE_RATE);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            logger.info("Normalizing audio file from {} to {}", originalTempFile, convertedAudioFile.getAbsolutePath());
            encoder.encode(new MultimediaObject(originalTempFile.toFile()), convertedAudioFile, attrs);
            logger.info("Successfully normalized audio.");

            byte[] audioBytes = Files.readAllBytes(convertedAudioFile.toPath());
            ByteString audioData = ByteString.copyFrom(audioBytes);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(TARGET_SAMPLE_RATE)
                    .setLanguageCode("vi-VN")
                    .setAudioChannelCount(1)
                    .build();

            RecognitionAudio recognitionAudio = RecognitionAudio.newBuilder()
                    .setContent(audioData)
                    .build();

            logger.info("Sending normalized audio to Google Speech-to-Text API...");
            RecognizeResponse response = speechClient.recognize(config, recognitionAudio);
            logger.info("Received response from API.");

            StringBuilder transcript = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcript.append(alternative.getTranscript());
            }

            return transcript.toString();

        } catch (Exception e) {
            logger.error("Error during audio transcription process", e);
            throw new IOException("Failed to transcribe audio", e);
        } finally {
            try {
                if (originalTempFile != null) {
                    Files.deleteIfExists(originalTempFile);
                }
                if (convertedAudioFile != null && convertedAudioFile.exists()) {
                    Files.deleteIfExists(convertedAudioFile.toPath());
                }
                logger.debug("Cleaned up temporary files.");
            } catch (IOException e) {
                logger.error("Error cleaning up temporary files", e);
            }
        }
    }
}
