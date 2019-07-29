import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.awt.List;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class MainClass {
	

	public static final int MAX_PROCESSING_TASKS=100;
	
	
	public static int getProcessingTime(int minTime,int maxTime)
	{
		int range=maxTime-minTime+1;
		return (int)(Math.random()*range)+minTime;
	}

	public static void removeFromToDoList(Connection connection,String taskId) throws SQLException
	{
		//removed from to-do-list
		PreparedStatement stmt = connection.prepareStatement("delete from task where taskId=?");
		stmt.setString(1, taskId);
		stmt.executeUpdate();
	}
	
	public static void addToProcessingList(Connection connection,String taskId,String custId) throws SQLException
	{
		PreparedStatement stmt=connection.prepareStatement("insert into processing values(?,?,?,?)");
		stmt.setString(1, taskId);
		//set time limit for task randomly between minTaskSeconds and maxTaskSeconds
		
		Statement selectStmt = (Statement) connection.createStatement();
		ResultSet rs1=selectStmt.executeQuery("select * from customer where custId='"+custId+"'");
		rs1.next();
		
		int minTime=rs1.getInt(2);
		int maxTime=rs1.getInt(3);
		
		int timeLimit=getProcessingTime(minTime,maxTime);
		
		Date date = new Date();
		Timestamp param = new java.sql.Timestamp(date.getTime());
		stmt.setObject(2,param);
		stmt.setObject(3,new Timestamp(date.getTime() + (timeLimit * 1000L)));
		stmt.setString(4, custId);
		stmt.execute();
	}
	
	public static void removeFromProcessingList(Connection connection,String task) throws SQLException
	{
		System.out.println("Ending.."+task);
		PreparedStatement stmt = connection.prepareStatement("delete from processing where taskID=?");
		stmt.setString(1, task);
		stmt.executeUpdate();

	}
	
	public static void addToDoList(Connection connection,String task,String customer) throws SQLException
	{
		PreparedStatement stmt=connection.prepareStatement("insert into task values(?,?,?)");
		stmt.setString(1, task);
		stmt.setString(2, customer);
		stmt.setTimestamp(3, new Timestamp(new Date().getTime()));
		stmt.execute();
	}
	
	public static ArrayList<String> getCustomers(Connection connection) throws SQLException
	{
		ArrayList<String> customers=new ArrayList<String>();
		
		Statement selectStmt = (Statement) connection.createStatement();
    	ResultSet rs = selectStmt.executeQuery("select distinct custId from task order by custId");

		while(rs.next()) {
    		customers.add(rs.getString(1));
    	}
    	
		return customers;
	}
	
	public static HashMap<String, Integer> sortTasksCountTable(HashMap<String, Integer> hm) 
    { 
        // Create a list from elements of HashMap 
        LinkedList<Entry<String, Integer>> list = 
               new LinkedList<Map.Entry<String, Integer> >(hm.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() { 
            public int compare(Map.Entry<String, Integer> o1,  
                               Map.Entry<String, Integer> o2) 
            { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>(); 
        for (Map.Entry<String, Integer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    } 
	
	public static Map<String, Integer> updateTasksCount(Connection connection) throws SQLException 
	{
		ArrayList<String> customers=getCustomers(connection);
		HashMap<String,Integer> map=new HashMap<String,Integer>();
		
		for (int counter = 0; counter < customers.size(); counter++) 
		{ 		      
	          map.put(customers.get(counter), 0); 		
	    }  
		
//		System.out.println("After initializing Customers Task Count table: "+Arrays.asList(map));
		
		Statement selectStmt = (Statement) connection.createStatement();
    	ResultSet rs = selectStmt.executeQuery("select custID,count(*) from processing group by custID");		
		
    	while(rs.next())
    	{
    		map.put(rs.getString(1), rs.getInt(2));
    	}
//		System.out.println("After Updating Customers Task Count table: "+Arrays.asList(map));
		
		Map<String, Integer> sorted=sortTasksCountTable(map);
		
		System.out.println("After Sorting Customers Task Count table: "+Arrays.asList(sorted));
		
		return sorted;
	}  
	
	
	public static void processTasks(Connection connection) throws SQLException, InterruptedException, IOException
	{
		int choice=1;
	
		while(true) {
			System.out.println("Choose an algorithm to pick tasks \n 1.FIFO \n 2.Round-Robin \n 3.Balanaced round-robin \n 4.Exit");
			Scanner sc=new Scanner(System.in);
			choice=sc.nextInt();
			
			if(choice==4)
				break;
			int custIndex=0;
			ArrayList<String> customers=new ArrayList<String>();
			
			
	    	Statement selectStmt = (Statement) connection.createStatement();
	    	ResultSet rs = selectStmt.executeQuery("select * from task");
	    	
	        int num_Processing_Tasks=0;
	        
	        boolean del=false;
	        
	        int available;
	        while((available = System.in.available()) == 0)
	    	{
	        	ResultSet rs1=null;
	        	
	        	if(num_Processing_Tasks!=MAX_PROCESSING_TASKS)
	        	{
	        		//Pick a task by FIFO (based on Insertion time)
	        		
	        		if(choice==1)
	        		{
	            		rs1=selectStmt.executeQuery("select * from task order by insertTime limit 1");
	            		rs1.next();
	        		}
	        		else if(choice==2)
	        		{
	        			customers=getCustomers(connection);
	        			PreparedStatement stmt;
	        			while(rs1==null)
	        			{
	        				stmt=connection.prepareStatement("select * from task where custId=? order by insertTime limit 1");
	            			String customer=customers.get(custIndex);
	            			stmt.setString(1, customer);
	            			rs1=stmt.executeQuery();
	            			custIndex=(custIndex+1)%customers.size();
	        			}
	        			rs1.next();
	        		}
	        		
	        		else if(choice==3)
	        		{
	        			Map<String, Integer> countsTable=updateTasksCount(connection);
	        			PreparedStatement stmt;
	        			int index=0;
	        			String customer="";
	        			
	        			while(true)
	        			{
	        				customer=(String) countsTable.keySet().toArray()[index];
	        				index=(index+1);
	        				stmt=connection.prepareStatement("select * from task where custId=? order by insertTime limit 1");
	            			stmt.setString(1, customer);
	            			rs1=stmt.executeQuery();
	            			if(rs1.next())
	            				break;
	        			}
	
	        		}
	        		
	        		String taskId=rs1.getString(1);
	        		String custId=rs1.getString(2);
	        		Timestamp dbSqlTimestamp = rs1.getTimestamp(3);
	        		
	        		//remove from to-do list
	        		
	        		removeFromToDoList(connection,taskId);
	        		
	        		//add to processing list
	        		
	        		addToProcessingList(connection,taskId,custId);
	        		
	        	}
	        	
	        	//Check for expired tasks in processing list
	        	
	        	rs1 = selectStmt.executeQuery("select * from processing order by startTime");
	        	System.out.println("\t\tProcessing List");
	        	while(rs1.next())
	    	    {
	        		String task,customer;
	        		Timestamp start,end;
	        		
	        		task=rs1.getString(1);
	        		start=rs1.getTimestamp(2);
	        		end=rs1.getTimestamp(3);
	        		customer=rs1.getString(4);
	        		
	        		Date date = new Date();
	        		Timestamp curTime = new Timestamp(date.getTime());
	        		
	        		if(curTime.after(end))
	        		{
	        			//Remove task from Processing list
	        			
	        			removeFromProcessingList(connection,task);
		        		
		        		//Adding task to Todo list (with current time as new insert time)
	        			
	        			addToDoList(connection,task,customer);
		        		del=true;
	        		}
	        		else
	        		{
	        			int timeLeft=(int)(end.getTime()-curTime.getTime())/1000;
	        			System.out.println(task+"\t"+start+"\t"+end+"\t"+customer+"\t"+timeLeft+" seconds left");
	        		}		
	    	    }
	        	
	        	// Display to-do list
	        	
	        	rs1 = selectStmt.executeQuery("select * from task order by insertTime");
	        	System.out.println("\t\tTo-do List");
	        	while(rs1.next()) 
	        	{
	        		System.out.println(rs1.getString(1)+"\t"+rs1.getString(2)+"\t"+rs1.getTimestamp(3));
	        	}
	        	
	        	// Display no. of tasks in processing list
	        	
	        	rs1=selectStmt.executeQuery("select count(taskID) from processing");
	        	rs1.next();
	        	num_Processing_Tasks=rs1.getInt(1);
	        	System.out.println("\nNo. of tasks processing:"+num_Processing_Tasks);
	        	
	        	// Display no. of tasks by Customer
	        	
	        	rs1=selectStmt.executeQuery("select custID,count(taskID) from processing group by custID");
	        	System.out.println("\nCustomer\tNo. of tasks");
	        	while(rs1.next())
	        	{
	        		System.out.println(rs1.getString(1)+"\t"+rs1.getInt(2));
	        	}
	        	
	        	System.out.println("-----------------------------------------------------");
	        	TimeUnit.SECONDS.sleep(1);
	    	}
			}
	}
	
	public static void connectJDBCToAWSEC2() throws SQLException, InterruptedException, IOException {

	    System.out.println("----MySQL JDBC Connection Testing -------");
	    
	    try {
	        Class.forName("com.mysql.jdbc.Driver");
	    } 
	    catch (ClassNotFoundException e) {
	        System.out.println("Where is your MySQL JDBC Driver?");
	        e.printStackTrace();
	        return;
	    }

	    System.out.println("MySQL JDBC Driver Registered!");
	    Connection connection = null;

	    try {
	        connection = (Connection) DriverManager.getConnection("jdbc:mysql://gmutest.ca1hr6mkhz7h.us-east-2.rds.amazonaws.com/gmutestdb?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "gmutestdb", "Janani_8");
	    } 
	    catch (SQLException e) {
	        System.out.println("Connection Failed!:\n" + e.getMessage());
	    }

	    if (connection != null) {
	    	
	    	processTasks(connection);
	        System.out.println("\nTasks scheduled successfully");	        
	    } 
	    else {
	        System.out.println("\nFAILURE! Failed to make connection!");
	    }

	}
	public static void main(String[] args) throws InterruptedException, IOException
	{
		try {
			connectJDBCToAWSEC2();
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}
}
