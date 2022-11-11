package neu.edu.info7255.springboot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import neu.edu.info7255.springboot.entity.Plan;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import neu.edu.info7255.springboot.repository.PlanDao;
import org.apache.coyote.Response;
import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.everit.json.schema.loader.SchemaLoader;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static java.util.Currency.getInstance;


@RestController
@RequestMapping
public class PlanController {

    @Autowired
    private PlanDao dao;
    static ObjectMapper mapper = new ObjectMapper();
    static String jws;

    @PostMapping("/schema")
    public ResponseEntity saveSchema(@RequestBody JsonNode schema, @RequestHeader HttpHeaders headers){

//        int pos = jws.indexOf(" ");
//
//        if(!headers.get(HttpHeaders.AUTHORIZATION).equals(jws.substring(pos + 1))){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
//            return ResponseEntity.ok(dao.save(schema));
//        }

        return ResponseEntity.ok(dao.save(schema));

    }

    @PostMapping("/plan")
    public ResponseEntity save(@RequestBody JsonNode plan, @RequestHeader HttpHeaders headers) throws Exception {

//        int pos = jws.indexOf(" ");

//        if(!headers.get(HttpHeaders.AUTHORIZATION).equals(jws.substring(pos + 1))){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
//            validatePayload(plan);
//            return ResponseEntity.ok(dao.save(plan));
//        }

        return ResponseEntity.ok(dao.save(plan));

    }

    private void validatePayload(JsonNode plan) throws Exception {


        JsonNode s = mapper.readTree(dao.getSchema());


        try{
            Schema schema = SchemaLoader.load(new JSONObject(s));
            schema.validate(new JSONObject(plan)); // throws a ValidationException if this object is invalid
        } catch(Exception e){
            e.printStackTrace();
            System.out.println(e);

        }


    }

//    public static String readFileAsString(String file)throws Exception
//    {
//        return new String(Files.readAllBytes(Paths.get(file)));
//    }

    @PatchMapping(value = "/plan/{id}/{objType}")
    public ResponseEntity updatePlan(@PathVariable("id") String id, @PathVariable("objType") String objectType,
                                     @RequestBody JsonNode patch, @RequestHeader HttpHeaders headers) throws IOException {
//        String[] fields = objectType.split("\\+");
//        LinkedList<String> ll = new LinkedList<String>();
//        for(int i = 0; i < fields.length; i++){
//            ll.add(fields[i]);
//        }

//        String s = headers.get(HttpHeaders.AUTHORIZATION).get(0);
//        int pos = s.indexOf(" ");


//        if(!s.substring(pos+1).equals(jws)){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
            List<JsonNode> plansPatched = new ArrayList<>();

            if(objectType.equals("plan")){
                JsonNode plan = dao.findPlanById(id);
                plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
                dao.update(plansPatched.get(0), id);
            } else if(objectType.equals("planservice")){
                int i = 0;
                List<JsonNode> plans = dao.findAll();
                for(JsonNode plan: plans){
                    plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
                    dao.update(plansPatched.get(i), plansPatched.get(i).get("objectId").toString());
                    i++;
                }
            } else {
                int i = 0;
                List<JsonNode> plans = dao.findAll();
                for(JsonNode plan: plans){
                    plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
                    dao.update(plansPatched.get(i), plansPatched.get(i).get("objectId").toString());
                    i++;
                }
            }


            return ResponseEntity.ok(plansPatched);
//        }


    }

    public static JsonNode applyPatchToPlan(JsonNode patch, JsonNode plan, String objectType, String id) {
        JsonNode patched = traverse(patch, plan, objectType, id);
        return patched;
    }

    public static JsonNode traverse(JsonNode patch, JsonNode root, String objectType, String id) {

        if(root.isObject() && !root.get("objectType").equals("plan")){
            Iterator<String> fieldNames = root.fieldNames();

            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = root.get(fieldName);
                if(fieldValue.isObject() && fieldValue.get("objectType").asText().equals(objectType) && fieldValue.get("objectId").asText().equals(id)){
                    Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
                    while(fields.hasNext()){
                        Map.Entry<String, JsonNode> field = fields.next();
                        ((ObjectNode) root.get(fieldName)).put(field.getKey(), field.getValue());
                    }
                } else if(fieldValue.isArray() && fieldValue.get(0).get("objectType").asText().equals(objectType)){
//                    Iterator<Map.Entry<String, JsonNode>> objs = patch.fields();
                    for(JsonNode obj: patch){
//                        Map.Entry<String, JsonNode> obj = objs.next();
                        ((ArrayNode) root.get(fieldName)).add(obj);
                    }
                }
                traverse(patch, fieldValue, objectType, id);
            }
        } else if(root.isArray()){
            ArrayNode arrayNode = (ArrayNode) root;
            for(int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                traverse(patch, arrayElement, objectType, id);
            }
        } else {

        }
        return root;
    }

    @GetMapping("/plan")
    public ResponseEntity getAllPlans(@RequestHeader HttpHeaders headers){

//        int pos = jws.indexOf(" ");
//
//        if(!headers.get(HttpHeaders.AUTHORIZATION).equals(jws.substring(pos + 1))){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
//            return ResponseEntity.ok(dao.findAll());
//        }

        return ResponseEntity.ok(dao.findAll());


    }

    @GetMapping("/plan/{id}")
    public ResponseEntity findPlan(@PathVariable String id, @RequestHeader HttpHeaders headers){

//        String s = headers.get(HttpHeaders.AUTHORIZATION).get(0);
//        int pos = s.indexOf(" ");

//        if(!s.substring(pos+1).equals(jws)){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
//            return ResponseEntity.ok(dao.findPlanById(id));
//        }

        return ResponseEntity.ok(dao.findPlanById(id));
    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity remove(@PathVariable String id, @RequestHeader HttpHeaders headers) {

//        int pos = jws.indexOf(" ");
//
//        if(!headers.get(HttpHeaders.AUTHORIZATION).equals(jws.substring(pos + 1))){
//            return ResponseEntity.badRequest().body("not authorized");
//        } else {
//            return ResponseEntity.ok(dao.deletePlan(id));
//        }

        return ResponseEntity.ok(dao.deletePlan(id));

    }

//    @GetMapping
//    public String getToken(){
//        jws = Jwts.builder()
//                .setIssuedAt(Date.from(Instant.ofEpochSecond(1466796822L)))
//                .setExpiration(Date.from(Instant.ofEpochSecond(4622470422L)))
//                .signWith(
//                        SignatureAlgorithm.HS256,
//                        TextCodec.BASE64.decode("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=")
//                )
//                .compact();
//
//        return jws;
//    }

}
