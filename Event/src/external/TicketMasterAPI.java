package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; //no restriction return all events
	private static final String API_KEY = Config.TicketMasterAPI_KEY;
	
	public JSONArray search(double lat, double lon, String keyword) {
		//if client submit keyword then use it 
		//else set keyword to DEFAULT_KEYWORD
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		
		try {
			//why encode?
			//convert client's input keyword to the format that http protocal can understand
			//eg. space <- encode -> %20
			//we should use the chatset that TicketMaster can recognize
			keyword = URLEncoder.encode(keyword, "UTF-8"); //"Rick Sun" => "Rick%20Sun"
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			//String java.net.URLEncoder.encode(String s, String enc) throws UnsupportedEncodingException
			//so we need to either throw it or catch it. Here we catch it 
			e.printStackTrace();
		}
		
		// query should looks like: "apikey=qqPuP6n3ivMUoT9fPgLepkRMreBcbrjV&latlong=37,-120&keyword=event&radius=50"
		String query = String.format("apikey=%s&latlong=%s,%s&keyword=%s&radius=%s", API_KEY, lat, lon, keyword, 50);
		String url = URL + "?" + query;
		
		try {
			//need a cast 
			//create a URLConnection instance that represents a connection to the remote object referred to by the URL
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			//Tell what HTTP method to use
			connection.setRequestMethod("GET");
			//Get status code from HTTP response message
			int responseCode = connection.getResponseCode();
			
			System.out.println("Sending request to url: " + url);
			System.out.println("Response code: " + responseCode);
			
			if (responseCode != 200) {
				System.out.println("error status code is " + responseCode);
				return new JSONArray();
			}
			
			//if response code is 200, then we can go ahead read the data
			//create a BufferedReader to help read text from a character-input stream. 
			//Provide for the efficient reading of characters, arrays, and lines
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();
			
			//append response data to response StringBuilder instance line by line
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			//close the BufferReader after reading the input stream/response data
			reader.close();
			
			//Extract events array only
			JSONObject obj = new JSONObject(response.toString());
			if (!obj.isNull("_embedded")) {
				JSONObject embedded = obj.getJSONObject("_embedded");
				return embedded.getJSONArray("events");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new JSONArray();
	}
	
	private void queryAPI(double lat, double lon) {
		JSONArray events = search(lat, lon, null);
		
		try {
			for (int i = 0; i < events.length(); i++) {
				JSONObject event = events.getJSONObject(i);
				System.out.println(event.toString(2));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}
}


