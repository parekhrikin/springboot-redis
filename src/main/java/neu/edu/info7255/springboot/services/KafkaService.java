package neu.edu.info7255.springboot.services;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {
    private final Logger LOG = LoggerFactory.getLogger(KafkaService.class);
    @Autowired
    private KafkaTemplate<String, JsonNode> kafkaTemplate;
    String kafkaTopic = "healthplan";
    public void send(JsonNode plan) {
        LOG.info("Sending User Json Serializer : {}", plan);
        kafkaTemplate.send(kafkaTopic, plan);
    }
    public void sendList(List<JsonNode> planList) {
        LOG.info("Sending UserList Json Serializer : {}", planList);
        for (JsonNode plan : planList) {
            kafkaTemplate.send(kafkaTopic, plan);
        }
    }
}
