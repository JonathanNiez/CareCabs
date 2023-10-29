package com.capstone.carecabs.Utility;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class VoiceAssistant {
	private final String TAG = "VoiceAssistant";
	private static VoiceAssistant instance;
	private TextToSpeech textToSpeech;

	private VoiceAssistant(Context context) {
		textToSpeech = new TextToSpeech(context, status -> {
			if (status == TextToSpeech.SUCCESS) {
				Log.i(TAG, "TTS initialization success");
			} else {
				Log.e(TAG, "TTS initialization failed");
			}
		});
	}

	public static synchronized VoiceAssistant getInstance(Context context) {
		if (instance == null) {
			instance = new VoiceAssistant(context.getApplicationContext());
		}
		return instance;
	}

	public void speak(String text) {
		textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
	}

	public void shutdown() {
		if (textToSpeech != null) {
			textToSpeech.stop();
			textToSpeech.shutdown();
		}
		instance = null;
	}
}
