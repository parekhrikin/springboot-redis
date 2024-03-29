package neu.edu.info7255.springboot.repository;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import neu.edu.info7255.springboot.entity.Plan;
import org.apache.http.HttpHost;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Repository
public class PlanDao {

    private static final Object HASH_KEY = "Plan";

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate template;

//    @Autowired
//    @Qualifier("elasticsearchTemplate")
//    private ElasticsearchOperations esOperations;

    public JsonNode save(JsonNode payload) throws IOException {

        //template.get

        if (payload.get("$schema") != null) {
            template.opsForHash().put("Schema", payload.get("type"), payload);
        } else {
            template.opsForHash().put(HASH_KEY, payload.get("objectId").toString(), payload);
//            template.convertAndSend("http://localhost:9393/publish", payload);




//            RestTemplate restTemplate = new RestTemplate();
//            restTemplate.postForObject("http://localhost:9393/publish", payload, String.class);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject("http://localhost:8080/producer", payload, String.class);
        }


        return payload;
    }

    public List<JsonNode> findAll() {
        return template.opsForHash().values(HASH_KEY);
    }

    public JsonNode findPlanById(String id) {
        List<JsonNode> plans = template.opsForHash().values(HASH_KEY);
        for (JsonNode plan : plans) {
            if (plan.get("objectId").asText().equals(id)) {
                return plan;
            }
        }
        return null;
    }

    public String deletePlan(String id) {


        template.opsForHash().delete(HASH_KEY, id);
        return "plan removed!";
    }

    public String getSchema() {
        return template.opsForHash().values("Schema").toString();
    }

    public void update(JsonNode planPatched, String id) {
        template.opsForHash().put(HASH_KEY, id, planPatched);
    }
}
