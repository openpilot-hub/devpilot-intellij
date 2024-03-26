package com.zhongan.devpilot.util;

import com.zhongan.devpilot.integrations.llms.entity.DevPilotSuccessStreamingResponse;

import org.junit.Assert;
import org.junit.Test;

public class JsonUtilsTest {

    @Test
    public void fromJson() {
        String data = "{\"id\":\"chatcmpl-96x1WN8Eo9zJCjrvZxBh1VBDodPsO\",\"object\":\"chat.completion.chunk\",\"created\":1711443882,\"model\":\"gpt-3.5-turbo-0125\",\"system_fingerprint\":\"fp_3bc1b5746c\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"},\"logprobs\":null,\"finish_reason\":null}]}";
        DevPilotSuccessStreamingResponse resp = JsonUtils.fromJson(data, DevPilotSuccessStreamingResponse.class);
        Assert.assertNotNull(resp);
        Assert.assertEquals(resp.getId(),"chatcmpl-96x1WN8Eo9zJCjrvZxBh1VBDodPsO");
    }
}
