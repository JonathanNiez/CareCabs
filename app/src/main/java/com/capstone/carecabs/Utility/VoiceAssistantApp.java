package com.capstone.carecabs.Utility;

import android.app.Application;

public class VoiceAssistantApp extends Application {
	private VoiceAssistant voiceAssistant;

	@Override
	public void onCreate() {
		super.onCreate();
		voiceAssistant = VoiceAssistant.getInstance(this);
	}

	public VoiceAssistant getVoiceAssistant() {
		return voiceAssistant;
	}
}
