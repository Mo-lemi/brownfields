package za.co.wethinkcode.robots.client;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestResponseHandler {

    @Test
    void decodesValidStateResponseGracefully() {
        ResponseHandler handler = new ResponseHandler();
        JSONObject validStateJson = new JSONObject()
                .put("result", "OK")
                .put("data", new JSONObject())
                .put("state", new JSONObject()
                        .put("position", new org.json.JSONArray().put(0).put(0))
                        .put("direction", "NORTH")
                        .put("shields", 5)
                        .put("shots", 5)
                        .put("status", "NORMAL"));

        assertDoesNotThrow(() -> handler.decodeResponse("state", validStateJson));
    }

    @Test
    void handlesErrorResponseWithoutCrashing() {
        ResponseHandler handler = new ResponseHandler();
        JSONObject errorJson = new JSONObject()
                .put("result", "ERROR")
                .put("data", new JSONObject().put("message", "Robot does not exist"));

        assertDoesNotThrow(() -> handler.decodeResponse("launch", errorJson));
    }
}