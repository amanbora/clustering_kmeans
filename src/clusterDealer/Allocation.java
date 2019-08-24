package clusterDealer;

import java.io.BufferedReader;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

//import weka.core.converters.ArffSaver;
//import weka.core.converters.CSVLoader;
//import weka.core.Attribute;
//import weka.filters.unsupervised.attribute.Remove;
//import weka.filters.Filter;
//import weka.classifiers.meta.FilteredClassifier;
//import weka.experiment.InstanceQuery;
//import weka.experiment.Tester;



public class Allocation  {
	
	
	private static String database = "HSRP_AllocationDB";
	private static String username = "root";
	private static String password = "Redhat@1234";
	private static final int NOT_ASSIGNED = 7;
	
	static Connection db;
	static DatabaseMetaData dbmd;
	
	
	
	class orderContainer {
		String  ec_code;
		long dealer_id,order_status_id;
	}
    
    class Location{
    	double latitude,longitude;
    }
    
    class clusterContainer{
    	String challanNo;
    	double latitude, longitude;
    }

    class dealerPendingOrder{
		long dealer_id = 0;
    	int plate_count = 0;
    	Date first_order_date_time;
    	Time order_ready_time;
    	int priority = 0;
    	Date updated_at;
    	int quadrant = NOT_ASSIGNED ;
    }
    
    
    
    
	
	
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
	
    
    
//    //count of active runners of EC whose orders are placed
//    static long getAAgentCount(String ec_code, Connection db) throws SQLException {
//    		
//    	String agentQuery = "select acode from paytmagententry where active_status=1 and AStatus = 'Y' "
//    			+ "and emp_type='OUT' and ROLE='MR'  and acode in "
//    			+ "(select userid from HSRP_runnerECMapping where ec_code = "+ec_code+ " )";
//    	Statement sql = db.createStatement();
//    	ResultSet countAgent = sql.executeQuery(agentQuery);
//    	ResultSetMetaData m = countAgent.getMetaData();
//    	return m.getColumnCount();
//    }
    
    
    

    // returns list of todays orders sorted according to priority
    static List<dealerPendingOrder> todayPendingOrdersSorted(List<dealerPendingOrder>dealList){
    	
    	List<dealerPendingOrder> todayOrders = new LinkedList<>();
    	
    	Date today = new java.util.Date();  //date .now();
    	
    	
    	for(int i=0;i<dealList.size();i++) {
    		if(dealList.get(i).first_order_date_time.compareTo(today) == 0)
    				todayOrders.add(dealList.get(i));
    	}
    	Collections.sort(todayOrders, new Comparator<dealerPendingOrder>() {
            @Override
			public int compare(dealerPendingOrder o1, dealerPendingOrder o2) {
                return o1.priority < o2.priority ? -1 : o1.priority == o2.priority ? 0 : 1;
            }
        });
    	
    	return todayOrders;
    }
    
    
    
    

    
    // returns list of previous day orders sorted according to priority
    static List<dealerPendingOrder> previousPendingOrdersSorted(List<dealerPendingOrder>dealList){
    	
    	List<dealerPendingOrder> prevOrders = new LinkedList<>();
    	
    	Date today = new java.util.Date();  //date .now();
    	
    	
    	for(int i=0;i<dealList.size();i++) {
    		if(dealList.get(i).first_order_date_time.compareTo(today) < 0)
    				prevOrders.add(dealList.get(i));
    	}
    	Collections.sort(prevOrders, new Comparator<dealerPendingOrder>() {
            @Override
			public int compare(dealerPendingOrder o1, dealerPendingOrder o2) {
                return o1.priority < o2.priority ? -1 : o1.priority == o2.priority ? 0 : 1;
            }
        });
    	
    	return prevOrders;
    }
    
    
    
