package vn.fpt.seima.seimaserver.service.impl;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.service.SpeechToTextService;

import java.io.IOException;
@Service
public class SpeechToTextServiceImpl implements SpeechToTextService {

    @Autowired
    private SpeechClient speechClient;

    @Override
    public String transcribeAudio(MultipartFile file) throws IOException {
        byte [] audioBytes = file.getBytes();
        ByteString audioData = ByteString.copyFrom(audioBytes);

        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.MP3) // Thay đổi định dạng nếu cần
                .setSampleRateHertz(16000) // Tần số mẫu
                .setLanguageCode("vi-VN")  // Ngôn ngữ Tiếng Việt
                .build();
        // Tạo đối tượng nhận diện
        RecognitionAudio recognitionAudio = RecognitionAudio.newBuilder()
                .setContent(audioData)
                .build();
        RecognizeResponse response = speechClient.recognize(config, recognitionAudio);

        // Xử lý kết quả trả về
        StringBuilder transcript = new StringBuilder();
        for (SpeechRecognitionResult result : response.getResultsList()) {
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            transcript.append(alternative.getTranscript());
        }

        return transcript.toString();
    }
}
