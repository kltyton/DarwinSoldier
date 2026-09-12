package com.kltyton.darwin_soldier.client.ui.foundation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InteractiveAuiScreenTest {
    @Test
    void numericUiEventsAcceptIntegerValuedDecimals() {
        JsonObject event = JsonParser.parseString("{\"direction\":1.0,\"value\":\"10.0\"}").getAsJsonObject();
        assertEquals(1, InteractiveAuiScreen.integerData(event, "direction"));
        assertEquals(10, InteractiveAuiScreen.integerData(event, "value"));
    }

    @Test
    void fractionalAndOverflowingIntegerEventsAreRejected() {
        JsonObject event = JsonParser.parseString("{\"fraction\":1.5,\"overflow\":2147483648}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> InteractiveAuiScreen.integerData(event, "fraction"));
        assertThrows(IllegalArgumentException.class, () -> InteractiveAuiScreen.integerData(event, "overflow"));
    }

    @Test
    void MissingAndNonScalarIntegerFieldsAreRejected() {
        JsonObject event = JsonParser.parseString("{\"object\":{}}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> InteractiveAuiScreen.integerData(event, "missing"));
        assertThrows(IllegalArgumentException.class, () -> InteractiveAuiScreen.integerData(event, "object"));
    }
}
