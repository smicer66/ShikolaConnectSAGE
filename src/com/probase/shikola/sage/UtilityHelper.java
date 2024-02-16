package com.probase.shikola.sage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.json.JSONObject;

public class UtilityHelper {
	
	private static String USER_AGENT = "Mozilla/5.0";
	
	
	public static String sendGet(String baseUrl, String parameters, JSONObject jsObj) throws Exception {

		String url = baseUrl + (parameters!=null ? ("?" + parameters) : "");
		////System.out.println("url ==" + url);

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		if(jsObj!=null && jsObj.length()>0)
		{
			Iterator<String> iter = jsObj.keys();
			while(iter.hasNext())
			{
				String key = iter.next();
				con.setRequestProperty(key, jsObj.getString(key));
			}
		}

		int responseCode = con.getResponseCode();
		////System.out.println("\nSending 'GET' request to URL : " + url);
		////System.out.println("Response Code : " + responseCode);
		if(responseCode==500)
		{
			String outputErrorLine;
			BufferedReader wrError = new BufferedReader(
			        new InputStreamReader(con.getErrorStream()));
			StringBuffer responseErr = new StringBuffer();

			while ((outputErrorLine = wrError.readLine()) != null) {
				responseErr.append(outputErrorLine);
			}
			wrError.close();
			System.out.println(responseErr.toString());
		}

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		////System.out.println(response.toString());
		
		if(response.toString().length()==0)
		{
			return null;
		}
		
		return response.toString();

	}
	

	public static String sendPost(String baseUrl, String parameters, JSONObject jsObj) throws Exception {

		String url = "https://selfsolve.apple.com/wcResults.do";
		url = baseUrl;
		//System.out.println("112");
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		//System.out.println("113");
		//add reuqest header
		con.setRequestMethod("POST");
		//System.out.println("114");
		con.setRequestProperty("User-Agent", USER_AGENT);
		//System.out.println("115");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setDoOutput(true);
		//System.out.println("116");
		if(jsObj!=null && jsObj.length()>0)
		{
			Iterator<String> iter = jsObj.keys();
			while(iter.hasNext())
			{
				String key = iter.next();
				con.setRequestProperty(key, jsObj.getString(key));
			}
		}/**/
		//System.out.println("117");

		//String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
		String urlParameters = parameters;
		//System.out.println("118");

		// Send post request
		con.setDoOutput(true);
		//System.out.println("119");
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = urlParameters.getBytes("utf-8");
		    os.write(input, 0, input.length);			
		}
		
		//System.out.println("113");

		int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + urlParameters);
		//System.out.println("Response Code : " + responseCode);
		
		
		
		if(responseCode==500)
		{
			String outputErrorLine;
			BufferedReader wrError = new BufferedReader(
			        new InputStreamReader(con.getErrorStream()));
			StringBuffer responseErr = new StringBuffer();

			while ((outputErrorLine = wrError.readLine()) != null) {
				responseErr.append(outputErrorLine);
			}
			wrError.close();
			System.out.println(responseErr.toString());
		}

	    StringBuilder response = new StringBuilder();
	    BufferedReader br = new BufferedReader(
	  		  new InputStreamReader(
	  				  con.getInputStream(), "utf-8"));
	  		  
	    String responseLine = null;
	    while ((responseLine = br.readLine()) != null) {
	        response.append(responseLine.trim());
	    }
	    //System.out.println(response.toString());

		//print result
		if(response.toString().length()==0)
		{
			return null;
		}
		//System.out.println(response.toString());
		return response.toString();

	}
}
