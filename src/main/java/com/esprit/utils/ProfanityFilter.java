package com.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for detecting and filtering profane content
 */
public class ProfanityFilter {

    private static final Set<String> PROFANITY_LIST = new HashSet<>();

    // Initialize the profanity list
    static {
        try {
            // Load English profanities from JSON file
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = ProfanityFilter.class.getResourceAsStream("/profane-words.json");
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String jsonContent = reader.lines().collect(Collectors.joining());
                    PROFANITY_LIST.addAll(mapper.readValue(jsonContent, new TypeReference<Set<String>>() {
                    }));
                }
            }

            // Load French profanities from text file
            InputStream frenchIs = ProfanityFilter.class.getResourceAsStream("/french-badwords.txt");
            if (frenchIs != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(frenchIs, StandardCharsets.UTF_8))) {
                    PROFANITY_LIST.addAll(
                            reader.lines()
                                    .map(String::trim)
                                    .filter(line -> !line.isEmpty())
                                    .collect(Collectors.toSet())
                    );
                }
            }

            // Add Arabic profanities
            PROFANITY_LIST.addAll(Arrays.asList(
                    "كس", "طيز", "زب", "شرموط", "قحبة", "خول", "منيوك", "كلب", "حمار", "عير",
                    "خرا", "خرة", "زبي", "نيك", "كسمك", "متناك", "عرص", "لبوة", "شاذ", "لواط",
                    "حقير", "ابن الكلب", "ابن المتناكة", "ابن القحبة", "كس امك", "كس اختك", 
                    "الكلب", "المتناك", "القحبة", "الزب", "الطيز", "الكس", "الكلب"));

        } catch (Exception e) {
            // If loading fails, fall back to a minimal list
            System.err.println("Failed to load profane words: " + e.getMessage());
            PROFANITY_LIST.add("fuck");
            PROFANITY_LIST.add("shit");
            // Add other minimal profane words as a fallback
        }
    }

    // Regex pattern to match leet speak and character substitutions
    private static final Pattern LEET_PATTERN = Pattern.compile(
            "\\b[\\w]*("
            + String.join("|", PROFANITY_LIST.stream()
                    .filter(word -> !word.matches(".*[\\u0600-\\u06FF].*")) // Filter out Arabic words for leet speak
                    .map(word -> word
                    .replace("a", "[aà@4]")
                    .replace("e", "[eéèê3]")
                    .replace("i", "[iî1!]")
                    .replace("o", "[oô0]")
                    .replace("s", "[s$5]")
                    .replace("t", "[t7]")
                    .replace("l", "[l1]")
                    ).toArray(String[]::new)
            )
            + ")[\\w]*\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Checks if the given text contains profanity
     *
     * @param text Text to check
     * @return true if profanity is detected, false otherwise
     */
    public static boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Direct word matching
        for (String word : PROFANITY_LIST) {
            // Check for whole word matches using word boundaries
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                return true;
            }
        }

        // Check for leet speak and character substitutions (only for non-Arabic words)
        return LEET_PATTERN.matcher(text).find();
    }

    /**
     * Returns a clean version of the text with profanity replaced by asterisks
     *
     * @param text Text to clean
     * @return Text with profanity censored
     */
    public static String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Replace whole profane words with asterisks
        for (String word : PROFANITY_LIST) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b",
                    "*".repeat(word.length()));
        }

        // Also handle leet speak variants
        return result;
    }
}
