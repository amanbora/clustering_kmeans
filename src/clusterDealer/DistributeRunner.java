package clusterDealer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.*;

import clusterDealer.Allocation.Location;









public class DistributeRunner {
	
	
	class agentOrder{
		String agent;
		List<Long> dealers;
		int ordersCount;
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
	
	
	
	
	
	static int getPlateCount(long dealer_id, Connection db) throws SQLException {

		int plateCount=0;
		String plateQuery = "SELECT Sum(plate_count)  FROM Test_HSRPCircuit.HSRP_order_detail det" + 
				" join HSRP_order_veh_reg_mapping oreg on det.order_ID = oreg.Challan_order_ID " + 
				" where oreg.dealer_ID = " + dealer_id + " and det.order_status_id !=6 ";
		

    	Statement sql = db.createStatement();
    	
    	
    	try {
    		ResultSet countPlate = sql.executeQuery(plateQuery);
        	while(countPlate.next()) {
        		 plateCount = countPlate.getInt(1);
        		
        		
        		//just to check
//        		System.out.println(agent);
        	}
    	}catch(SQLException e) {
    		e.printStackTrace();
    	}
    	return plateCount;
	}
	
		
	 static List<String> getAAgentList(String ec, List<Long>dealList, Connection db) throws SQLException {
		
		 	
	    	List <String> activeList = new LinkedList<>();
	    	
	    	String agentQuery = "select Acode from paytmagententry where active_status=1 and AStatus = 'Y' "
	    			+ "and emp_type='OUT' and ROLE='MR'  and acode in "
	    			+ "(select userid from HSRP_runnerECMapping where ec_code ="+ec+ ")"
	    			+ "limit "+ dealList.size();
	    	
	    	Statement sql = db.createStatement();
	    	
	    	
	    	try {
	    		ResultSet countAgent = sql.executeQuery(agentQuery);
	        	while(countAgent.next()) {
	        		String agent = countAgent.getString(1);
	        		activeList.add(agent);
	        		
	        		//just to check
//	        		System.out.println(agent);
	        	}
	    	}catch(SQLException e) {
	    		e.printStackTrace();
	    	}
	    	
	    	activeList.add("788888");
	    	activeList.add("SS00157");
//	    	activeList.add("SS00158");
//	    	activeList.add("SA00141");
//	    	activeList.add("SP1254");
//	    	activeList.add("SP3530");
//	    	activeList.add("SP4568");
	    	return activeList;
	    }
	 
	 
	 
