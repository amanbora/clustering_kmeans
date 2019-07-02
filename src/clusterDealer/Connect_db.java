package clusterDealer;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


public class Connect_db {
	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch (ClassNotFoundException e) {
	        throw new IllegalArgumentException("MySQL db driver is not on classpath");
	    }
	}

	
	public static Connection getConnection(String database, String username, String password) throws SQLException{
		Properties properties = new Properties();
		properties.setProperty("user", username);
		properties.setProperty("password", password);
		properties.setProperty("useSSL", "false");
		properties.setProperty("autoReconnect", "true");
		return DriverManager.getConnection("jdbc:mysql://172.25.37.56:3306/"+database,properties);
	}
}
