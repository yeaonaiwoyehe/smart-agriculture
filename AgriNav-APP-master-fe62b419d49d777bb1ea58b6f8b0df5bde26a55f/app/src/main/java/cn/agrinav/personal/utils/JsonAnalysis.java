package cn.agrinav.personal.utils;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonAnalysis {
    private SharedPreferences sharedPreferences;

    public JsonAnalysis() {

    }

    public JsonAnalysis(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public JsonObject getJsonObject() {
        String jsonStr = sharedPreferences.getString("userInfo", "");
        try{
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
            return jsonObject;
        }catch (Exception e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public String getUserName() {
        JsonObject jsonObject = getJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        JsonElement usernameElement = userObject.get("userName");
        if (usernameElement != null && usernameElement.isJsonPrimitive()) {
            return usernameElement.getAsString();
        }
        return "";
    }

    public String getNickName() {
        JsonObject jsonObject = getJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        JsonElement nicknameElement = userObject.get("nickName");
        if (nicknameElement != null && nicknameElement.isJsonPrimitive()) {
            return nicknameElement.getAsString();
        }
        return "";
    }

    public String getPhoneNumber() {
        JsonObject jsonObject = getJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        JsonElement phoneNumberElement = userObject.get("phonenumber");
        if (phoneNumberElement != null && phoneNumberElement.isJsonPrimitive()) {
            return phoneNumberElement.getAsString();
        }
        return "";
    }

    public String getDepartmentName() {
        JsonObject jsonObject = getJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        JsonElement deptElement = userObject.get("dept");
        if(deptElement == null || deptElement.isJsonNull()) {
            return "";
        }
        JsonObject deptObject = deptElement.getAsJsonObject();
        JsonElement deptNameElement = deptObject.get("deptName");
        if (deptNameElement != null && deptNameElement.isJsonPrimitive()) {
            return deptNameElement.getAsString();
        }
        return "";
    }

    public String getAvatar() {
        JsonObject jsonObject = getJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        JsonElement avatarElement = userObject.get("avatar");
        if (avatarElement != null && avatarElement.isJsonPrimitive()) {
            return avatarElement.getAsString();
        }
        return "";
    }
}
