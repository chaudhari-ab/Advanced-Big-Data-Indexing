package hello;

import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import connection.RedisConnection;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import redis.clients.jedis.Jedis;
import validator.Validator;

import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
public class HelloController{
	
	Jedis jedis = RedisConnection.getConnection();
    
	@RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
    
    @RequestMapping(value="/plan", method=RequestMethod.POST,produces="application/json")
    public String addPlan(@RequestBody JSONObject jsonObject, HttpServletResponse response) throws IOException, ProcessingException
    {
    	
    	
    	String data = jsonObject.toString();
    	
    	Boolean jsonValidity = Validator.isJSONValid(data);
    	if(jsonValidity == true) {
	    	String key = jsonObject.get("objectId").toString();
	    	System.out.println(data);
	    	String jsonFromDB = null;
	    	jedis.connect();
	    	try {
	    	jsonFromDB =  jedis.get(key);
	    	}catch(Exception e) {
	    		
	    	}
	    	String ETag = DigestUtils.sha256Hex(data);
	    	String keyForEtag = "";
	    	if(jsonFromDB==null ) {
    		keyForEtag = key+"etag";
	    		
	    	jedis.set(key,data);
	    	jedis.set(keyForEtag, ETag.substring(0, 36));
	    	
	    	response.setHeader("Etag", ETag.substring(0, 36));
	    	response.setStatus(201);
	    	return data;
	    	}else {
	    		response.setStatus(409);
	    		return "Plan Exists";
	    		
	    	}
    	
    	}
    	else {
    		response.setStatus(400);
			return "JSON Schema not valid!";
    	}
    }
    
    @RequestMapping(value="/plan/{id}", method=RequestMethod.GET,produces="application/json")
    public String getPlan(@PathVariable String id,HttpServletRequest request, HttpServletResponse response)
    {
    	System.out.println("id"+id);
    	String eTagFromHeader=null;
    	try {
    	System.out.println(request.getHeader("If-None-Match"));
    	eTagFromHeader = request.getHeader("If-None-Match");
    	}catch(Exception e) {
    		System.out.println("No Header");
    	}
    	if(eTagFromHeader!=null) {
    		String eTagFromDB = jedis.get(id+"etag");
    		if(eTagFromDB.equals(eTagFromHeader)) {
    			response.setStatus(304);
    			return "";
    		}
    	}
    	jedis.connect();
    	String key = id;
    	String planKey =  jedis.get(key);
    	if (planKey==null) {
    		response.setStatus(404);
    		return "Plan Not Found";
    	}
    	String eTagFromDB = jedis.get(id+"etag");
    	response.setHeader("Etag", eTagFromDB);
    	return planKey;
    }
    
    @RequestMapping(value="/plan/{id}", method=RequestMethod.DELETE)
    public String deletePlan(@PathVariable String id, HttpServletResponse response)
    {
    	
    	
    	jedis.connect();
    	String planKey =  jedis.get(id);
    	if (planKey==null) {
    		response.setStatus(404);
    		return "Plan Not Found to Delete";
    	} 
    	String key = id;
    	jedis.del(key);
    	return key + " deleted successfully!";
    }
    
    @RequestMapping(value="/plan/{id}", method=RequestMethod.PUT)
    public String updatePlan(@RequestBody JSONObject jsonObject, @PathVariable String id, HttpServletResponse response) throws IOException, ProcessingException
    {
    	jedis.connect();
    	String key = id;
    	String data = jsonObject.toString();
    	Boolean jsonValidity = Validator.isJSONValid(data);
    	if(jsonValidity == true) {
    		jedis.set(key, data);
    		return key + " updated successfully!";
    	}
    	else {
    		return "Invalid JSON!";
    	}
    }
    
    private String convertToHex(final byte[] messageDigest) {
        BigInteger bigint = new BigInteger(1, messageDigest);
        String hexText = bigint.toString(16);
        while (hexText.length() < 32) {
           hexText = "0".concat(hexText);
        }
        return hexText;
     }
}
