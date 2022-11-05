package neu.edu.info7255.springboot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import neu.edu.info7255.springboot.entity.Plan;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import neu.edu.info7255.springboot.repository.PlanDao;
import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.everit.json.schema.loader.SchemaLoader;

import javax.json.JsonMergePatch;
import java.io.IOException;
import java.util.*;

import static java.util.Currency.getInstance;


@RestController
@RequestMapping("/plan")
public class PlanController {

    @Autowired
    private PlanDao dao;
    static ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/schema")
    public JsonNode saveSchema(@RequestBody JsonNode schema){
        return dao.save(schema);
    }

    @PostMapping
    public JsonNode save(@RequestBody JsonNode plan) throws Exception {


        validatePayload(plan);
        return dao.save(plan);
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

    @PatchMapping(value = "/{id}/{objType}")
    public List<JsonNode> updatePlan(@PathVariable("id") String id, @PathVariable("objType") String objectType, @RequestBody JsonNode patch) throws IOException {
//        String[] fields = objectType.split("\\+");
//        LinkedList<String> ll = new LinkedList<String>();
//        for(int i = 0; i < fields.length; i++){
//            ll.add(fields[i]);
//        }

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



        return plansPatched;
    }

    public static JsonNode applyPatchToPlan(JsonNode patch, JsonNode plan, String objectType, String id) {
        JsonNode patched = traverse(patch, plan, objectType, id);
        return patched;
    }

    public static JsonNode traverse(JsonNode patch, JsonNode root, String objectType, String id) {

        if(root.isObject()){
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
                } else if(fieldValue.isArray()){
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
            // JsonNode root represents a single value field - do something with it.

        }
        return root;
    }

    @GetMapping
    public List<JsonNode> getAllPlans(){
        return dao.findAll();
    }

    @GetMapping("/{id}")
    public JsonNode findPlan(@PathVariable String id){
        return dao.findPlanById(id);
    }

    @DeleteMapping("/{id}")
    public String remove(@PathVariable String id) {
        return dao.deletePlan(id);
    }

}
