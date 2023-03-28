package neu.edu.info7255.springboot.controller;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import neu.edu.info7255.springboot.services.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ProducerController {
    @Autowired
    KafkaService kafkaProducer;

    @PostMapping("/producer")
    public String sendMessage(@RequestBody JsonNode plan) {
        kafkaProducer.send(plan);
        return "Message sent successfully to the Kafka topic shubham";
    }

    @PostMapping("/producerlist")
    public String sendMessage(@RequestBody List<JsonNode> plan) {
        kafkaProducer.sendList(plan);
        return "Message sent successfully to the Kafka topic shubham";
    }
}
