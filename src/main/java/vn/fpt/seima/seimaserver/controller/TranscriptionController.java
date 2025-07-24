package vn.fpt.seima.seimaserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.service.SpeechToTextService;

@RestController
@RequestMapping("/api/v1/transcription")
public class TranscriptionController {

    @Autowired
    private SpeechToTextService speechToTextService;

    @PostMapping(value= "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> transcribeAudio(
            @RequestParam MultipartFile file) {
        try {
            String transcription = speechToTextService.transcribeAudio(file);
            return new ApiResponse<>(HttpStatus.CREATED.value(), "Transcription successful", transcription);
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Transcription failed: " + e.getMessage(), null);
        }
    }
}