	 public static void sort(int arr[][], int col) 
	    { 
	        // Using built-in sort function Arrays.sort 
	        Arrays.sort(arr, new Comparator<int[]>() { 
	            
	                        
	          // Compare values according to columns 
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
	
	 
	 static void quadCluster(List<Long>dealList, List<Location>locList, Location ecLoc, List<String>agentList, Connection db) throws Exception {
		    
		 	List <agentOrder> agentDetail = new LinkedList<>();
			
		 	
		 	List<Long> firstQuadId = new LinkedList<>(); 
	    	List<Long>secQuadId=new LinkedList<>(); 
	    	List<Long>thirdQuadId=new LinkedList<>(); 
	    	List<Long>fourQuadId=new LinkedList<>();
	    	
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
    		
    		DistributeRunner dr = new DistributeRunner();
    		
    		for(int i=0;i<agentList.size();i++) {
    			agentOrder a = dr.new agentOrder();
    			a.agent = agentList.get(i);
    			a.ordersCount = 0;
    			agentDetail.add(a);
    		}
    		
    		
    		// allocating pending dealers order to respective agent and removing them from dealer list 
    		for(int i = 0; i < dealList.size(); i++) {
    			
    			String pendingAgent = getPendencyAgent(dealList.get(i),db);
    			if(pendingAgent != null) {
    					long dealer = dealList.get(i);
    					String aa = pendingAgent;
    					dealList.remove(i);
    						
    					for(int j=0;j<agentDetail.size();j++) {
    						if(agentDetail.get(j).agent == aa) {
    							agentDetail.get(j).dealers.add(dealer);
    							agentDetail.get(j).ordersCount += getPlateCount(dealer,db);
    	     				
    						}
    					}
    		    		
    					System.out.println("dealer - "+ dealer + "pendency agent allocated - "+aa);
    			}
    		}
	    
    		
	    	for(int i = 0; i < dealList.size(); i++) {
	    		if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			firstQuadId.add(dealList.get(i));
	    			firstQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude >= ecLoc.longitude) {
	    			secQuadId.add(dealList.get(i));
	    			secQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude <= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			thirdQuadId.add(dealList.get(i));
	    			thirdQuadLoc.add(locList.get(i));
	    		}
	    		
	    		
	    		else if(locList.get(i).latitude >= ecLoc.latitude  && locList.get(i).longitude <= ecLoc.longitude) {
	    			fourQuadId.add(dealList.get(i));
	    			fourQuadLoc.add(locList.get(i));
	    		}
	    	}
	    		
	    		System.out.println("First Quad List");
	    		for(int j=0;j<firstQuadId.size();j++) {
	    			plateCount[0] += getPlateCount(firstQuadId.get(j),db);
	    			System.out.println(firstQuadId.get(j)+ " Lat-"+firstQuadLoc.get(j).latitude+ " Long-"+firstQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nSecond Quad List");
	    		for(int j=0;j<secQuadId.size();j++) {
	    			plateCount[1] += getPlateCount(secQuadId.get(j),db);
	    			System.out.println(secQuadId.get(j)+ " Lat-"+secQuadLoc.get(j).latitude+ " Long-"+secQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nThird Quad List");
	    		for(int j=0;j<thirdQuadId.size();j++) {
	    			plateCount[2] += getPlateCount(thirdQuadId.get(j),db);
	    			System.out.println(thirdQuadId.get(j)+ " Lat-"+thirdQuadLoc.get(j).latitude+ " Long-"+thirdQuadLoc.get(j).longitude);
	    		}
	    		
	    		System.out.println("\nFourth Quad List");
	    		for(int j=0;j<fourQuadId.size();j++) {
	    			plateCount[3] += getPlateCount(fourQuadId.get(j),db);
	    			System.out.println(fourQuadId.get(j)+ " Lat-"+fourQuadLoc.get(j).latitude+ " Long-"+fourQuadLoc.get(j).longitude);
	    		}
	    		
	    		
	    		
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
	    		
	    		int agentCount = agentList.size();
	    		
	    		int quadCluster = 0;
	    		
	    		if(agentCount == 0) {
	    			quadCluster = 0;
	    		}
	    		
	    		
	    		//if runners less than quadrants with orders
	    		else if(agentCount<(quadAgent[0]+quadAgent[1]+quadAgent[2]+quadAgent[3])) {
	    			quadCluster = 1;
	    		}
	    		
	    		//runners greater quadrants
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
								    						
								    						while((plateCount[agent[i+1][0]] > 300*quadAgent[agent[i+1][0]])&& agentCount>0) {
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
	    				  	Clustering.Cluster[] cluster = Clustering.kmeansClusters(locList, dealList.size(), 2, agentCount);
	    				  	
	    				  	for(int i=0;i<agentCount;i++) {
	    				  		ArrayList<Integer> members = cluster[i].getMembership();
	    				  		System.out.println("Cluster - "+(i+1));
	    				  		
	    				  		for(int j=0;j<members.size();j++) {
	    				  			System.out.println("dealerId-"+ dealList.get(members.get(j)) );
	    				  		}
	    				  		
	    						 System.out.println("AgentId- "+ agentList.get(i));
			        			 System.out.println();
			        			
	    				  		
	    				  	}
	    				  	break;

	  	    		  // runners greater than quadrants with non zero dealers
	  	    		  case 2 : 
	  	    			  			int agentIndex = 0;
	  	    			  			
	  	    			  			System.out.println("MaxAgents = "+quadAgent[agent[0][0]]+ " Quadrant = "+ (agent[0][0]+1));
	  	    		
	  					    		
	  					    		//1st quad clusters
	  					    		System.out.println("***clusters for 1st quad***");
	  					    		if(firstQuadId.size()>0) {
	  					    			Clustering.Cluster[] clusters1 = Clustering.kmeansClusters(firstQuadLoc,firstQuadId.size(),2,quadAgent[0]);
	  				
	  						    		
	  						    		for(int i=0;i<quadAgent[0];i++) {
	  						    			ArrayList<Integer> members = clusters1[i].getMembership();
	  						    			System.out.println("cluster- "+(i+1));
	  						    			for(int j=0; j<members.size();j++)
	  						    			{
	  						    				System.out.println("dealerId-"+ firstQuadId.get(members.get(j))); 
	  						    			}
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
	  					        			ArrayList<Integer> members = clusters2[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{
	  					        				System.out.println("dealerId-"+ secQuadId.get(members.get(j)));
	  					        				
	  					        			}
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
	  					        			ArrayList<Integer> members = clusters3[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{
	  					        				System.out.println("dealerId-"+ thirdQuadId.get(members.get(j)));
	  					        			}
	  					        			
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
	  					        			ArrayList<Integer> members = clusters4[i].getMembership();
	  					        			System.out.println("cluster- "+(i+1));
	  					        			for(int j=0; j<members.size();j++)
	  					        			{
	  					        				System.out.println("dealerId-"+ fourQuadId.get(members.get(j)) );
	  					        				
	  					        			}
	  					        			
	  					        			 System.out.println("AgentId- "+ agentList.get(agentIndex));
	  					        			 System.out.println();
	  					        			 agentIndex++;
	  					        		}
	  				
	  					    		}
	  					    		
	  					    		
//	  					    		for(int i=0;i<4;i++) {
//	  			    					
//	  			    					int runnersReq = (plateCount[i]/300);
//	  			    					runnersReq -- ;
//	  			    					
//	  			    					if(runnersReq * 300 < plateCount[i])
//	  			    							runnersReq++;
	  			    					
//	  			    					if(agentCount >= runnersReq)
//	  			    					{
//	  			    						quadAgent[i] += runnersReq;
//	  			    						agentCount -= runnersReq;
//	  			    					}
	  			    					
//	  			    					if(runnersReq < quadAgent[i]) {
//	  			    						checkCap = 1;
//	  			    						quadCap.add
//	  			    					}
//	  			    					else if(runnersReq > quadAgent[i]) {
//	  			    						extra ++;
//	  			    					}
//	  			    					
//	  			    						
//	  			    				}
//	  					    		
//	  					    		if(checkCap == 1 && extra > 0) {
//	  					    			
//	  					    		}
	  	    		
	  					    		break;
	  	    		
	    		}
	    		
	    		
	    }
	    
	 
	 
	 static void allotRunners(List<Long>dealList, List<Location>locList, String ec_code,Location ecLoc, Connection db) throws Exception {
		 
		List<String> agentList = new LinkedList<>();
		
		agentList = getAAgentList(ec_code, dealList, db);
		
		long agentCount = agentList.size();
		
		System.out.println("&&&&&&&&& Total Available Agents = "+ agentCount +"&&&&&&&&&\n&&&&&&&&&& Available Agents -");
		
		for(int i=0;i<agentList.size();i++)
			System.out.println(agentList.get(i));
		
		System.out.println();
		quadCluster(dealList, locList, ecLoc, agentList, db);
		
		
	 }
}
