package com.google.mlkit.vision.demo.java.objectdetector;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

    private static final String TAG = "ObjectDetectorProcessor";
    private final Context context;
    private final ObjectDetector detector;
    TextToSpeech textToSpeech;

    public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        this.context = context;
        detector = ObjectDetection.getClient(options);
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<DetectedObject>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(@NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {
        JSONArray objectList = new JSONArray();
        for (DetectedObject object : results) {
            if (object.getLabels().size() > 0) {
                String objectName = object.getLabels().get(0).getText();
                textToSpeech.speak(objectName, TextToSpeech.QUEUE_FLUSH, null);
                objectList.put(objectName);
            }
            graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
        }

        sendPostRequest(objectList.toString());
    }

    public void sendPostRequest(String results) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String url = "https://proxy.tune.app/chat/completions";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("temperature", 1.0);
            jsonBody.put("model", "MeghanaM4/Optica");
            jsonBody.put("stream", false);
            jsonBody.put("max_tokens", 50);

            JSONArray messagesArray = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "From a list of objects, create a description of what is happening in the room...");
            messagesArray.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", results);
            messagesArray.put(userMessage);

            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", "");
            messagesArray.put(assistantMessage);

            jsonBody.put("messages", messagesArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        textToSpeech = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody, response -> {
//            textToSpeech.speak(response.toString(), TextToSpeech.QUEUE_FLUSH, null);
            textToSpeech.speak("AHHHHHHHHH", TextToSpeech.QUEUE_FLUSH, null);
        }, error -> {
            Log.e(TAG, String.valueOf(error));
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "sk-tune-x8Pz3FbV3HUWFpy6RKiq83NMVqPLFlCqaYn");
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }
}