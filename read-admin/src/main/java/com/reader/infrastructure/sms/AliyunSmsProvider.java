package com.reader.infrastructure.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AliyunSmsProvider implements SmsProvider {
    private final Client client;
    private final AliyunSmsProperties properties;
    private final Gson gson;

    public AliyunSmsProvider(AliyunSmsProperties properties) throws Exception {
        this.properties = properties;
        this.gson = new Gson();
        
        Config config = new Config()
            .setAccessKeyId(properties.getAccessKeyId())
            .setAccessKeySecret(properties.getAccessKeySecret())
            .setEndpoint(properties.getEndpoint());
            
        this.client = new Client(config);
    }

    @Override
    public Mono<Void> sendSms(String phoneNumber, String code) {
        return Mono.fromCallable(() -> {
            Map<String, String> templateParam = new HashMap<>();
            templateParam.put("code", code);
            
            SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phoneNumber)
                .setSignName(properties.getSignName())
                .setTemplateCode(properties.getTemplateCode())
                .setTemplateParam(gson.toJson(templateParam));

            try {
                var response = client.sendSms(request);
                if (!"OK".equals(response.getBody().getCode())) {
                    log.error("Failed to send SMS: {}", response.getBody().getMessage());
                    throw new RuntimeException("Failed to send SMS");
                }
                log.info("Successfully sent SMS to {}", phoneNumber);
                return response;
            } catch (Exception e) {
                log.error("Error sending SMS", e);
                throw new RuntimeException("Error sending SMS", e);
            }
        }).then();
    }
} 