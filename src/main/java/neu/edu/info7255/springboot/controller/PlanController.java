package neu.edu.info7255.springboot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import neu.edu.info7255.springboot.entity.Plan;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import neu.edu.info7255.springboot.repository.PlanDao;
import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.everit.json.schema.loader.SchemaLoader;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

        validatePayload(plan);

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

    @PatchMapping(path = "/plan/{id}")
    public ResponseEntity updatePlan(@PathVariable("id") String id,
                                     @RequestBody String patch, @RequestHeader HttpHeaders headers) throws IOException {
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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

        JsonNode updatedPlan = new ObjectMapper().readTree(patch);
        String objectId = updatedPlan.get("objectId").asText();
        JsonNode orignalPlan = dao.findPlanById(objectId);
        applyUpdatedPlan(updatedPlan, orignalPlan);
        dao.save(orignalPlan);
        return ResponseEntity.ok(orignalPlan);


//            List<JsonNode> plansPatched = new ArrayList<>();
//
//            if(objectType.equals("plan")){
//                JsonNode plan = dao.findPlanById(id);
//                plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
//                dao.update(plansPatched.get(0), id);
//            } else if(objectType.equals("planservice")){
//                int i = 0;
//                List<JsonNode> plans = dao.findAll();
//                for(JsonNode plan: plans){
//                    plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
//                    dao.update(plansPatched.get(i), plansPatched.get(i).get("objectId").toString());
//                    i++;
//                }
//            } else {
//                int i = 0;
//                List<JsonNode> plans = dao.findAll();
//                for(JsonNode plan: plans){
//                    plansPatched.add(applyPatchToPlan(patch, plan, objectType, id));
//                    dao.update(plansPatched.get(i), plansPatched.get(i).get("objectId").toString());
//                    i++;
//                }
//            }


//            return ResponseEntity.ok(plansPatched);
//        }


    }

    private void applyUpdatedPlan(JsonNode updatedPlan, JsonNode originalPlan) {
        if (updatedPlan.equals(originalPlan)){
            System.out.println("updated and orignal plan are same");
            return;
        }



        if (!updatedPlan.get("planCostShares").isNull()) {
            Iterator<Map.Entry<String, JsonNode>> fields = updatedPlan.get("planCostShares").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                addToOrignalPlanIfNeeded(field, originalPlan);
            }
        }
//        else if(updatedPlan.get("linkedPlanServices").){
//            Iterator<Map.Entry<String, JsonNode>> fields = updatedPlan.get("linkedPlanServices").fields();
//            while(fields.hasNext()) {
//                Map.Entry<String, JsonNode> field = fields.next();
//                addToOrignalPlanIfNeededLinkedPlanServices(field, orignalPlan);
//            }
//        }

