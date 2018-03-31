package ner.annotation.extraction;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;

/**
 * An extractor for DBPedia entities.
 * Uses the DBPedia Spotlight web service - we could potentially host this service on one of our machines.
 * NOTE: VERY SLOW
 * @author alexaulabaugh
 */

public class DBPediaExtractor extends EntityExtractor
{
	
	private HttpClient client;
	private JsonParser parser;

	public DBPediaExtractor(EntityCatalog cat)
	{
		super(cat);
		client = HttpClientBuilder.create().build();
		parser = new JsonParser();
	}

	@Override
	public void extractEntities(String text)
	{
		String JSONResponse = "";
		try
		{
			JSONResponse = sendPost(text);
			//System.out.println(JSONResponse);
			JsonObject jobject = parser.parse(JSONResponse).getAsJsonObject();
			if(!jobject.has("Resources"))
				return;
			JsonArray a = jobject.get("Resources").getAsJsonArray();
 			for(int i = 0; i < a.size  (); i++)
			{
				EntityAnnotation newAnnotation = new EntityAnnotation();
				JsonObject annotationJson = a.get(i).getAsJsonObject();
				String[] entityTypeList = annotationJson.get("@types").toString().split(",");
				if(!entityTypeList[0].contains(":"))
					continue;
				newAnnotation.setContent(annotationJson.get("@surfaceForm").toString());
				newAnnotation.addType(catalog.getEntityType(entityTypeList[entityTypeList.length-1].split(":")[1].replaceAll("\"", "")));
				newAnnotation.setPosition(Integer.parseInt(annotationJson.get("@offset").toString().replaceAll("\"", "")));
				newAnnotation.setSource("DBPEDIA");
				catalog.addAnnotation(newAnnotation);
			}
		}
		catch (Exception e)
		{
			System.out.println(JSONResponse);
			e.printStackTrace();
		}
	}
	
	// CITATION: https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
	private String sendPost(String text) throws Exception
	{
		String url = "http://model.dbpedia-spotlight.org/en/annotate";
		HttpPost post = new HttpPost(url);
		post.setHeader("Accept", "application/json");

		// add header
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("text", text));//URLEncoder.encode(text, "UTF-8")));
		urlParameters.add(new BasicNameValuePair("data", "confidence=0.35"));

		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		HttpResponse response = client.execute(post);
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + post.getEntity());
		//System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
		if(response.getStatusLine().getStatusCode() != 200)
		{
			return null;
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null)
		{
			result.append(line);
		}

		return result.toString();

	}

}
