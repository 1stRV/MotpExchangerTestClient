package ru.x5.motpsender.testclient.controller;

import com.fasterxml.jackson.databind.ser.std.UUIDSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.x5.motpsender.dao.dto.AggregatedCisResponse;
import ru.x5.motpsender.dao.dto.GetProductsListResponse;
import ru.x5.motpsender.integration.dto.KafkaAggregatedCisRequest;
import ru.x5.motpsender.integration.dto.KafkaSessionInfo;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Controller
public class ApplicationController {

    @Autowired
    ReplyingKafkaTemplate replyingKafkaTemplate;

    @Autowired
    MotpToken appMotpToken;

    @PostMapping("/loadToken")
    public String loadToken(@ModelAttribute MotpToken motpToken, Model model) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sendAndReceive(motpToken,UUID.randomUUID().toString(), MotpToken.class,  model, countDownLatch,
                "tokenIn", "tokenOut", "tokenError",
                "motpToken", "tokenSuccess");
        if (!countDownLatch.await(10, TimeUnit.SECONDS)) model.addAttribute("tokenError", true);
        return "index";
    }

    @PostMapping("/getAggregated")
    public String getAggregated(@ModelAttribute KafkaAggregatedCisRequest kafkaAggregatedCisRequest, Model model) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setRequestInn(kafkaAggregatedCisRequest, model, "aggregateError");
        sendAndReceive(kafkaAggregatedCisRequest, UUID.randomUUID().toString(), AggregatedCisResponse.class, model, countDownLatch,
                "aggregatedCisIn", "aggregatedCisOut", "aggregateError",
                "aggregatedCisResponse", "aggregateSuccess");
        if (!countDownLatch.await(10, TimeUnit.SECONDS)) model.addAttribute("aggregateError", true);
        return "index";
    }

    @PostMapping("/getProducts")
    public String getProducts(@ModelAttribute KafkaSessionInfo kafkaSessionInfo, Model model) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setRequestInn(kafkaSessionInfo, model, "productsError");
        sendAndReceive(kafkaSessionInfo, UUID.randomUUID().toString(), GetProductsListResponse.class, model, countDownLatch, "productsIn",
                "productsOut", "productsError", "getProductsListResponse",
                "productsSuccess");
        if (!countDownLatch.await(10, TimeUnit.SECONDS)) model.addAttribute("productsError", true);
        return "index";
    }

    private <T extends KafkaSessionInfo>void setRequestInn(@ModelAttribute T kafkaRequest, Model model, String modelError) {
        model.addAttribute("appMotpToken", appMotpToken);
        if ( appMotpToken != null && !"".equals(appMotpToken.getInn()) && appMotpToken.getInn() != null) {
            kafkaRequest.setUserInn(appMotpToken.getInn()); }
        else {
            model.addAttribute(modelError, true);
            appMotpToken.setInn("Токен не загружен!");
        }
    }

    private <T, S> void sendAndReceive(@ModelAttribute T messageObject, String key, Class<S> type, Model model, CountDownLatch countDownLatch, String topicIn, String topicOut, String modelError, String modelResponseName, String modelSuccess) {
        ProducerRecord<String, T> record = new ProducerRecord<>(topicIn, key, messageObject);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, topicOut.getBytes()));

        RequestReplyFuture<String, T, S> sendAndReceive = replyingKafkaTemplate.sendAndReceive(record);
        sendAndReceive.addCallback(new ListenableFutureCallback<ConsumerRecord<String, S>>() {
            @Override
            public void onFailure(Throwable throwable) {
                model.addAttribute(modelError, true);
                countDownLatch.countDown();
            }

            @Override
            public void onSuccess(ConsumerRecord<String, S> consumerRecord) {
                // get consumer record value
                S kafkaResponse = consumerRecord.value();
                if ("MotpToken".equals(type.getSimpleName())) {
                    appMotpToken = (MotpToken) kafkaResponse;
                    model.addAttribute("appMotpToken", appMotpToken);
                }
                model.addAttribute(modelResponseName, kafkaResponse);
                model.addAttribute(modelSuccess, true);

                countDownLatch.countDown();
            }
        });
    }

    @RequestMapping("/")
    public String welcome(Model model) {
        model.addAttribute("appMotpToken", appMotpToken);
        return "index";
    }

}
