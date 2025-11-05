package it.unibs.ingsw24_25.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.ProvisionedCredentialStore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;

public final class JSONSupport {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
            .registerTypeAdapter(YearMonth.class, new YearMonthAdapter())
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .setPrettyPrinting()
            .create();

    private static final class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return LocalDate.parse(json.getAsString());
        }
    }

    private static final class LocalTimeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        @Override
        public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return LocalTime.parse(json.getAsString());
        }
    }

    private static final class YearMonthAdapter implements JsonSerializer<YearMonth>, JsonDeserializer<YearMonth> {
        @Override
        public JsonElement serialize(YearMonth src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public YearMonth deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return YearMonth.parse(json.getAsString());
        }
    }

    private static final class DurationAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
        @Override
        public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return Duration.parse(json.getAsString());
        }
    }

    // Tipi per la (de)serializzazione delle mappe
    private static final Type CONFIG_MAP_TYPE = new TypeToken<Map<String, Configurator>>(){}.getType ();
    private static final Type VOL_MAP_TYPE = new TypeToken<Map<String, Volunteer>>(){}.getType();
    private static final Type PLACE_MAP_TYPE = new TypeToken<Map<String, Place>>(){}.getType();
    private static final Type VISIT_MAP_TYPE = new TypeToken<Map<String, VisitType>>(){}.getType();
    private static final Type PLAN_MAP_TYPE = new TypeToken<Map<String, MonthlyVisitPlan>>(){}.getType();
    private static final Type BENEFICIARY_MAP_TYPE = new TypeToken<Map<String, Beneficiary>>(){}.getType();
    private static final Type PROVISIONED_CREDENTIALS_TYPE = new TypeToken<ProvisionedCredentialStore>(){}.getType();

    private JSONSupport() {
        // Costruttore privato: classe utility
    }

    // ---------- Configurator ----------
    public static String serializeConfigurator(Map<String, Configurator> map){
        return GSON.toJson (map, CONFIG_MAP_TYPE);
    }
    public static Map<String, Configurator> deserializeConfigurator(String json){
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson (json, CONFIG_MAP_TYPE);
    }

    // ---------- Volunteer ----------
    public static String serializeVolunteerMap(Map<String, Volunteer> map) {
        return GSON.toJson(map, VOL_MAP_TYPE);
    }

    public static Map<String, Volunteer> deserializeVolunteerMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(json, VOL_MAP_TYPE);
    }

    // ---------- Place ----------
    public static String serializePlaceMap(Map<String, Place> map) {
        return GSON.toJson(map, PLACE_MAP_TYPE);
    }

    public static Map<String, Place> deserializePlaceMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(json, PLACE_MAP_TYPE);
    }

    // ---------- VisitType ----------
    public static String serializeVisitTypeMap(Map<String, VisitType> map) {
        return GSON.toJson(map, VISIT_MAP_TYPE);
    }

    public static Map<String, VisitType> deserializeVisitTypeMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(json, VISIT_MAP_TYPE);
    }

    // ---------- MonthlyVisitPlan ----------
    public static String serializeMonthlyVisitPlanMap(Map<String, MonthlyVisitPlan> map) {
        return GSON.toJson(map, PLAN_MAP_TYPE);
    }

    public static Map<String, MonthlyVisitPlan> deserializeMonthlyVisitPlanMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(json, PLAN_MAP_TYPE);
    }

    // ---------- Beneficiaries ----------
    public static String serializeBeneficiaryMap(Map<String, Beneficiary> map) {
        return GSON.toJson(map, BENEFICIARY_MAP_TYPE);
    }

    public static Map<String, Beneficiary> deserializeBeneficiaryMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(json, BENEFICIARY_MAP_TYPE);
    }


    // ---------- SystemSettings (singolo oggetto) ----------
    public static String serializeSystemSettings(SystemSettings settings) {
        return GSON.toJson(settings);
    }

    public static SystemSettings deserializeSystemSettings(String json) {
        if (isBlank(json)) {
            return null;
        }
        return GSON.fromJson(json, SystemSettings.class);
    }

    // ---------- Credenziali provisionate ----------
    public static String serializeProvisionedCredentials(ProvisionedCredentialStore store) {
        return GSON.toJson(store, PROVISIONED_CREDENTIALS_TYPE);
    }

    public static ProvisionedCredentialStore deserializeProvisionedCredentials(String json) {
        if (isBlank(json)) {
            return new ProvisionedCredentialStore ();
        }
        ProvisionedCredentialStore store = GSON.fromJson(json, PROVISIONED_CREDENTIALS_TYPE);
        return store == null ? new ProvisionedCredentialStore() : store;
    }

    private static boolean isBlank(String json) {
        return json == null || json.isBlank();
    }
}
