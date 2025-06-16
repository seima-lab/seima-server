package vn.fpt.seima.seimaserver.service.impl;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.ocr.VisionConfig;
import vn.fpt.seima.seimaserver.service.OcrService;
import java.io.*;
import java.util.List;

@Service
public class OcrServiceImpl implements OcrService {

    @Override
    public String extractTextFromFile(MultipartFile file) throws Exception {

        VisionConfig.initGoogleCredentials();

        String credentialsPath = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
        GoogleCredentials credentials;
        try (FileInputStream credStream = new FileInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(credStream);
        }

        // 3. Tạo ImageAnnotatorClient với credentials thủ công
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
            ByteString imgBytes = ByteString.readFrom(file.getInputStream());

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(img)
                    .build();

            List<AnnotateImageResponse> responses = vision.batchAnnotateImages(List.of(request)).getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    throw new IOException("Lỗi từ Vision API: " + res.getError().getMessage());
                }
                return res.getFullTextAnnotation().getText();
            }
        }

        return "Không có văn bản nào được nhận diện.";
    }

}
