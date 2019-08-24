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


    
    
    
	
	
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
	
    
    
    //count of active runners of EC whose orders are placed
    static long getAAgentCount(String ec_code, Connection db) throws SQLException {
    		
    	String agentQuery = "select acode from paytmagententry where active_status=1 and AStatus = 'Y' "
    			+ "and emp_type='OUT' and ROLE='MR'  and acode in "
    			+ "(select userid from HSRP_runnerECMapping where ec_code = "+ec_code+ " )";
    	Statement sql = db.createStatement();
    	ResultSet countAgent = sql.executeQuery(agentQuery);
    	ResultSetMetaData m = countAgent.getMetaData();
    	return m.getColumnCount();
    }
    
    
    
     //list of dealers of a particular EC whose orders are placed
     static List<Long> dealerList(String ec_code, Connection db) throws SQLException {
    	String dealerListQuery = 	" select distinct dealerId FROM  HSRP_dealer_master where IsAddressResolved = 1"
    								+ " and (courier_delivery='n' or courier_delivery='N') "
					    			+ " and ec_code= "+ ec_code + "and dealerId in " 
					    			+ "(select dealer_ID from HSRP_order_detail where ec_code =" + ec_code 
					    			+ " and order_status_id != 6)";
    	
    	
    	Statement sql = db.createStatement();
    	ResultSet dset;
    	List <Long> dealList = new LinkedList<>();
    	
    	
    	
    	try {
    		dset = sql.executeQuery(dealerListQuery);
    		while(dset.next()){
    			
	    			long val = dset.getLong(1);
	    			dealList.add(val);
	    			locDealer(val,db);

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
     
     
     static List<Location> getLocList (List<Long> dealerList, Connection db) throws SQLException{
    	 List<Location> locList = new LinkedList<>();
    	 for(Long deal : dealerList)
    	 {
    		 locList.add(locDealer(deal, db));
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
    
    
    
	
	public static void main(String[] args) throws Exception {
	
		//Database connection
		
		
		String database = "Test_HSRPCircuit";
		String username = "root";
		String password = "Redhat@1234";
		
		Connection db=Connect_db.getConnection(database, username, password);
		DatabaseMetaData dbmd = db.getMetaData();
		System.out.println("Connection to " + dbmd.getDatabaseProductName()
        + " " + dbmd.getDatabaseProductVersion() + " successful.\n");
		
		
		
		Allocation alot = new Allocation();
		Location ecLoc = alot.new Location();
		
//		List<String> ecList = getECList(db);
		
		
		
		
		
//		for(int i=0; i<ecList.size(); i++) {
//			
//			System.out.println("$$$$$$$$$$$$$ EC-Code- "+ecList.get(i)+" $$$$$$$$$$$$$");
//			
//			ecLoc = getECLoc( "\'"+ecList.get(i)+"\'" , db );
//
//			List < Long > dL = dealerList("\'"+ecList.get(i)+"\'", db);
//			
//			List<Location> locList = getLocList(dL,db);
//			
//			System.out.println("eclat-"+ecLoc.latitude+" eclong-"+ecLoc.longitude+"\n");
//			
//			if(dL.size()==0) {
//				System.out.println("No Dealers");
//				
//			}
//			else {
//				DistributeRunner.allotRunners(dL, locList, "\'"+ecList.get(0)+"\'", ecLoc, db);
//
//			}
//			
//			System.out.println("------------------------------------");
//			System.out.println();
//
//		}
		
		ecLoc = getECLoc("'EC-0009'", db);
		
		
		// will be called for each EC in order list
		List < Long > dL = dealerList(" 'EC-0009' ", db);

		List<Location> locList = getLocList(dL,db);
		
		
		System.out.println("eclat-"+ecLoc.latitude+" eclong-"+ecLoc.longitude);
		
		
		// this will be called for all the EC in the orders list
		DistributeRunner.allotRunners(dL, locList, "'EC-0009'", ecLoc, db);
		
		;

		
		
	}
}