     //list of dealers of a particular EC whose orders are placed
     static List<dealerPendingOrder> dealerList(String ec_code, Connection db) throws SQLException {

//    	 String dealerListQuery = 	" select distinct dealerId FROM  HSRP_dealer_master where IsAddressResolved = 1"
//    								+ " and (courier_delivery='n' or courier_delivery='N') "
//					    			+ " and ec_code= "+ ec_code + "and dealerId in " 
//					    			+ "(select dealer_ID from HSRP_order_detail where ec_code =" + ec_code 
//					    			+ " and order_status_id != 6)";
//    	
//    	
//    	Statement sql = db.createStatement();
//    	ResultSet dset;
//    	List <Long> dealList = new LinkedList<>();
//    	
//    	
//    	
//    	try {
//    		dset = sql.executeQuery(dealerListQuery);
//    		while(dset.next()){
//    			
//	    			long val = dset.getLong(1);
//	    			dealList.add(val);
//	    			locDealer(val,db);
//
//    			}
//    	}
//    	catch(SQLException e) {
//			e.printStackTrace();
//		}
//    	
//    	return dealList;
		
    	 String dealerListQuery = 	" select distinct dealer_id, priority, "
	    	 			+ " plate_count, order_ready_time, first_order_date_time, updated_at "
	    	 			+ " FROM  HSRP_dealer_pending_order"
	    	 			+ " where ec_code= "+ ec_code  ;
	
	
			Statement sql = db.createStatement();
			ResultSet dset;
			List <dealerPendingOrder> dealList = new LinkedList<>();
			
			
			
			try {
			dset = sql.executeQuery(dealerListQuery);
			while(dset.next()){
				Allocation dealerpendorderobj=new Allocation();
				
				dealerPendingOrder dp = dealerpendorderobj.new dealerPendingOrder();
				
				long val = dset.getLong(1);
				int pri = dset.getInt(2);
				int plate = dset.getInt(3);
				Time or = dset.getTime(4);
				Date d = dset.getDate(5);
				Date up = dset.getDate(6);

//				java.util.Date myDate=null;
//				java.sql.Time theTime = new java.sql.Time(dset.getTimestamp(4));
//				LocalTime or = (LocalTime)dset.getTimestamp(4);
				
				dp.dealer_id=val;
				dp.plate_count=plate;
				dp.priority=pri;
				dp.order_ready_time = or;
				dp.first_order_date_time = d;
				dp.updated_at = up;
				dealList.add(dp);
				locDealer(dp.dealer_id,db);
			
			}
			}
			catch(SQLException e) {
			e.printStackTrace();
			}
			
			return dealList;

    	
    }
     
    
     //gives longitude and latitude of dealers
     static Location locDealer(Long dealer_id, Connection db)throws SQLException {
    	 
    	
    	Statement sql = db.createStatement();
		String locQuery = "Select  geolat, geoLong From HSRP_dealer_master where dealerid ="+dealer_id;
 		
		ResultSet dealLoc;
		Allocation c1 = new Allocation();
		
		Location loc = c1.new Location();
		try {
			dealLoc = sql.executeQuery(locQuery);
			if(dealLoc != null) {
				 
				 while(dealLoc.next()) {
					 
				
					 loc.latitude = dealLoc.getDouble(1);
					 loc.longitude = dealLoc.getDouble(2);
				
				 }
				
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		
		return loc;
		
    }
     
     
     static List<Location> getLocList (List<dealerPendingOrder> dealerList, Connection db) throws SQLException{
    	 List<Location> locList = new LinkedList<>();
    	 for(dealerPendingOrder deal : dealerList)
    	 {
    		 locList.add(locDealer(deal.dealer_id, db));
    	 }
    	 return locList;
     }
    
     
     
     static Location getECLoc(String ec_code, Connection db) throws SQLException{
    	 String locQuery = "SELECT geo_lat, geo_long FROM HSRP_ec_master where ec_code ="+ec_code;
    	 Statement sql = db.createStatement();
    	 ResultSet ecLoc;
    	 Allocation a1 = new Allocation();
    	 Location ECLoc = a1.new Location();
    	 try {
    		 ecLoc=sql.executeQuery(locQuery);
    		 if(ecLoc!=null) {
    			 while(ecLoc.next()) {
    				 ECLoc.latitude = ecLoc.getDouble(1);
        			 ECLoc.longitude = ecLoc.getDouble(2);
    			 }
    			 
    			 
    		 }

    	 }
    	 catch(SQLException e){
    		 e.printStackTrace();
    	 }

    	 return ECLoc;
     }
     
     
    static List<String> getECList (Connection db)throws SQLException{
    	
    	List<String> ecList = new LinkedList<>();
    	
    	String ecQuery = "SELECT Distinct ec_code FROM HSRP_order_veh_reg_mapping where ec_code in" + 
    			"(select ec_code from HSRP_ec_master where isActive = 1)";
    	
    	String countDisEC = "select count(distinct(ec_code)) from  HSRP_order_veh_reg_mapping" ;
    	
    	Statement sql1 = db.createStatement();
    	Statement sql2 = db.createStatement();
    	
    	ResultSet rs1;
    	
    	rs1=sql1.executeQuery(countDisEC);
    	
    	if(rs1.first())
    		System.out.println("Total Distinct EC = "+ rs1.getInt(1)+"\n");
    			
    	try {
    			ResultSet rs2;
    			rs2 = sql2.executeQuery(ecQuery);
    			if(rs2 != null) {
    				while(rs2.next()) {
    					ecList.add(rs2.getString(1));
    				}
    			}
    			
    	}
    	catch(SQLException e) {
    		e.printStackTrace();
    	}
    	return ecList;
    }
    
    public static class TimerClass extends TimerTask{
    		
    		public void run()
    		{	
    			
    			
    			Allocation alot = new Allocation();
    			Location ecLoc = alot.new Location();
    			List < dealerPendingOrder > dL=new LinkedList<>();
    			List<dealerPendingOrder> previousOrders=new LinkedList<>() ;
    			List<dealerPendingOrder> todayOrders=new LinkedList<>();
    			List<Location> locListPrev=new LinkedList<>();
    			List<Location> locListToday=new LinkedList<>();
    			
    	    	try {
    	    		
    	    		// will be called for each EC in order list
    	    		
    	    		List<String> ecList = getECList(db);
    	    		
    	    		
    	    		for(int i=0; i<ecList.size(); i++) {
    	    			
    	    			System.out.println("$$$$$$$$$$$$$ EC-Code- "+ecList.get(i)+" $$$$$$$$$$$$$");
    	    			
    	    			ecLoc = getECLoc( "\'"+ecList.get(i)+"\'" , db );

    	    			dL = dealerList("\'"+ecList.get(i)+"\'", db);
    	    			
//    	    			List<Location> locList = getLocList(dL,db);
    	    			
    	    			System.out.println("eclat-"+ecLoc.latitude+" eclong-"+ecLoc.longitude+"\n");
    	    			
    	    			if(dL.size()==0) {
    	    				System.out.println("No Dealers");		
    	    			}
    					else {
    						
    	    				previousOrders = previousPendingOrdersSorted(dL);
        					todayOrders = todayPendingOrdersSorted(dL);
        	    			
        	    			
        	    			locListPrev= getLocList(previousOrders,db);		
        	    			locListToday= getLocList(todayOrders,db);
        	    			
        	    			DistributeRunner.allotRunners(previousOrders, locListPrev, "\'"+ecList.get(0)+"\'", ecLoc, db);
        	    			DistributeRunner.allotRunners(todayOrders, locListToday, "\'"+ecList.get(0)+"\'", ecLoc, db);
    	    			}
    	    			
    	    			System.out.println("------------------------------------");
    	    			System.out.println();
    	    		}

    	    			
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    		}
    }
    			
    
    
   			

//				ecLoc = getECLoc("'EC-0009'", db);
//				dL = dealerList(" 'EC-0009' ", db);
//				System.out.println("eclat-"+ecLoc.latitude+" eclong-"+ecLoc.longitude);
//    			try {
//
//    				DistributeRunner.allotRunners(previousOrders, locListPrev, "'EC-0009'", ecLoc, db);
//
//					// DistributeRunner.allotRunners(todayOrders, locListToday, "'EC-0009'", ecLoc, db);
//					
//					
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
    		
		
		
    
    
	
	
	public static void main(String[] args) throws Exception {
		
		//Database connection		
		
		try {
	    	 db=Connect_db.getConnection(database, username, password);
	    	 dbmd = db.getMetaData();
	    	
	    }catch(SQLException e) {
	    	e.printStackTrace();
	    }
		
		
		System.out.println("Connection to " + dbmd.getDatabaseProductName()
	    + " " + dbmd.getDatabaseProductVersion() + " successful.\n");
		
		
		String obtainQuery = "select distinct vr.dealer_id, dm.ec_code, dm.Priority,sum(plate_count) as 'plateCount',"
				+ " min(order_datetime) as 'firstOrderTime' " 
				+ " from HSRP_order_veh_reg_mapping vr " 
				+ " left join HSRP_order_detail od on vr.Challan_order_ID=od.order_ID "  
				+ " join HSRP_dealer_master dm on vr.dealer_id=dm.dealerId and courier_delivery='N' " 
				+ " where od.order_status_id not in (4,5,6) " 
				+ " group by dealer_id";
		
		Statement sql1 = db.createStatement();
		ResultSet rs = sql1.executeQuery(obtainQuery);
		
		long dealer_id = 0;
		int plate_count = 0;
		Date first_order_date_time = new Date();
		Time order_ready_time = null;
		int priority = 0;
		Date updated_at = new Date();
		String ec_code = null;
		
		if(rs!=null) {
			while(rs.next()) {
				dealer_id = rs.getLong(1);
				ec_code = rs.getString(2);
				priority = rs.getInt(3);
				plate_count = rs.getInt(4);
				first_order_date_time = rs.getDate(5);				
			}
		}
		

		String insertQuery = "INSERT INTO HSRP_dealer_pending_order" + 
				"( dealer_id, plate_count, first_order_date_time, " + 
				"order_ready_time, priority, updated_at, " + 
				"ec_code) VALUES (" + dealer_id + "," + plate_count + "," + 
				 first_order_date_time+ "," + order_ready_time + "," + 
				 priority + "," + updated_at + "," + ec_code + ")";
		
		
		db.prepareStatement(insertQuery);
		
//		List<String> ecList = getECList(db);

		
		 Date date = new Date(); 
		 date = Calendar.getInstance().getTime();
		
		 
		 //Now create the time and schedule it
		 Timer timer = new Timer();
		 
		 
		 //10 seconds
		 
		 long time_interval = 1000*10*60;
		
		 timer.schedule(new TimerClass(), date, time_interval);
		
		
	}
}
