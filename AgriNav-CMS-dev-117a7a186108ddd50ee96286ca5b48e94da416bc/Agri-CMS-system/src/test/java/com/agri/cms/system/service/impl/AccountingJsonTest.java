package com.agri.cms.system.service.impl;

import com.agri.cms.system.service.accounting.AccountingModels.PreviewRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccountingJsonTest {
    private final ObjectMapper json=new ObjectMapper();
    private String body(String width) {return "{\"plotId\":\"9007199254740993\",\"widthCm\":"+width+",\"segments\":[[{\"longitude\":114,\"latitude\":38}]]}";}
    @Test public void centimetresAndSnowflakeIdBindWithoutJavascriptPrecisionLoss() throws Exception {
        var request=json.readValue(body("300"),PreviewRequest.class);
        assertEquals(Long.valueOf(9007199254740993L),request.plotId());assertEquals(Integer.valueOf(300),request.widthCm());
    }
    @Test public void fractionalStringAndOverflowWidthsCannotBeSilentlyCoerced() {
        for(String width:new String[]{"300.9","3e2","\"300\"","true","2147483648"})
            assertThrows(Exception.class,()->json.readValue(body(width),PreviewRequest.class));
    }
    @Test public void omittedCoordinatesAndWidthRemainNullForServiceValidation() throws Exception {
        var request=json.readValue("{\"plotId\":1,\"segments\":[[{\"latitude\":38}]]}",PreviewRequest.class);
        assertNull(request.widthCm());assertNull(request.segments().get(0).get(0).longitude());
    }
}
