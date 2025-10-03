package it.unibs.ingsw24_25.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.unibs.ingsw24_25.model.*;

import java.lang.reflect.Type;
import java.util.Map;

public final class JSONSupport {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Tipi per la (de)serializzazione delle mappe
    private static final Type CONFIG_MAP_TYPE = new TypeToken<Map<String, Configurator>>(){}.getType ();
    private static final Type VOL_MAP_TYPE = new TypeToken<Map<String, Volunteer>>(){}.getType();
    private static final Type PLACE_MAP_TYPE = new TypeToken<Map<String, Place>>(){}.getType();
    private static final Type VISIT_MAP_TYPE = new TypeToken<Map<String, VisitType>>(){}.getType();

    private JSONSupport() {
        // Costruttore privato: classe utility
    }

    // ---------- Configurator ----------
    public static String serializeConfigurator(Map<String, Configurator> map){
        return GSON.toJson (map, CONFIG_MAP_TYPE);
    }
    public static Map<String, Configurator> deserializeConfigurator(String json){
        return GSON.fromJson (json, CONFIG_MAP_TYPE);
    }

    // ---------- Volunteer ----------
    public static String serializeVolunteerMap(Map<String, Volunteer> map) {
        return GSON.toJson(map, VOL_MAP_TYPE);
    }

    public static Map<String, Volunteer> deserializeVolunteerMap(String json) {
        return GSON.fromJson(json, VOL_MAP_TYPE);
    }

    // ---------- Place ----------
    public static String serializePlaceMap(Map<String, Place> map) {
        return GSON.toJson(map, PLACE_MAP_TYPE);
    }

    public static Map<String, Place> deserializePlaceMap(String json) {
        return GSON.fromJson(json, PLACE_MAP_TYPE);
    }

    // ---------- VisitType ----------
    public static String serializeVisitTypeMap(Map<String, VisitType> map) {
        return GSON.toJson(map, VISIT_MAP_TYPE);
    }

    public static Map<String, VisitType> deserializeVisitTypeMap(String json) {
        return GSON.fromJson(json, VISIT_MAP_TYPE);
    }

    // ---------- SystemSettings (singolo oggetto) ----------
    public static String serializeSystemSettings(SystemSettings settings) {
        return GSON.toJson(settings);
    }

    public static SystemSettings deserializeSystemSettings(String json) {
        return GSON.fromJson(json, SystemSettings.class);
    }
}
