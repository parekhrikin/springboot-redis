package neu.edu.info7255.springboot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import neu.edu.info7255.springboot.entity.Plan;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static java.util.Currency.getInstance;


@RestController
@RequestMapping("/plan")
public class PlanController {

    @Autowired
    private PlanDao dao;
    ObjectMapper mapper = new ObjectMapper();

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

    @PatchMapping(value = "/{id}/{fNames}")
    public JsonNode updatePlan(@PathVariable("id") String id, @PathVariable("fNames") String fieldNames, @RequestBody JsonNode patch) throws IOException {
        String[] fields = fieldNames.split("\\+");
        LinkedList<String> ll = new LinkedList<String>();
        for(int i = 0; i < fields.length; i++){
            ll.add(fields[i]);
        }
        JsonNode plan = dao.findPlanById(id);
        JsonNode planPatched = applyPatchToPlan(patch, plan, ll);
        dao.update(planPatched, id);
        return planPatched;
    }

    public static JsonNode applyPatchToPlan(JsonNode patch, JsonNode plan, LinkedList<String> ll) {
        JsonNode patched = traverse(patch, plan, ll);
        return patched;
    }

    public static JsonNode traverse(JsonNode patch, JsonNode root, LinkedList<String> ll) {

        if(root.isObject()){
            Iterator<String> fieldNames = root.fieldNames();

            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if(fieldName.equals(ll.getFirst())){

                    ll.removeFirst();
                }
                JsonNode fieldValue = root.get(fieldName);
                traverse(patch, fieldValue, ll);
            }
        } else if(root.isArray()){
            ArrayNode arrayNode = (ArrayNode) root;
            for(int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                traverse(patch, arrayElement, ll);
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
