package com.capstone.carecabs.Utility;

public class IDTypeIdentifier {

    public static String identifyIDType(String extractedText) {
        // Convert the extracted text to lowercase for case-insensitive matching
        extractedText = extractedText.toLowerCase();

        // Keywords and patterns for PWD and Senior Citizen IDs
        String[] pwdKeywords = {"person with disability", "pwd", "disability id"};
        String[] seniorKeywords = {"senior citizen", "senior id"};

        // Check for PWD keywords
        for (String keyword : pwdKeywords) {
            if (extractedText.contains(keyword)) {
                return "PWD";
            }
        }

        // Check for Senior Citizen keywords
        for (String keyword : seniorKeywords) {
            if (extractedText.contains(keyword)) {
                return "Senior Citizen";
            }
        }

        // If no keywords are matched, return null
        return null;
    }
}
