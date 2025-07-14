package vn.fpt.seima.seimaserver.service;

import com.google.firebase.messaging.BatchResponse;

import java.util.List;
import java.util.Map;

public interface FcmService {

    BatchResponse sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data);
}