//        List<JsonNode> linkedPlanServices = new ArrayList<>();


        //LinkedPlanServices
        for(JsonNode obj: updatedPlan.get("linkedPlanServices")){
            addToOrignalPlanIfNeededLinkedPlanServices(obj, originalPlan);
        }

        Iterator<Map.Entry<String, JsonNode>> fields = updatedPlan.fields();

        while(fields.hasNext()){
            Map.Entry<String, JsonNode> field = fields.next();
            switch(field.getKey()){
                case "_org" :
                case "objectId" :
                case "objectType" :
                case "planType" :
                case "creationDate" :
                    ((ObjectNode) originalPlan).put(field.getKey(), field.getValue());
                    break;
            }
        }



    }

    private void addToOrignalPlanIfNeededLinkedPlanServices(JsonNode obj, JsonNode originalPlan) {

        boolean flag = false;

        for(JsonNode org: originalPlan.get("linkedPlanServices")){
            if(obj.get("objectId").asText().contains(org.get("objectId").asText())){
                addToOriginalPlanIfNeededLinkedService(obj.get("linkedService"), org.get("linkedService"));
                addToOriginalPlanIfNeededPlanServiceCostShares(obj.get("planserviceCostShares"), org.get("planserviceCostShares"));

                Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();

                while(fields.hasNext()){
                    Map.Entry<String, JsonNode> field = fields.next();
                    switch(field.getKey()){
                        case "_org" :
                        case "objectId" :
                        case "objectType" :
                            ((ObjectNode) org).put(field.getKey(), field.getValue());
                            break;
                    }
                }

                flag = true;
            }
        }

        if(flag == false){
            ((ArrayNode) originalPlan.get("linkedPlanServices")).add(obj);
        }

    }

    private void addToOriginalPlanIfNeededPlanServiceCostShares(JsonNode obj, JsonNode org) {

        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();

        while(fields.hasNext()){
            Map.Entry<String, JsonNode> field = fields.next();
            switch (field.getKey()) {
                case "deductible":
                case "_org" :
                case "copay" :
                case "objectId" :
                case "objectType" :
                    ((ObjectNode) org).put(field.getKey(), field.getValue());
                    break;
            }
        }

    }

    private void addToOriginalPlanIfNeededLinkedService(JsonNode obj, JsonNode org) {

        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();

        while(fields.hasNext()){
            Map.Entry<String, JsonNode> field = fields.next();
            switch (field.getKey()) {
                case "_org" :
                case "objectId" :
                case "objectType" :
                case "name" :
                    ((ObjectNode) org).put(field.getKey(), field.getValue());
                    break;


            }
        }



    }

    private void addToOrignalPlanIfNeeded(Map.Entry<String, JsonNode> field, JsonNode originalPlan) {

//        List<Map.Entry<String, JsonNode>> fieldsToAdd = new

        switch (field.getKey()) {
            case "deductible" :
            case "_org" :
            case "copay" :
            case "objectId" :
            case "objectType" :
                ((ObjectNode) originalPlan.get("planCostShares")).put(field.getKey(), field.getValue());
                break;


        }
    }


    public static JsonNode applyPatchToPlan(JsonNode patch, JsonNode plan) {
        JsonNode patched = traverse(patch, plan);
        return patched;
    }

    public static JsonNode traverse(JsonNode patch, JsonNode root) {

        if(root.isObject()){
            Iterator<String> fieldNames = root.fieldNames();

            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = root.get(fieldName);
                if(fieldValue.isObject() && fieldValue.get("objectType").asText().equals(patch.get(fieldName).get("objectType"))){
                    Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
                    while(fields.hasNext()){
                        Map.Entry<String, JsonNode> field = fields.next();
                        if(fieldValue.get(field.getKey()).isNull()){
                            ((ObjectNode) root.get(fieldName)).put(field.getKey(), field.getValue());
                        } else {
                            if(fieldValue.get(field.getKey()).equals(field.getValue())){
                                continue;
                            } else {
                                ((ObjectNode) root.get(fieldName)).put(field.getKey(), field.getValue());
                            }
                        }
                    }
                }
//                else if(fieldValue.isArray()){
////                    Iterator<Map.Entry<String, JsonNode>> objs = patch.fields();
//                    for(JsonNode obj: fieldValue){
////                        Map.Entry<String, JsonNode> obj = objs.next();
//                        ((ArrayNode) root.get(fieldName)).add(obj);
//
//                    }
//                }
                traverse(patch, fieldValue);
            }
        } else if(root.isArray()){
//            ArrayNode arrayNode = (ArrayNode) root;
//            for(int i = 0; i < arrayNode.size(); i++) {
//                JsonNode arrayElement = arrayNode.get(i);
//                traverse(patch, arrayElement);
//            }
            ArrayNode patchArray = (ArrayNode) patch.get("linkedPlanServices");
            boolean flag = false;
            for(JsonNode o: patchArray){
//                        Map.Entry<String, JsonNode> obj = objs.next();
                for(JsonNode obj: root){
                    if(o.get("objectId").equals(obj.get("objectId"))){
                        if(obj.get("linkedService").get("objectId").equals(o.get("linkedService").get("objectId"))) {
                            traverse(o.get("linkedService"), obj.get("linkedService"));
                        } else if(obj.get("planserviceCostShares").get("objectId").equals(o.get("planCostShares").get("objectId"))) {
                            traverse(o.get("planCostShares"), obj.get("planCostShares"));
                        }
                        flag = true;
                    }

                }
                if(flag == true){
                    ((ArrayNode) root).add(o);
                    flag = false;
                }


            }
        } else {
            // JsonNode root represents a single value field - do something with it.

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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

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

        if (!authorize(headers)) {
            return (ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

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


    @GetMapping("/token")
    public ResponseEntity<String> generateToken(){

        JSONObject jsonToken = new JSONObject();
        jsonToken.put("issuer", "admin");
        TimeZone timeZone = TimeZone.getTimeZone("UTC");

        DateFormat ds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:'Z'");
        ds.setTimeZone(timeZone);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, 60);
        Date date = calendar.getTime();

        jsonToken.put("expiry", ds.format(date));

        return new ResponseEntity<String>(jsonToken.toString(), HttpStatus.ACCEPTED);
    }

    private boolean authorize(HttpHeaders headers) {
        String token = headers.getFirst("Authorization");

        System.out.println(headers.getFirst("Authorization"));
        System.out.println(token.substring(7));

        JSONObject jToken = new JSONObject(token.substring(7));

        String dateAsString = jToken.get("expiry").toString();

        Date currentDate = Calendar.getInstance().getTime();

        TimeZone timeZone = TimeZone.getTimeZone("UTC");

        DateFormat ds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:'Z'");
        ds.setTimeZone(timeZone);

        Date ttlDate = null;

        try {
            ttlDate = ds.parse(dateAsString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            currentDate = ds.parse(ds.format(currentDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(currentDate.after(ttlDate)){
            System.out.println("Expired TOKEN!!!!!!!!");
            return false;
        } else {
            return true;
        }

    }


}
