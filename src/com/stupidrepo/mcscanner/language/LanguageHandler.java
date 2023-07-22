package com.stupidrepo.mcscanner.language;

import org.bson.BsonString;
import org.bson.json.JsonObject;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import java.util.logging.Level;

public class LanguageHandler {
    private final Hashtable<Object, Object> strings = new Hashtable<>();

    private Locale language;
    private final Locale fallbackLocale;

    private final Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

    public LanguageHandler(Locale fallback) {
        this.fallbackLocale = fallback;
        this.language = Locale.getDefault();
        this.loadStringsToDictionary();
    }

    public String get(String key) {
        if(this.strings.containsKey(key)) {
            return ((BsonString) this.strings.get(key)).getValue();
        } else {
            logger.log(Level.SEVERE, "No string found for key '%s'.".formatted(key));
            return key;
        }
    }

    public Locale getLanguage() {
        return this.language;
    }

    public Locale setLanguage(String language) {
        this.language = Locale.forLanguageTag(language);
        this.loadStringsToDictionary();
        return this.language;
    }

    public void loadStringsToDictionary() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("lang/%s.json".formatted(this.language.toLanguageTag()));

        if (is == null) { is = classLoader.getResourceAsStream("lang/%s.json".formatted(this.fallbackLocale.toLanguageTag())); }
        if (is == null) { logger.log(Level.SEVERE, "No language file found for %s or %s!".formatted(this.language.toLanguageTag(), this.fallbackLocale.toLanguageTag())); } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) { sb.append(line); }
                JsonObject json = new JsonObject(sb.toString());
                this.strings.putAll(json.toBsonDocument());
                logger.log(Level.INFO, "Loaded language file for %s!".formatted(this.language.toLanguageTag()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
