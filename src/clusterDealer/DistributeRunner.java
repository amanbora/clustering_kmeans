package clusterDealer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.*;


import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import clusterDealer.Allocation.Location;
import clusterDealer.Allocation.dealerPendingOrder;
import clusterDealer.DistributeRunner.RestClient.RequestMethod;




public class DistributeRunner {

	static Allocation alloc = new Allocation();
	
	private static final int PLATE_CAPACITY = 300;
	private static final int NOT_ASSIGNED = 7;
	
	static int agentIndex = 0;

	//container for agent wise order details
	class agentOrder{
		String agent;
		List<dealerPendingOrder> dealers = new LinkedList<>();
		int ordersCount;
		int quadNumber;
	}
	
	//container for bulk orders
	class bulk{
		long dealer_id = 0;
		int bulk_plate_count=0;
		Date first_order_date_time;
    	Time order_ready_time;
    	int priority=0;
    	Date updated_at;
    	
	}
	
	
	
	
	static String getPendencyAgent(long dealer_id, Connection db)throws SQLException{
		
		String pendingAgent = null;
			
		String pendencyQuery = "SELECT distinct agent_code FROM Test_HSRPCircuit.HSRP_allocation_details alloc" + 
				" join HSRP_order_detail det on det.cID = alloc.circuit_id\r\n" + 
				" where alloc.delivery_status = 'P' and det.dealer_ID =" + dealer_id+ 
				" and det.order_status_id!=6";
		
		Statement sql = db.createStatement();
		
		try {
			ResultSet rs = sql.executeQuery(pendencyQuery);
			while(rs.next()) {
				pendingAgent = rs.getString(1);
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return pendingAgent;
				
	}
	
	
	
	
//	
//	static int getPlateCount(long dealer_id, Connection db) throws SQLException {
//
//		int plateCount=0;
//		String plateQuery = "SELECT Sum(plate_count)  FROM HSRP_AllocationDB.HSRP_order_detail det" + 
//				" join HSRP_order_veh_reg_mapping oreg on det.order_ID = oreg.Challan_order_ID " + 
//				" where oreg.dealer_ID = " + dealer_id + " and det.order_status_id !=6 ";
//		
//
//    	Statement sql = db.createStatement();
//    	
//    	
//    	try {
//    		ResultSet countPlate = sql.executeQuery(plateQuery);
//        	while(countPlate.next()) {
//        		 plateCount = countPlate.getInt(1);
//        		
//        		
//        		//just to check
////        		System.out.println(agent);
//        	}
//    	}catch(SQLException e) {
//    		e.printStackTrace();
//    	}
//    	return plateCount;
//	}
	
	
	
	
	public static class RestClient
	{
	    private ArrayList<NameValuePair> params;
	    private ArrayList<NameValuePair> headers;

	    private String url;

	    private int responseCode;
	    private String message;

	    private String response;

	    public String getResponse()
	    {
	        return response;
	    }

	    public String getErrorMessage()
	    {
	        return message;
	    }

	    public int getResponseCode()
	    {
	        return responseCode;
	    }

	    public RestClient(String url) {
	        this.url = url;
	        params = new ArrayList<NameValuePair>();
	        headers = new ArrayList<NameValuePair>();
	    }

	    public void AddParam(String name, String value)
	    {
	        params.add(new BasicNameValuePair(name, value));
	    }

	    public void AddHeader(String name, String value)
	    {
	        headers.add(new BasicNameValuePair(name, value));
	    }

	    public void Execute(RequestMethod method) throws Exception
	    {
	        switch (method)
	        {
	        case GET:
	        {
	            // add parameters
	            String combinedParams = "";
	            if (!params.isEmpty())
	            {
	                combinedParams += "";
	                for (NameValuePair p : params)
	                {
	                    String paramString = p.getName() + "" + URLEncoder.encode(p.getValue(),"UTF-8");
	                    if (combinedParams.length() > 1)
	                    {
	                        combinedParams += "&" + paramString;
	                    }
	                    else
	                    {
	                        combinedParams += paramString;
	                    }
	                }
	            }

	            HttpGet request = new HttpGet(url + combinedParams);

	            // add headers
	            for (NameValuePair h : headers)
	            {
	                request.addHeader(h.getName(), h.getValue());
	            }

	            executeRequest(request, url);
	            break;
	        }
	        case POST:
	        {
	            HttpPost request = new HttpPost(url);

	            // add headers
	            for (NameValuePair h : headers)
	            {
	                request.addHeader(h.getName(), h.getValue());
	            }

	            if (!params.isEmpty())
	            {
	                request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	            }

	            executeRequest(request, url);
	            break;
	        }
	        }
	    }

	    private void executeRequest(HttpUriRequest request, String url) throws Exception
	    {

	        HttpClient client = new DefaultHttpClient();

	        HttpResponse httpResponse;





	            httpResponse = client.execute(request);
	            responseCode = httpResponse.getStatusLine().getStatusCode();
	            message = httpResponse.getStatusLine().getReasonPhrase();

	            HttpEntity entity = httpResponse.getEntity();

	            if (entity != null)
	            {

	                InputStream instream = entity.getContent();
	                response = convertStreamToString(instream);

	                // Closing the input stream will trigger connection release
	                instream.close();
	            }


	    }

	    private static String convertStreamToString(InputStream is)
	    {

	        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	        StringBuilder sb = new StringBuilder();

	        String line = null;
	        try
	        {
	            while ((line = reader.readLine()) != null)
	            {
	                sb.append(line + "\n");
	            }
	        }
	        catch (IOException e)
	        {

	            e.printStackTrace();
	        }
	        finally
	        {
	            try
	            {
	                is.close();
	            }
	            catch (IOException e)
	            {
	                e.printStackTrace();
	            }
	        }
	        return sb.toString();
	    }
	    public InputStream getInputStream(){
	    	
	        
	        HttpClient client = new DefaultHttpClient();

	        HttpResponse httpResponse;

	        try
	        {

	               HttpPost request = new HttpPost(url);

	            httpResponse = client.execute(request);
	            responseCode = httpResponse.getStatusLine().getStatusCode();
	            message = httpResponse.getStatusLine().getReasonPhrase();

	            HttpEntity entity = httpResponse.getEntity();

	            if (entity != null)
	            {

	                InputStream instream = entity.getContent();
	                return instream;
	             /*   response = convertStreamToString(instream);

	                // Closing the input stream will trigger connection release
	                instream.close();*/
	            }

	        }
	        catch (ClientProtocolException e)
	        {
	            client.getConnectionManager().shutdown();
	            e.printStackTrace();
	        }
	        catch (IOException e)
	        {
	            client.getConnectionManager().shutdown();
	            e.printStackTrace();
	        }
	        return null;
	    }
	    public enum RequestMethod
	    {
	        GET,
	        POST
	    }
	}
	
	
	
		
	 static List<String> getAAgentList(String ec, List<dealerPendingOrder>dealList, Connection db) throws Exception {
		
		 	
	    	List <String> activeList = new LinkedList<>();
	    	
	    	String agentQuery = "select Acode from paytmagententry where active_status=1 and AStatus = 'Y' "
	    			+ "and emp_type='OUT' and ROLE='MR'  and acode in "
	    			+ "(select userid from HSRP_runnerECMapping where ec_code ="+ec+ ")"
	    			+ "limit "+ dealList.size();
	    	
	    	Statement sql = db.createStatement();
	    	
	    	String ss = "agent";
	    	
	    	try {
	    		ResultSet countAgent = sql.executeQuery(agentQuery);
	        	while(countAgent.next()) {
	        		
	        		String agent = countAgent.getString(1);
	        		activeList.add(agent);
	        		ss += agent+",";
	        	
	        	}
	    	}catch(SQLException e) {
	    		e.printStackTrace();
	    	}
	    	
//	    	HttpClient httpClient = new DefaultHttpClient();
	        try {
	        	
	        	if(ss!=null) {
	        		
	        		ss.substring(0, ss.length() - 1);

//	        		HttpPost postRequest = new HttpPost("http://172.25.38.119:8090/HSRPCircuit/REST/AgentLatLongECwise");
//    
//	        		List<NameValuePair> data = new  ArrayList<NameValuePair>();
//		            data.add(new BasicNameValuePair("AgentList",ss));
//		            data.add(new BasicNameValuePair("ec_code",ec));
//		            
//		            postRequest.setEntity(new UrlEncodedFormEntity(data));   
// 					Execute HTTP request
//		            HttpResponse response = httpClient.execute(postRequest);
//		            System.out.println(response.getStatusLine().getStatusCode());
//		            if (response.getStatusLine().getStatusCode() != 200) {
//		    			throw new RuntimeException("Failed : HTTP error code : "
//		    			   + response.getStatusLine().getStatusCode());
//		    		}
//		            HttpEntity respEntity = response.getEntity();
//	                if (respEntity != null) {
//	                    // EntityUtils to get the response content
//	                    String content =  EntityUtils.toString(respEntity);
//	                    System.out.println(content);
//	                }
	                
	        		System.out.println("Calling Agent Api....");
	        		
	        		RestClient client=new RestClient("http://172.25.38.119:8090/HSRPCircuit/REST/AgentLatLongECwise");
	        		JSONObject obj=new JSONObject();
	        		
	        		obj.put("AgentList",ss);
	        		obj.put("ec_code", ec);

	        		client.Execute(RequestMethod.POST);	        		
	        		client.getResponse();
	        		
	                
		    
	        	}
	        }
	        	
	            
//
//	            
//	            System.out.println(response.getEntity().getContent());
//		            
//	    		BufferedReader br = new BufferedReader(
//	                             new InputStreamReader((response.getEntity().getContent())));
//
//	    		String output;
//	    		
//	    		System.out.println("Output from Server .... \n");
//	    		while ((output = br.readLine()) != null) {
//	    			System.out.println(output);
//	    		}
//
//	        }
	        catch(ClientProtocolException e) {
	        	
	    		e.printStackTrace();

	    	  } catch (IOException e) {
	    	
	    		e.printStackTrace();
	    	  }

	    	return activeList;
	    }
	 
	 
	 
	 public static void sort(int arr[][], int col) 
	    { 
	        // Using built-in sort function Arrays.sort 
	        Arrays.sort(arr, new Comparator<int[]>() { 
	            
	                        
	          // Compare values according to columns 
	          @Override
			public int compare(final int[] entry1, final int[] entry2) { 
	        	  
	            // To sort in descending order revert  
	            // the '>' Operator 
	            if (entry1[col] < entry2[col]) 
	                return 1; 
	            else
	                return -1; 
	          } 
	        });  // End of function call sort(). 
	    }

	 
	 
	static DistributeRunner dr = new DistributeRunner();
	 
 	static List <agentOrder> agentDetail = new LinkedList<>();
 	
 	static List <bulk> bulkOrders = new LinkedList<>();

	
 	
 	static void quadClusterP2(List<dealerPendingOrder>dealListP2, List<Location>locList, Location ecLoc, List<String>agentList, Connection db) throws Exception {
			//dealer list quadrant wise 
	 		List<dealerPendingOrder>firstQuadId = new LinkedList<>(); 
	    	List<dealerPendingOrder>secQuadId = new LinkedList<>(); 
	    	List<dealerPendingOrder>thirdQuadId = new LinkedList<>(); 
	    	List<dealerPendingOrder>fourQuadId = new LinkedList<>();
	    	
	    	//dealer location quadrant wise     	
	    	List<Location>firstQuadLoc = new LinkedList<>();
	    	List<Location>secQuadLoc = new LinkedList<>();
	    	List<Location>thirdQuadLoc = new LinkedList<>(); 
	    	List<Location>fourQuadLoc = new LinkedList<>();
	    	
	    	//to count number of bulk orders in each quadrant
    		int[] bulk = {0,0,0,0};
    		
    		int plateCount[]= {0,0,0,0};
    		
    		
//	    	for(int i=0;i<agentList.size();i++) {
//    			agentOrder a = dr.new agentOrder();
//    			a.agent = agentList.get(i);
//    			a.ordersCount = 0;
//    			agentDetail.add(a);
//    		}
    		
    		
    		// allocating pending dealers order to respective agent and removing them from dealer list 
//    		for(int i = 0; i < dealList.size(); i++) {
//    			
//    			String pendingAgent = getPendencyAgent(dealList.get(i),db);
//    			if(pendingAgent != null) {
//    					long dealer = dealList.get(i);
//    					String aa = pendingAgent;
//    					dealList.remove(i);
//    						
//    					for(int j=0;j<agentDetail.size();j++) {
//    						if(agentDetail.get(j).agent == aa) {
//    							agentDetail.get(j).dealers.add(dealer);
//    							agentDetail.get(j).ordersCount += getPlateCount(dealer,db);
//    	     				
//    						}
//    					}
//    		    		
//    					System.out.println("dealer - "+ dealer + "pendency agent allocated - "+aa);
//    			}
//    		}
    		
    		// for P2 orders 
    		
	    	for(int i = 0; i < dealListP2.size(); i++) {
	    		if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			dealListP2.get(i).quadrant=0;
	    			firstQuadId.add(dealListP2.get(i));
	    			firstQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			dealListP2.get(i).quadrant=1;
	    			secQuadId.add(dealListP2.get(i));
	    			secQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			dealListP2.get(i).quadrant=2;
	    			thirdQuadId.add(dealListP2.get(i));
	    			thirdQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			dealListP2.get(i).quadrant=3;
	    			fourQuadId.add(dealListP2.get(i));
	    			fourQuadLoc.add(locList.get(i));
	    		}
	    	}
	    		
	    		System.out.println("First Quad List");
	    		for(int j=0;j<firstQuadId.size();j++) {
	    			int count = firstQuadId.get(j).plate_count ;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=firstQuadId.get(j).dealer_id;
	    				bulkOrders.add(b);
	    				bulk[0]++;
	    			}
	    			plateCount[0] += count ;
	    			
	    			System.out.println(firstQuadId.get(j)+ " Lat-"+firstQuadLoc.get(j).latitude+ " Long-"+firstQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nSecond Quad List");
	    		for(int j=0;j<secQuadId.size();j++) {
	    			int count = secQuadId.get(j).plate_count;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=secQuadId.get(j).dealer_id;
	    				bulkOrders.add(b);
	    				bulk[1]++;
	    			}
	    			plateCount[1] += count ;
	    			
	    			System.out.println(secQuadId.get(j)+ " Lat-"+secQuadLoc.get(j).latitude+ " Long-"+secQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nThird Quad List");
	    		for(int j=0;j<thirdQuadId.size();j++) {
	    			int count = thirdQuadId.get(j).plate_count;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=thirdQuadId.get(j).dealer_id;
	    				bulkOrders.add(b);
	    				bulk[2]++;
	    			}
	    			plateCount[2] += count ;
	    			
	    			System.out.println(thirdQuadId.get(j)+ " Lat-"+thirdQuadLoc.get(j).latitude+ " Long-"+thirdQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nFourth Quad List");
	    		for(int j=0;j<fourQuadId.size();j++) {
	    			int count = fourQuadId.get(j).plate_count;
	    			
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=fourQuadId.get(j).dealer_id;
	    				bulkOrders.add(b);
	    				bulk[3]++;
	    			}
	    			
	    			plateCount[3] += count ;
	    			
	    			System.out.println(fourQuadId.get(j)+ " Lat-"+fourQuadLoc.get(j).latitude+ " Long-"+fourQuadLoc.get(j).longitude);
	    		}
	    		
	    		
	    		int remAgent=0;
	    		
	    		//to store number of runners for each quadrant
	    		int quadAgent[] = {0,0,0,0};
	    		
	    		
	    		//to store quadrant number (1,2,3,4) and distinct dealers in each according to orders
	    		int agent[][]=	{ 	{ 0, 0 }, 
			                        { 1, 0 }, 
			                        { 2, 0 }, 
			                        { 3, 0 }
			                    };
	    		
	    		
	    		
	    		//refers to quadrant number 0 1 2 3
	    		int quadNum=0;
	    		
	    		
	    		
	    		if(firstQuadId.size()!=0)
	    		{
	    		
	    			agent[quadNum][0]=0; 
	    			agent[quadNum][1]=firstQuadId.size();
	    			
	    		}
	    		if(secQuadId.size()!=0)
	    		{
	    		
	    			agent[quadNum+1][0]=1;
	    			agent[quadNum+1][1]=secQuadId.size();
	    			
	    		}
	    		if(thirdQuadId.size()!=0)
	    		{
	    		
	    			agent[quadNum+2][0]=2;
	    			agent[quadNum+2][1]=thirdQuadId.size();
	    			
	    		}
	    		if(fourQuadId.size()!=0)
	    		{
	    		
	    			agent[quadNum+3][0]=3;
	    			agent[quadNum+3][1]=fourQuadId.size();
	    			
	    		}
	    		
	    		
	    			
	    		List< List<agentOrder> > agentListP2 = new LinkedList<>();
	    		List<agentOrder> list = new LinkedList<>();
	    		
	    		agentListP2.add(0,list);
	    		agentListP2.add(1,list);
	    		agentListP2.add(2,list);
	    		agentListP2.add(3,list);
	    		agentListP2.add(4,list);
	    		
	    		DistributeRunner dr = new DistributeRunner();
	    		
	    		for(int i=0;i<agentDetail.size();i++) {
	    			
	    			if(agentDetail.get(i).quadNumber == 0) {
	    				quadAgent[0]++;
	    				agentOrder a = dr.new agentOrder();
	    				a.agent = agentDetail.get(i).agent;
	    				a.quadNumber = agentDetail.get(i).quadNumber;
	    				agentListP2.get(0).add(a);
	    			}
	    			else if(agentDetail.get(i).quadNumber == 1) {
	    				quadAgent[1]++;
	    				agentOrder a = dr.new agentOrder();
	    				a.agent = agentDetail.get(i).agent;
	    				a.quadNumber = agentDetail.get(i).quadNumber;
	    				agentListP2.get(1).add(a);
	    			}
	    			else if(agentDetail.get(i).quadNumber == 2) {
	    				quadAgent[2]++;
	    				agentOrder a = dr.new agentOrder();
	    				a.agent = agentDetail.get(i).agent;
	    				a.quadNumber = agentDetail.get(i).quadNumber;
	    				agentListP2.get(2).add(a);
	    			}
	    			else if(agentDetail.get(i).quadNumber == 3) {
	    				quadAgent[3]++;
	    				agentOrder a = dr.new agentOrder();
	    				a.agent = agentDetail.get(i).agent;
	    				a.quadNumber = agentDetail.get(i).quadNumber;
	    				agentListP2.get(3).add(a);
	    			}
	    			else if(agentDetail.get(i).quadNumber == NOT_ASSIGNED) {
	    				agentOrder a = dr.new agentOrder();
	    				a.agent = agentDetail.get(i).agent;
	    				a.quadNumber = agentDetail.get(i).quadNumber;
	    				agentListP2.get(4).add(a);
	    			}
	    		}
	    		
	    		sort(agent,1);
	    		
	    		
	    		//distributing remaining runners to quadrants with 0 runners and non zero P2 dealers
	    		for(int i=0;i<4;i++) {
	    			
	    			if( agent[i][1]>0 && quadAgent[agent[i][0]]==0 && remAgent>0)
	    			{	quadAgent[agent[i][0]]++;

	    				agentOrder a = dr.new agentOrder();
    					a.agent = agentListP2.get(4).get(0).agent;
    					a.quadNumber = agent[i][0];
    					
	    				agentListP2.get(agent[i][0]).add(a);
	    				System.out.println(agentListP2.get(4).get(0));
	    				agentListP2.get(4).remove(0);
	    				remAgent--;
	    			}
	    		}

	    		//distributing remaining runners to quadrants according to number of dealers and capacity
	    		for(int i=0;i<4;i++) {
	    			
	    			if(remAgent>0) {
	    				
	    				if( agent[i][1]>0) {
	    					if(quadAgent[agent[i][0]] < agent[i][1] && plateCount[agent[i][0]] > 300*agent[i][1]  )
	    					{
			    				quadAgent[agent[i][0]]++;
			    				agentOrder a = dr.new agentOrder();
		    					a.agent = agentListP2.get(4).get(0).agent;
		    					a.quadNumber = agentDetail.get(i).quadNumber;
			    				agentListP2.get(agent[i][0]).add(a);
			    				
			    				agentListP2.get(4).remove(0);
			    				remAgent--;
	    					}
	    					
	    				}
	    				
	    			}
	    			
	    		}
	    		
	    		
	    		//distributing remaining runners to quadrants according to number of dealers
	    		for(int i=0;i<3;i++) {
	    			
	    			if(remAgent>0) {
	    				
	    				if( agent[i][1]>0) {
	    					if(quadAgent[agent[i][0]] < agent[i][1] && quadAgent[agent[i+1][0]] < agent[i+1][1])
	    					{	int quad = 0;
	    					
	    					
			    				if(plateCount[agent[i][0]] >= plateCount[agent[i+1][0]])
			    				{	quad = agent[i][0];
			    					quadAgent[agent[i][0]]++;
			    				}
			    				else 
			    				{
			    					quadAgent[agent[i+1][0]]++;;
				    				quad = agent[i+1][0] ;
				    				
			    				}
			    					
		    					agentOrder a = dr.new agentOrder();
		    					a.agent = agentListP2.get(4).get(0).agent;
		    					a.quadNumber = agentDetail.get(i).quadNumber;
			    				agentListP2.get(quad).add(a);
			    				
			    				agentListP2.get(4).remove(0);	
			    				remAgent--;
	    					}
	    					
	    				}
	    				
	    			}
	    			
	    		}
	    			    		
	    		
	    		
	    		
	    		
	    		
	    		
    				
		    		
		    		//1st quad clusters
		    		System.out.println("***clusters for P2 1st quad***");
		    		if(firstQuadId.size()>0 && quadAgent[0]>0) {
		    			Clustering.Cluster[] clusters1 = Clustering.kmeansClusters(firstQuadLoc,firstQuadId.size(),2,quadAgent[0]);
		    				
			    		for(int i=0 ; i < agentListP2.get(0).size();i++) {

			    			agentOrder a = dr.new agentOrder();
			    			dealerPendingOrder d = alloc.new dealerPendingOrder();
			    			
			    			a.quadNumber=0;	
			    			int flag = 0;
			    			ArrayList<Integer> members = clusters1[i].getMembership();
			    			
			    			System.out.println("cluster- "+(i+1));
			    			
			    			a.agent = agentListP2.get(0).get(i).agent;
			    			
			    			for(int j=0; j<members.size();j++)
			    			{
			    				System.out.println("dealerId (P2)-"+ firstQuadId.get(members.get(j))); 
			    				
			    				d.dealer_id=firstQuadId.get(members.get(j)).dealer_id;
			    				d.plate_count= firstQuadId.get(members.get(j)).plate_count;
			    				
			    				a.dealers.add(d);
			    				a.ordersCount += d.plate_count;
			    				
			    				for(int k=0;k<agentDetail.size();k++) {
			    					if(a.agent.equals(agentDetail.get(k).agent)) {
			    						
			    						agentDetail.get(k).dealers.add(d);
			    						agentDetail.get(k).ordersCount += a.ordersCount;
			    						flag = 1;
			    						break;
			    					}
			    				} 
  	    				
			    			}
			    			if(flag == 0)
			    				agentDetail.add(a);
  						
			    			System.out.println("AgentId- "+ agentListP2.get(0).get(i).agent);
			    			System.out.println();
		       			 	
			    		}
		    		}
		    		
	
		    		
		    		
		    		
		    		
		    		//2nd quad clusters
		    		System.out.println("***clusters for P2 2nd quad***");
		    		
		    		if(secQuadId.size()>0 && quadAgent[1]>0) {
		    			Clustering.Cluster[] clusters2 = Clustering.kmeansClusters(secQuadLoc,secQuadId.size(),2,quadAgent[1]);
		    		
		    			
		    		
		    			for(int i=0;i<agentListP2.get(1).size();i++) {
		        			int flag=0;
		        			
		        			agentOrder a = dr.new agentOrder();
			    			dealerPendingOrder d = alloc.new dealerPendingOrder();
			    			   
		        			ArrayList<Integer> members = clusters2[i].getMembership();
		        			System.out.println("cluster- "+(i+1));
		        			for(int j=0; j<members.size();j++)
		        			{
		        				System.out.println("dealerId (P2)-"+ secQuadId.get(members.get(j)));
		        				
		        				d.dealer_id=secQuadId.get(members.get(j)).dealer_id;
			    				d.plate_count= secQuadId.get(members.get(j)).plate_count;
			    				
			    				a.quadNumber=1;
			    				a.agent = agentListP2.get(1).get(i).agent;				
			    				a.dealers.add(d);	
		        				a.ordersCount += d.plate_count; 
		        				a.quadNumber=1;
		        				
			    				for(int k=0;k<agentDetail.size();k++) {
			    					if(a.agent.equals(agentDetail.get(k).agent)) {
			    						agentDetail.get(k).dealers.add(d);
			    						agentDetail.get(k).ordersCount += a.ordersCount;
			    						flag = 1;
			    						break;
			    					}
			    				} 
		        			
		        			}
		        			
		        			if(flag == 0)
		        				agentDetail.add(a);
  					
		        			System.out.println( "AgentId- "+ agentListP2.get(0).get(i).agent);
		        			System.out.println();
		        			
		        		}
		    		}
		    		
		    		
		    		
		    		
		    		//3rd quad clusters
		    		
		    		
		    		
		    		System.out.println("***clusters for P2 3rd quad***");
		    		
		    		if(thirdQuadId.size()>0 && quadAgent[2]>0) {
		 
		    			Clustering.Cluster[] clusters3 = Clustering.kmeansClusters(thirdQuadLoc,thirdQuadId.size(),2,quadAgent[2]);
		    			
		        		for(int i=0;i<agentListP2.get(2).size();i++) {
		        			agentOrder a = dr.new agentOrder();
			    			dealerPendingOrder d = alloc.new dealerPendingOrder();
			    			
			    			int flag =0;
		        			ArrayList<Integer> members = clusters3[i].getMembership();
		        			System.out.println("cluster- "+(i+1));
		        			for(int j=0; j<members.size();j++)
		        			{	
		        				d.dealer_id=thirdQuadId.get(members.get(j)).dealer_id;
		    					d.plate_count=thirdQuadId.get(members.get(j)).plate_count;
		    				
		        				a.dealers.add(d);
		        				a.ordersCount += d.plate_count; 
		        				a.quadNumber=2;
		        				a.agent = agentListP2.get(2).get(i).agent;	
				    				
		        				System.out.println("dealerId (P2)-"+ thirdQuadId.get(members.get(j)));
		        				
		        				for(int k=0;k<agentDetail.size();k++) {
			    					if(a.agent.equals(agentDetail.get(k).agent)) {
			    						agentDetail.get(k).dealers.add(d);
			    						agentDetail.get(k).ordersCount += a.ordersCount;
			    						flag = 1;
			    						break;
			    					}
			    				}
		        			}
		        			
		        			if(flag==0)
		        			 agentDetail.add(a);
  						
				   			 System.out.println("AgentId- "+ agentListP2.get(2).get(i).agent);
				   		   	 System.out.println();
				   			 agentIndex++;
		        		}
		    		}
		    		
		    		
		    		
		    		
		    		//4th quad clusters
		    		System.out.println("***clusters for P2 4th quad***");
		    		
		    		if(fourQuadId.size()>0 && quadAgent[3]>0) {
		    			Clustering.Cluster[] clusters4 = Clustering.kmeansClusters(fourQuadLoc,fourQuadId.size(),2,quadAgent[3]);
		    			
		        		for(int i=0;i<agentListP2.get(3).size();i++) {

			    			agentOrder a = dr.new agentOrder();
			    			dealerPendingOrder d = alloc.new dealerPendingOrder();
			    			
		        			int flag = 0;
		        			ArrayList<Integer> members = clusters4[i].getMembership();
		        			System.out.println("cluster- "+(i+1));
		        			for(int j=0; j<members.size();j++)
		        			{	
		        				System.out.println("dealerId (P2)-"+ fourQuadId.get(members.get(j)) );
		        				
		        				d.dealer_id=fourQuadId.get(members.get(j)).dealer_id;
			    				d.plate_count=fourQuadId.get(members.get(j)).plate_count;
			    				
		        				a.quadNumber=3;
			        			a.agent = agentListP2.get(3).get(i).agent;
		        				a.dealers.add(d);
		        				a.ordersCount += d.plate_count;
		        						
		        				for(int k=0;k<agentDetail.size();k++) {
			    					if(a.agent.equals(agentDetail.get(k).agent)) {
			    						
			    						agentDetail.get(k).dealers.add(d);
			    						agentDetail.get(k).ordersCount += a.ordersCount;
			    						flag = 1;
			    						break;
			    					
			    					}
			    				}
		        			}
		        			if(flag==0)
		        				agentDetail.add(a);
  						
		        			 System.out.println("AgentId- "+ agentListP2.get(0).get(i).agent);
		        			 System.out.println();
		        			 agentIndex++;
		        		
		        		}
	
		    		}
	
	    		
	    		
	    		
	    		
	 }
	 
	 
	 
	 
	 
	 
	 
	 static void quadClusterP1(List<dealerPendingOrder>dealListP1, List<Location>locList, Location ecLoc, List<String>agentList, Connection db) throws Exception {
		 	
		 	
			//to store dealer id quadrant wise
		 	
		 	List<dealerPendingOrder> firstQuadId = new LinkedList<>(); 
	    	List<dealerPendingOrder>secQuadId=new LinkedList<>(); 
	    	List<dealerPendingOrder>thirdQuadId=new LinkedList<>(); 
	    	List<dealerPendingOrder>fourQuadId=new LinkedList<>();
	    	

			//to store dealer location quadrant wise
	    	List<Location> firstQuadLoc=new LinkedList<>();
	    	List<Location> secQuadLoc=new LinkedList<>();
	    	List<Location>thirdQuadLoc=new LinkedList<>(); 
	    	List<Location>fourQuadLoc=new LinkedList<>();
	    	
	    	int plateCount[]= {0,0,0,0};
//	    	int plateCount2=0;
//	    	int plateCount3=0;
//	    	int plateCount4=0;
//	    	
//	    	int plateExceeded = 0;
	    	
	    	//to store number of runners for each quadrant
    		int quadAgent[] = {0,0,0,0};
    		
    		
    		//to store quadrant number (1,2,3,4) and distinct dealers in each according to orders
    		int agent[][]=	{ 	{ 0, 0 }, 
		                        { 0, 0 }, 
		                        { 0, 0 }, 
		                        { 0, 0 }
		                    };
    		
    		//refers to quadrant number 0 1 2 3
    		int quadNum=0;
    		
    		//to count number of bulk orders in each quadrant
    		int[] bulk = {0,0,0,0};
    		
    		int agentCount = agentList.size();
    		
    		List<Integer> bulkDealerIndex = new LinkedList<>();
    		
    		int bulkIndex = 0;
    		int agentIndex = 0;
    		
    		
    		
    		// allocating pending dealers order to respective agent and removing them from dealer list 
//    		for(int i = 0; i < dealList.size(); i++) {
//    			
//    			String pendingAgent = getPendencyAgent(dealList.get(i),db);
//    			if(pendingAgent != null) {
//    					long dealer = dealList.get(i);
//    					String aa = pendingAgent;
//    					dealList.remove(i);
//    						
//    					for(int j=0;j<agentDetail.size();j++) {
//    						if(agentDetail.get(j).agent == aa) {
//    							agentDetail.get(j).dealers.add(dealer);
//    							agentDetail.get(j).ordersCount += getPlateCount(dealer,db);
//    	     				
//    						}
//    					}
//    		    		
//    					System.out.println("dealer - "+ dealer + "pendency agent allocated - "+aa);
//    			}
//    		}
    		
    		// for P1 orders 
    		
	    	for(int i = 0; i < dealListP1.size(); i++) {
	    		if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			
	    			dealListP1.get(i).quadrant=0;
	    			
	    			int count = dealListP1.get(i).plate_count;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=dealListP1.get(i).dealer_id;
	    				b.first_order_date_time=dealListP1.get(i).first_order_date_time;
	    				b.order_ready_time=dealListP1.get(i).order_ready_time;
	    			
	    				bulkOrders.add(b);
	    				bulkDealerIndex.add(i);
	    				bulk[0]++;
	    			}
	    			else {
	    				plateCount[0] += count ;
	    				firstQuadId.add(dealListP1.get(i));
		    			firstQuadLoc.add(locList.get(i));
		    				
	    			}
	    				
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			dealListP1.get(i).quadrant=1;
	    			int count = dealListP1.get(i).plate_count;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.order_ready_time=dealListP1.get(i).order_ready_time;
	    				b.first_order_date_time=dealListP1.get(i).first_order_date_time;
	    				b.dealer_id=dealListP1.get(i).dealer_id;
	    				b.updated_at=dealListP1.get(i).updated_at;
	    				bulkOrders.add(b);
	    				bulkDealerIndex.add(i);
	    				bulk[1]++;
	    			}
	    			else {
	    				plateCount[1] += count ;
	    				secQuadId.add(dealListP1.get(i));
		    			secQuadLoc.add(locList.get(i));
		    		
	    			}
	    			
	    			}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			dealListP1.get(i).quadrant=2;
	    			int count = dealListP1.get(i).plate_count;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.dealer_id=dealListP1.get(i).dealer_id;
	    				b.order_ready_time = dealListP1.get(i).order_ready_time;
	    				b.priority = dealListP1.get(i).priority;
	    				b.dealer_id=dealListP1.get(i).dealer_id;
	    				b.updated_at=dealListP1.get(i).updated_at;
	    				
	    				bulkOrders.add(b);
	    				bulkDealerIndex.add(i);
	    				bulk[2]++;
	    			}
	    			else
	    			{
	    				plateCount[2] += count ;
	    				thirdQuadId.add(dealListP1.get(i));
		    			thirdQuadLoc.add(locList.get(i));
		    			
	    			}
	    			
	    		
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			int count = dealListP1.get(i).plate_count;
	    			dealListP1.get(i).quadrant=0;
	    			if(count>=PLATE_CAPACITY) {
	    				bulk b = dr.new bulk();
	    				b.bulk_plate_count=count;
	    				b.order_ready_time = dealListP1.get(i).order_ready_time;
	    				b.priority = dealListP1.get(i).priority;
	    				b.dealer_id=dealListP1.get(i).dealer_id;
	    				b.updated_at=dealListP1.get(i).updated_at;
	    				
	    				bulkOrders.add(b);
	    				bulkDealerIndex.add(i);
	    				bulk[3]++;
	    			}
	    			
	    			else
	    			{
	    				plateCount[3] += count ;
	    				fourQuadId.add(dealListP1.get(i));
		    			fourQuadLoc.add(locList.get(i));
	    			
	    			}
		    		
	    		}
	    	}
	    		
	    		System.out.println("First Quad List");
	    		for(int j=0;j<firstQuadId.size();j++) {
	    			
	    			System.out.println(firstQuadId.get(j).dealer_id+ " Lat-"+firstQuadLoc.get(j).latitude+ " Long-"+firstQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nSecond Quad List");
	    		for(int j=0;j<secQuadId.size();j++) {
	    			
	    			System.out.println(secQuadId.get(j).dealer_id+ " Lat-"+secQuadLoc.get(j).latitude+ " Long-"+secQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nThird Quad List");
	    		for(int j=0;j<thirdQuadId.size();j++) {
	    			System.out.println(thirdQuadId.get(j).dealer_id+ " Lat-"+thirdQuadLoc.get(j).latitude+ " Long-"+thirdQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nFourth Quad List");
	    		for(int j=0;j<fourQuadId.size();j++) {
	    			
	    			System.out.println(fourQuadId.get(j).dealer_id+ " Lat-"+fourQuadLoc.get(j).latitude+ " Long-"+fourQuadLoc.get(j).longitude);
	    		}
	    		
	    		
	    		
	    		// one agent assigned to each bulk order
	    		if(agentCount > bulkOrders.size() && bulkOrders.size()>0) {
	    				for(int i=0;i<bulkOrders.size();i++) {
		    				
	    					agentOrder a = dr.new agentOrder();
	    					dealerPendingOrder d = alloc.new dealerPendingOrder();
	    		    		
	    					dealListP1.remove(bulkDealerIndex.get(bulkIndex));
	    					
	    					d.dealer_id=bulkOrders.get(i).dealer_id;
	    					d.plate_count = bulkOrders.get(i).bulk_plate_count;
	    					d.first_order_date_time=bulkOrders.get(i).order_ready_time;
	    					d.priority=bulkOrders.get(i).priority;
	    					
	    					
	    					a.dealers.add(d);
	    					a.ordersCount += d.plate_count; 
	    					a.agent = agentList.get(agentIndex);
	    					
	    					agentIndex++;
	    					bulkIndex++;
	    					agentDetail.add(a);
	    					
		    			}	
	    				agentCount -= bulkOrders.size();
	    				
	    		}
	    		
	    		else if(bulkOrders.size()>0){
	    			for(int i=0;i<agentList.size();i++) {
	    				while(agentCount>0) {
	    					
	    					agentOrder a = dr.new agentOrder();
	    					dealerPendingOrder d = alloc.new dealerPendingOrder();
	    		    		
	    					dealListP1.remove(bulkDealerIndex.get(bulkIndex));

	    					d.dealer_id=bulkOrders.get(i).dealer_id;
	    					d.plate_count= bulkOrders.get(i).bulk_plate_count;
	    					d.first_order_date_time=bulkOrders.get(i).order_ready_time;
	    					d.priority=bulkOrders.get(i).priority;
	    					
	    					a.dealers.add(d);
	    					a.ordersCount += d.plate_count; 
	    					a.agent = agentList.get(agentIndex);
	    					agentIndex++;
	    					bulkIndex++;
	    					agentDetail.add(a);
	    					agentCount--;
	    				}
	    			}
	    		}
	    		
	    		
	    		// if runners > bulk orders
	    		if(firstQuadId.size()!=0)
	    		{
	    			quadAgent[0]++;
	    			agent[quadNum][0]=0; 
	    			agent[quadNum][1]=firstQuadId.size();
	    			
	    		}
	    		if(secQuadId.size()!=0)
	    		{
	    			quadAgent[1]++;
	    			agent[quadNum+1][0]=1;
	    			agent[quadNum+1][1]=secQuadId.size();
	    			
	    		}
	    		if(thirdQuadId.size()!=0)
	    		{
	    			quadAgent[2]++;
	    			agent[quadNum+2][0]=2;
	    			agent[quadNum+2][1]=thirdQuadId.size();
	    			
	    		}
	    		if(fourQuadId.size()!=0)
	    		{
	    			quadAgent[3]++;
	    			agent[quadNum+3][0]=3;
	    			agent[quadNum+3][1]=fourQuadId.size();
	    			
	    		}
	    		
	    		
	    		
	    		int quadCluster = 0;
	    		
	    		if(agentCount == 0) {
	    			quadCluster = 0;
	    		}
	    		
	    		
	    		//if runners less than quadrants with orders
	    		else if(agentCount<(quadAgent[0]+quadAgent[1]+quadAgent[2]+quadAgent[3])) {
	    			quadCluster = 1;
	    		}
	    		
	    		//runners greater than quadrants with orders
	    		else {
	    			
	    			quadCluster = 2;
	    			agentCount -= quadAgent[0]+quadAgent[1]+quadAgent[2]+quadAgent[3];
	    			
	    			sort(agent,1);
	    			
	    			if(agentCount>0) {
	    				
	    				
	    				//allocating in terms of capacity
	    				for(int i=0;i<4;i++) {
	    					
	    					int runnersReq = (plateCount[i]/300);
	    					runnersReq -- ;
	    					
	    					if(runnersReq * 300 < plateCount[i])
	    							runnersReq++;
	    					
	    					if(agentCount >= runnersReq)
	    					{
	    						quadAgent[i] += runnersReq;
	    						agentCount -= runnersReq;
	    					}
	    						
	    				}
	    				
	    				
	    				//descending order of distinct dealers
	    				// allocating all available runners
	    				// is problem
	    				//if runners > dealers
	    				
	    				while(agentCount>0) {
		    					for(int i=0; i<3;i++) {
			    					
		    						// allocating only to non zero quadrants and forcing runners to be less than= dealers
		    						if(agent[i][1] > 0 && agentCount > 0 && quadAgent[agent[i][0]] < agent[i][1]) {
		    							
		    							
			    							// quad with more distinct dealers gets more runners
		    								// and condition prevents runners to exceed dealers
					    					if( agent[i][1] > agent[i+1][1]  ) {
					    							
					    						if((plateCount[agent[i][0]] > 300*quadAgent[agent[i][0]]) || (plateCount[agent[i+1][0]] < 300*quadAgent[agent[i+1][0]])) 
					    						{
					    							 quadAgent[agent[i][0]]++;
					    							 agentCount--;
					    						}
					    						
					    						else {
					    							 quadAgent[agent[i+1][0]]++;
					    							 agentCount--;
					    						}
					    							
					    						
					    					}//if closing
					    					
					    					// if equal runners given to both if enough runners 
					    					else {
					    						
					    						if(agentCount>=2) {
					    							
					    								
						    							if( quadAgent[agent[i+1][0]] < agent[i+1][1] && agent[i+1][1]>0) {
						    								
						    								while((plateCount[agent[i][0]] > 300*quadAgent[agent[i][0]]) && agentCount>0) 
								    						{
								    							 quadAgent[agent[i][0]]++;
								    							 agentCount--;
								    						}
								    						
								    						while((plateCount[agent[i+1][0]] > 300*quadAgent[agent[i+1][0]])&& agentCount>0 ) {
								    							 quadAgent[agent[i+1][0]]++;
								    							 agentCount--;
								    						}
								    							
						    								//quadAgent[agent[i][0]]++;
							    							//quadAgent[agent[i+1][0]]++;
								  							//agentCount-=2;
						    							}
						    							else {
						    								quadAgent[agent[i][0]]++;
								  							agentCount-=1;
						    							}
					    								
					    							
					    						}
					    						
					    						else if(agentCount==1) {
					    							quadAgent[agent[i][0]]++;
						  							agentCount-=1;
					    						}
					    					}//else closing
					    					
					    					
		    						}//if closing
			    					
			    					
			    					
			    				}//for closing
	    				
	    				}//while closing
	    				
	    				
	    				
	    				
	    				
	    			}//if closing

	    		}//else closing
	    		
	    		
	    		switch(quadCluster) {
	    				
	    			  //no runners
	    			  case 0: System.out.println("No agents");
	    			  		  break;
	    			  		  
	    			  // runners less than quadrants with non zero dealers
	    			  case 1:		
	    				  	Clustering.Cluster[] cluster = Clustering.kmeansClusters(locList, dealListP1.size(), 2, agentCount);
	    				  	
	    				  	for(int i=0;i<agentCount;i++) {
		    				  		agentOrder a = dr.new agentOrder();
		    						dealerPendingOrder d = alloc.new dealerPendingOrder();
		    			    		
		    				  		ArrayList<Integer> members = cluster[i].getMembership();
		    				  		System.out.println("Cluster - "+(i+1));
		    				  
		    				  		
		    				  		for(int j=0;j<members.size();j++) {
		    				  			
			    				  			d.dealer_id=dealListP1.get(members.get(j)).dealer_id;
			    				  			d.plate_count=dealListP1.get(members.get(j)).plate_count;
			    				  			d.order_ready_time=dealListP1.get(members.get(j)).order_ready_time;
			    				  			d.priority = dealListP1.get(members.get(j)).priority;
			    				  			d.first_order_date_time=dealListP1.get(members.get(j)).first_order_date_time;
			    				  			d.quadrant = dealListP1.get(members.get(j)).quadrant;
			    				  			
			    				  			System.out.println("dealerId-"+ dealListP1.get(members.get(j)));
			    				  			
			    				  			a.dealers.add(d);
			    	    					a.ordersCount += d.plate_count; 		    	    					
		    	    					
		    				  		}
		    				  		 a.agent = agentList.get(i);
		    				  		 agentDetail.add(a);
		    						 System.out.println("AgentId- "+ agentList.get(i));
				        			 System.out.println();
			        			
	    				  	}
	    				  	break;

	  	    		  // runners greater than quadrants with non zero dealers
	  	    		  case 2 : 
	  	    			  
	  					    		//1st quad clusters
	  					    		System.out.println("***clusters for 1st quad***");
	  					    		if(firstQuadId.size()>0) {
	  					    			Clustering.Cluster[] clusters1 = Clustering.kmeansClusters(firstQuadLoc,firstQuadId.size(),2,quadAgent[0]);
	  					    			
	  					    			
	  					    					
	  						    		for(int i=0;i<quadAgent[0];i++) {
	  						    			agentOrder a = dr.new agentOrder();
		  									dealerPendingOrder d = alloc.new dealerPendingOrder();
		  						    		
	  						    			ArrayList<Integer> members = clusters1[i].getMembership();
	  						    			System.out.println("cluster- "+(i+1));
	  						    			for(int j=0; j<members.size();j++)
	  						    			{	
	  						    				d.dealer_id=firstQuadId.get(members.get(j)).dealer_id;
	  			    				  			d.plate_count=firstQuadId.get(members.get(j)).plate_count;
	  			    				  			d.order_ready_time=firstQuadId.get(members.get(j)).order_ready_time;
	  			    				  			d.priority = firstQuadId.get(members.get(j)).priority;
	  			    				  			d.first_order_date_time=firstQuadId.get(members.get(j)).first_order_date_time;
	  			    				  			d.quadrant = firstQuadId.get(members.get(j)).quadrant;  			
	  						    				
	  			    				  			System.out.println("dealerId-"+ firstQuadId.get(members.get(j)).dealer_id); 
	  						    				a.quadNumber=0;
	  						    				a.dealers.add(d);
	  			    	    					a.ordersCount += d.plate_count ; 
	  			    	    				
	  						    			}
	  						    			 a.agent = agentList.get(i);
	  			    				  		 agentDetail.add(a);
	  			    						
	  						    			System.out.println("AgentId- "+ agentList.get(agentIndex));
	  						    			System.out.println();
	  					       			 	agentIndex++;
	  						    		}
	  					    		}
	  					    		
	  				
	  					    		
	  					    		
	  					    		
	  					    		
	  					    		//2nd quad clusters
	  					    		
	  					    		System.out.println("***clusters for 2nd quad***");
	  					    		if(secQuadId.size()>0) {
	  					    			Clustering.Cluster[] clusters2 = Clustering.kmeansClusters(secQuadLoc,secQuadId.size(),2,quadAgent[1]);

	  					        		for(int i=0;i<quadAgent[1];i++) {

		  					    			agentOrder a = dr.new agentOrder();
		  									dealerPendingOrder d = alloc.new dealerPendingOrder();
		  									
		  					    			
	  					        			ArrayList<Integer> members = clusters2[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{	
	  					        				d.dealer_id=secQuadId.get(members.get(j)).dealer_id;
	  			    				  			d.plate_count=secQuadId.get(members.get(j)).plate_count;
	  			    				  			d.order_ready_time=secQuadId.get(members.get(j)).order_ready_time;
	  			    				  			d.priority = secQuadId.get(members.get(j)).priority;
	  			    				  			d.first_order_date_time=secQuadId.get(members.get(j)).first_order_date_time;
	  			    				  			d.quadrant = secQuadId.get(members.get(j)).quadrant;
	  					        				
	  			    				  			System.out.println("dealerId-"+ secQuadId.get(members.get(j)).dealer_id);
	  					        				
	  			    				  			a.dealers.add(d);
	  			    	    					a.ordersCount += d.plate_count;
	  			    	    					a.quadNumber=1;	
	  					        			}
	  					        			 a.agent = agentList.get(i);
	  			    				  		 agentDetail.add(a);
	  			    					
	  					        			System.out.println( "AgentId- "+ agentList.get(agentIndex));
	  					        			System.out.println();
	  					        			agentIndex++;
	  					        		}
	  					    		}
	  					    		
	  					    		
	  					    		
	  					    		
	  					    		//3rd quad clusters
	  					    		System.out.println("***clusters for 3rd quad***");
	  					    		if(thirdQuadId.size()>0) {
	  					    			Clustering.Cluster[] clusters3 = Clustering.kmeansClusters(thirdQuadLoc,thirdQuadId.size(),2,quadAgent[2]);
	  					    			
	  					        		for(int i=0;i<quadAgent[2];i++) {
	  					        			agentOrder a = dr.new agentOrder();
		  									dealerPendingOrder d = alloc.new dealerPendingOrder();
		  					    			a.quadNumber=2;
		  					        		
	  					        			ArrayList<Integer> members = clusters3[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{	
	  					        				d.dealer_id=thirdQuadId.get(members.get(j)).dealer_id;
	  			    				  			d.plate_count=thirdQuadId.get(members.get(j)).plate_count;
	  			    				  			d.order_ready_time=thirdQuadId.get(members.get(j)).order_ready_time;
	  			    				  			d.priority = thirdQuadId.get(members.get(j)).priority;
	  			    				  			d.first_order_date_time=thirdQuadId.get(members.get(j)).first_order_date_time;
	  			    				  			d.quadrant = thirdQuadId.get(members.get(j)).quadrant;
	  			    				  			
	  					        				a.dealers.add(d);
	  			    	    					a.ordersCount += d.plate_count; 
	  			    	    			
	  					        				System.out.println("dealerId-"+ thirdQuadId.get(members.get(j)).dealer_id);
	  					        			}
	  					        			 a.agent = agentList.get(i);
	  			    				  		 agentDetail.add(a);
	  			    						
	  							   			 System.out.println("AgentId- "+ agentList.get(agentIndex));
	  							   		   	 System.out.println();
	  							   			 agentIndex++;
	  					        		}
	  					    		}
	  					    		
	  					    		
	  					    		
	  					    		
	  					    		//4th quad clusters
	  					    		System.out.println("***clusters for 4th quad***");
	  					    		
	  					    		if(fourQuadId.size()>0) {
	  					    			Clustering.Cluster[] clusters4 = Clustering.kmeansClusters(fourQuadLoc,fourQuadId.size(),2,quadAgent[3]);

	  					    			
	  					        		for(int i=0;i<quadAgent[3];i++) {
	  					        			
	  					        			agentOrder a = dr.new agentOrder();
		  									dealerPendingOrder d = alloc.new dealerPendingOrder();
		  					        		
	  					        			ArrayList<Integer> members = clusters4[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{	
	  					        				d.dealer_id=fourQuadId.get(members.get(j)).dealer_id;
	  			    				  			d.plate_count=fourQuadId.get(members.get(j)).plate_count;
	  			    				  			d.order_ready_time=fourQuadId.get(members.get(j)).order_ready_time;
	  			    				  			d.priority = fourQuadId.get(members.get(j)).priority;
	  			    				  			d.first_order_date_time=fourQuadId.get(members.get(j)).first_order_date_time;
	  			    				  			d.quadrant=fourQuadId.get(members.get(j)).quadrant;
  			    				  			
	  					        				System.out.println("dealerId-"+ fourQuadId.get(members.get(j)).dealer_id );

	  			    	    					a.ordersCount += d.plate_count;
	  					        				a.dealers.add(d);
	  		  					    			a.quadNumber=3;
	  			    	    					
	  					        			}
	  					        		
	  					        			 a.agent = agentList.get(i);
	  			    				  		 agentDetail.add(a);
	  			    						
	  					        			 System.out.println("AgentId- "+ agentList.get(agentIndex));
	  					        			 System.out.println();
	  					        			 agentIndex++;
	  					        		
	  					        		}
	  				
	  					    		}
	  					    		
	  					}
	    		
	    		// to add all unsigned agents to agentOrder list 
	    		for(int i=0;i<agentList.size();i++) {
	    			int flag=0;
	    			for(int j=0;j<agentDetail.size();j++) {
	    				if(agentDetail.get(j).agent.equals(agentList.get(i)))
		    			{
		    				flag=1;
		    				break;
		    			}
	    			}
	    			if(flag==0) {
	    				agentOrder a = dr.new agentOrder();
		    			a.agent = agentList.get(i);
		    			a.ordersCount = 0;
		    			a.quadNumber=NOT_ASSIGNED;
		    			agentDetail.add(a);
	    			}
	    			
	    		}
	    		
	    		

	    		
	    		
	    }
	 
	 
	 //comparator class for sorting
	 public class dealerComparator implements Comparator<dealerPendingOrder>
	 {
	     public int compare(dealerPendingOrder p1, dealerPendingOrder p2)
	     {
	         // Assume no nulls, and simple ordinal comparisons

	         // First by priority
	         int priority = (p1.priority == p2.priority)?0:1;
	         if (priority != 0)
	         {
	             return priority;
	         }

	         // Next by plate_count
	         int plate = (p1.plate_count == p2.plate_count)?0:1;
	         if (plate != 0)
	         {
	             return plate;
	         }
	         
	         return 1;
	     }
	 }
	 
	 
	 //sorts dealers assigned to a runner according to priority and then plate_count 
	 static List<dealerPendingOrder> sortDealer(List<dealerPendingOrder>list){
		 
		 Collections.sort(list, dr.new dealerComparator() );
		 return list;
	 }
	 
	    
	 //final distribution method, clusters are checked on the basis of plate count,distance,allocation time
	 static void redistribute() throws Exception {
		 
		 for(int i=0; i<agentDetail.size(); i++ ) {
			 String agent = agentDetail.get(i).agent;
			 
			 System.out.println("Agent - "+agent);
			 System.out.println("Plates Assigned - "+ agentDetail.get(i).ordersCount);
			 
			
			  
			 //if over capacity 
			 if(agentDetail.get(i).ordersCount > 300)
			 {	 dealerPendingOrder removedDealer = alloc.new dealerPendingOrder();
				 sortDealer(agentDetail.get(i).dealers);
				 while(agentDetail.get(i).ordersCount>300) {
					 
					 removedDealer = agentDetail.get(i).dealers.get(0);
					 agentDetail.get(i).dealers.remove(0);
					 
					 for(int j=i+1; j<agentDetail.size();j++) {
						 if(removedDealer.quadrant == agentDetail.get(j).quadNumber) {
							 if((agentDetail.get(j).ordersCount + removedDealer.plate_count) <= PLATE_CAPACITY)
								 agentDetail.get(j).dealers.add(removedDealer);
						 }
					 
					  }
					 
				  }		
			
			  }
			 
			 //calculate max allocation time 
			 for(int j=0;j<agentDetail.get(i).dealers.size();j++) {
				 
			 }
			 
			 //calculate total distance and time 
			 for(int j=0;j<agentDetail.get(i).dealers.size();j++) {
				 
				 System.out.println("DealerId - " + agentDetail.get(i).dealers.get(j).dealer_id);
			 }
			  
		 }
		 
		 System.out.println("\nCalling Allocation Api....");
		 callAllocationApi();
	 }
	 
	 static JSONObject  getOrder(String agent, long dealer){
		   JSONObject order = new JSONObject();
		   order.put("Dealerid", dealer);
		   order.put("agent_code", agent);
		   return order ;
		} 
	 static void callAllocationApi() throws Exception {
		 
		 	RestClient client=new RestClient("http://172.25.38.119:8090/HSRPCircuit/REST/Allocation");
 			JSONArray orderArray = new JSONArray();
 			
 			for(int i=0;i<agentDetail.size();i++) {
 					for(int j=0;j<agentDetail.get(i).dealers.size();j++) {
 						long dealer = agentDetail.get(i).dealers.get(j).dealer_id;
 						String agent = agentDetail.get(i).agent;
 						orderArray.put( getOrder(agent, dealer));	
 					}
 			}
 			
 			JSONObject obj = new JSONObject();
 			obj.put("AssignOrders", orderArray);
 			
 			client.Execute(RequestMethod.POST);
    		client.getResponse();
    		
 			
 		
	 }
	 
	 
	 
	 // return list of P1 dealers
	 static List<dealerPendingOrder>getP1Dealers(List<dealerPendingOrder>dealList){
		 List<dealerPendingOrder> dealListP1 = new LinkedList<>();
		 for(int i=0;i<dealList.size();i++) {
			 if(dealList.get(i).priority==1)
				 dealListP1.add(dealList.get(i));
		 }
		 
		 return dealListP1;
	 }
	 
	 //return list of P2 dealers
	 static List<dealerPendingOrder>getP2Dealers(List<dealerPendingOrder>dealList){
		 List<dealerPendingOrder> dealListP2 = new LinkedList<>();
		 for(int i=0;i<dealList.size();i++) {
			 if(dealList.get(i).priority==2)
				 dealListP2.add(dealList.get(i));
		 }
		 
		 return dealListP2;
	 }
	 
	 
	 // called from Allocation()
	 static void allotRunners(List<dealerPendingOrder>dealList, List<Location>locList, String ec_code,Location ecLoc, Connection db) throws Exception {
		List<dealerPendingOrder> dealListP1 = new LinkedList<>();
		List<dealerPendingOrder> dealListP2 = new LinkedList<>();
		List<String> agentList = new LinkedList<>();
		
		agentList = getAAgentList(ec_code, dealList, db);
		
		long agentCount = agentList.size();
		
		if(agentCount>0) {
			
			System.out.println("&&&&&&&&& Total Available Agents = "+ agentCount +"&&&&&&&&&\n&&&&&&&&&& Available Agents -");
			
			for(int i=0;i<agentList.size();i++)
				System.out.println(agentList.get(i));
			
			System.out.println();
			
			dealListP1 = getP1Dealers(dealList);
			
			quadClusterP1(dealListP1, locList, ecLoc, agentList, db);
			
			dealListP2 = getP2Dealers(dealList);
			
			quadClusterP2(dealListP2, locList, ecLoc, agentList, db);
			
			redistribute();
		
		}
		else {
			System.out.println("NO RUNNERS");
		}
		
		
	 }
}
