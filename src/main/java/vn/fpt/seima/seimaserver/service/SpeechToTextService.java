package vn.fpt.seima.seimaserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SpeechToTextService {
    String transcribeAudio(MultipartFile file) throws IOException;
}
