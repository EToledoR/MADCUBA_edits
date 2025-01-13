/**
 * $Id $
 *
 *              Copyright (c) 2006
 *              Sergio Martin Ruiz, Madrid, Spain
 */

package es.smr.slim;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.Double;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JOptionPane;

//import org.python.modules.synchronize;

import es.cab.madcuba.log.Log;
import es.cab.plugins.MADCUBA;
import es.cab.swing.gui.EditorInformationMADCUBA;
import es.smr.slim.utils.SlimConstants;
//import ij.IJ;

/**
 * @author  Sergio Martin Ruiz
 * 
 * Class to create and test the DB.
 * Functions are defined to insert each of the catalogs.
 */
public class DbHSQLDBCreate {

	//our connnection to the db - persist for life of program
	Connection conn;
	Connection conn_user;
	// Statement to send SQL commands to the database
    Statement st = null;  
    Statement st_user = null;  
    // Path name of the databases
    String db_name;
    String db_name_user;
    
    //String pathdb_root="/data/workspace_bck/datos/";
	String pathdb_root= "/home/eduardo/datos/dbMADCUBA/";
    String pathdb_slovas92 = pathdb_root+"SmallLovasOriginal.txt";
	String pathdb_slovas03 = pathdb_root+"SmallLovasOriginal_2003.txt";
	String pathdb_blovas = pathdb_root+"BigLovasOriginal.txt";
    String pathdb_JPL = pathdb_root+SlimConstants.CATALOG_JPL+"/";
    String pathdb_CDMS = pathdb_root+SlimConstants.CATALOG_CDMS+"/";
    /**
	 * Eduardo Toledo Sep 2024
	 * Changes to add the HITRAN catalog
	 */
    String pathdb_HITRAN = pathdb_root+SlimConstants.CATALOG_HITRAN+"/";
    /**
     * Eduardo Toledo Dec 2024
     * Changes to add the CDMS ortho para catalog
     */
    String pathdb_ORTPAR = pathdb_root+SlimConstants.CATALOG_CDMSOP+"/";
    String pathdb_LSD = pathdb_root+SlimConstants.CATALOG_LSD+"/";
    String pathdb_USER;
    //String user_db_tablename = SlimConstants.CATALOG_USER;
    
    boolean standaloneMode;
    boolean debugmode = true;
    //boolean debugmode = false;
    
    private org.apache.log4j.Logger _log;

    /**
     * Creates the Database
     * @param dbPath Full path and name of the database
     * @throws Exception If database cannot be connected
     */
    public DbHSQLDBCreate(String dbPath, boolean isStandalone) throws Exception {    
    	_log = Log.getInstance().logger;
        _log.info("db_file_name_prefix (lines db name) = " + dbPath);

        
        db_name = dbPath;
        db_name_user = dbPath+SlimConstants.CATALOG_USER.toLowerCase();
        standaloneMode = isStandalone;

        // Load the HSQL Database Engine JDBC driver
	    // hsqldb.jar should be in the class path or made part of the current jar
	    Class.forName("org.hsqldb.jdbcDriver");

	    // IF not in standalone mode, it connects to both.
	    // This is to be used by MADCUBA
	    if(!isStandalone){
	    	connect(false);
	    	connect(true);
	    }
    }

    /**
     * Creator just with full path and defaults to not standalone
     * @param dbPath Full path
     * @throws Exception If database cannot be connected
     */
    public DbHSQLDBCreate(String dbPath) throws Exception {    
    	this(dbPath, false);
	}
    
    /**
     * Redirect messages to:
     * - System.out if standalone
     * - EditorInformationMADCUBA if connected to MADCUBA
     * @param message Message to display
     * @param isError Different behaviour in case it is declared an error
     * @param silent Only for debugging purposes. Only to be shown in standalone mode as System.out.print
     */
    private void printOutput(String message, boolean isError, boolean silent){
    	if (standaloneMode | debugmode)
    		if (isError)
    			System.out.println("ERROR: " + message);
    		else
    			System.out.println(message);
    	else
    		if (!silent)
	    		if (isError)
	    			EditorInformationMADCUBA.appendLineError("ERROR: " + message);
	    		else
	    			EditorInformationMADCUBA.append(message);    		
    }

	/**
     * Connects to database
     */
    private void connect(boolean isUserDB) throws SQLException{ 
	    // connect to the database.   This will load the db files and start the
	    // database if it is not already running.
	    // db_file_name_prefix is used to open or create files that hold the state
	    // of the db.
	    // It can contain directory names relative to the
	    // current working directory
    	
    	if(isUserDB){
        	//CONNECTING TO USER DATABASE
        	printOutput("connecting to database: " + db_name_user,false,true);
    	    _log.info("connecting to database: " + db_name_user);
    	    try {
    	    	//System.out.println("Connect to: "+ db_name_user);
    	    	conn_user = DriverManager.getConnection("jdbc:hsqldb:file:" + db_name_user);
    	    } catch (SQLException e) {
            	printOutput("connecting to database: " + db_name_user,true,true);
    	    	_log.error(" connection to DB failed due to: DriverManager.getConnection failure");
    	    	if(e.getCause()!=null)
    	    		_log.error(" details: cause" + e.getCause().toString());
    	        _log.error(" details: error code" + e.getErrorCode());
    	        _log.error(" exception trace follows: ");
    	        e.printStackTrace();

        	    _log.error("* Not found any lineuser.data");
    	        return;
    	    }
    	    st_user = conn_user.createStatement();
    	    _log.info("* DONE");
    	}
    	else{
	    	//CONNECTING TO MAIN DATABASE
	    	printOutput("connecting to database: " + db_name,false,true);
		    _log.info("connecting to database: " + db_name);
		    try {
		    	//System.out.println("Connect to: "+ db_name);
		    	conn = DriverManager.getConnection("jdbc:hsqldb:file:" + db_name);
		    } catch (SQLException e) {
            	printOutput("connecting to database: " + db_name,true,true);
		        _log.error(" connection to DB failed due to: DriverManager.getConnection failure");
		        _log.error(" details: cause" + e.getCause().toString());
		        _log.error(" details: error code" + e.getErrorCode());
		        _log.error(" exception trace follows: ");
		        e.printStackTrace();
		    }
		    if(conn!=null)
		    	st = conn.createStatement();
		    _log.info("* DONE");
    	}
	    
    }
    
    /**
     * Disconnects from database
     */
    private void disconnect(boolean isUserDB) throws SQLException{
    	if(isUserDB){
            _log.info("DISCONNECTING FROM USER DATABASE");
            if(conn_user!=null)
            	conn_user.close();
            if(st_user!=null)
            	st_user.close();
            _log.info("* DONE");
    	}
    	else{
	        _log.info("DISCONNECTING FROM MAIN DATABASE");
            if(conn!=null)
            	conn.close();
            if(st!=null)
            	st.close();
	        _log.info("* DONE");
    	}
    }
    
     /**
     * Closes and compact the database
     * @throws SQLException
     */
    private void compact(boolean isUserDB) throws SQLException {
        //Statement st = conn.createStatement();
    	if (isUserDB){
    		printOutput("CLOSING AND COMPACTING USER DATABASE", false, true);
            _log.info("CLOSING AND COMPACTING USER DATABASE");
            if(st_user!=null)
            	st_user.execute("SHUTDOWN COMPACT");
            else
            	return;
    	}
    	else{
    		printOutput("CLOSING AND COMPACTING MAIN DATABASE", false, true);
            _log.info("CLOSING AND COMPACTING MAIN DATABASE");
            st.execute("SHUTDOWN COMPACT");
    	}
        _log.info("* DONE");
        disconnect(isUserDB);
        connect(isUserDB);
    }

    /**
     * Closes the database and set it to readonly
     * @throws SQLException
     */
    private void shutdown(boolean isUserDB) throws SQLException {
        //Statement st = conn.createStatement();
        // db writes out to files and performs clean shuts down
        // otherwise there will be an unclean shutdown
        // when program ends
    	//TODO readonly
        //_log.info("SETTING READ-ONLY DATABASE");
        //st.execute("SET PROPERTY \"readonly\" true");
    	if(isUserDB){
            _log.info("CLOSING USER DATABASE");
            if(st_user!=null)
            	st_user.execute("SHUTDOWN");
            _log.info("* DONE");
    	}
    	else{
    		_log.info("CLOSING MAIN DATABASE");
    		if(st!=null) //SOLO TIENE QUE APAGARLA SI ESTA CONECTADO
    			st.execute("SHUTDOWN");
            _log.info("* DONE");
    	}
        disconnect(isUserDB);
    }


    /**
     * Set db.properties readonly parameter
     * 
     */
    private void setreadonly(boolean isUserDB, boolean readonlydb) throws IOException {
        try {
        	String dbname;
    		if(isUserDB)
    			dbname = db_name_user;
    		else
    			dbname = db_name;
            // input the file content to the StringBuffer "input"
           	printOutput(dbname + ".properties - original",false,true);
            BufferedReader file = new BufferedReader(new FileReader(dbname + ".properties"));
            String line;
            StringBuffer inputBuffer = new StringBuffer();

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
            String inputStr = inputBuffer.toString();

            file.close();
            deleteDb(isUserDB, true, false);

            printOutput(inputStr,false,true); // check that it's inputted right

            // this if structure determines whether or not to replace "0" or "1"
            if (readonlydb) {
                inputStr = inputStr.replace("readonly=false", "readonly=true"); 
            }
            else {
                inputStr = inputStr.replace("readonly=true", "readonly=false");
            } 
            // TODO is this actually important to set modified to no?
            inputStr = inputStr.replace("modified=yes", "modified=no");

            // check if the new input is right
            printOutput("----------------------------------",false,true);
            // write the new String with the replaced line OVER the same file
            printOutput(dbname + ".properties",false,true);
            printOutput(inputStr,false,true);
            FileOutputStream fileOut = new FileOutputStream(dbname + ".properties");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

        } catch (Exception e) {
//        	e.printStackTrace();
            Log.getInstance().logger.error("Problem reading file.");
        }
    }    	
    
	
    /**
     * Updates database
     * Used for SQL commands CREATE, DROP, INSERT and UPDATE
     * @param expression SQL command
     * @throws SQLException
     */
    private synchronized void update(String expression, boolean isUserDB) throws SQLException {
    	
        //st = conn.createStatement();    // statements
    	int i;
    	if (isUserDB)
    		if(st_user!=null)
    			i = st_user.executeUpdate(expression);    // run the query
    		else
    			return;
    	else
    		i = st.executeUpdate(expression);    // run the query
        if (i == -1) {
            _log.info("db error : " + expression);
        }
        //st.close();
    }    // void update()

    /**
     * Queries the database
     * @param expression SQL query command 
     */
    public synchronized void queryPrint(boolean isUserDB, String expression) throws SQLException {
        dump(query(isUserDB, expression));
        //st.close();    
    }

    public synchronized ResultSet query(boolean isUserDB, String expression) throws SQLException {

    	_log.info(expression);
    	// Eduardo Toledo Dec 2024
    	// Debugging wrong queries 
    	new Exception("Debug Stack Trace").printStackTrace();
    	//System.out.println(isUserDB+"---"+expression);
    	printOutput("USERDB:"+isUserDB+"---"+expression,false,true);
//    	if(isUserDB && st_user==null)
//    		return null;
        ResultSet rs = null;

        //TODO st = conn.createStatement();        // statement objects can be reused with
        									// repeated calls to execute but we
        									// choose to make a new one each time
        long time = new Date().getTime();
        if(isUserDB){
        	//dump(st_user.executeQuery(expression));
        	try {
        		rs = st_user.executeQuery(expression);    // run the query in USER DB
        	}catch (Exception e) {
				// TODO: handle exception
        //		e.printStackTrace();
			}
        }
        else{
        	// Eduardo Toledo Dec 2024
        	Log.getInstance().logger.debug("test");
        	//dump(st.executeQuery(expression));
        	Log.getInstance().logger.debug(expression);

        	try {
        		rs = st.executeQuery(expression);    // run the query in MAIN DB
        	}catch (Exception e) {
				// TODO: handle exception
        		//e.printStackTrace();
			}
        }
        time = new Date().getTime()-time;
        _log.info("Search Time=" + time);
        //TODO st.close(); // this is new as of 2017-05...to ve tested. Do we really need to create/close all the time?
        // do something with the result set.
        //dump(rs);
        return rs;
        //st.close();  TODO to be checked again!!!  
        // NOTE!! if you close a statement the associated ResultSet is
        // closed too
        // so you should copy the contents to some other object.
        // the result set is invalidated also  if you recycle an Statement
        // and try to execute some other query before the result set has been
        // completely examined.
    }
    
    public synchronized ResultSet query(String expression) throws SQLException {
    	if(expression.contains(SlimConstants.CATALOG_USER))
    		return query(true, expression);
    	else
    		return query(false, expression);
    }

    private void dump(ResultSet rs) throws SQLException {

        // the order of the rows in a cursor
        // are implementation dependent unless you use the SQL ORDER statement
        ResultSetMetaData meta   = rs.getMetaData();
        int               colmax = meta.getColumnCount();
        int               i;
        Object            o = null;
        int  rowcounter = 0;

        // the result set is a cursor into the data.  You can only
        // point to one row at a time
        // assume we are pointing to BEFORE the first row
        // rs.next() points to next row and returns true
        // or false if there is no next row, which breaks the loop
        for (; rs.next(); ) {
        	rowcounter++;
            for (i = 0; i < colmax; ++i) {
                o = rs.getObject(i + 1);  
                // In SQL the first column is indexed
                // with 1 not 0
                if (!rs.wasNull())
                	System.out.print(o.toString() + " ");
            }
            //System.out.println("");
            _log.info(" ");
        }
        System.out.println();
        //i=rs.getRow()-1;
        _log.info("FOUND = " + rowcounter);
    }                                       //void dump( ResultSet rs )



    
    /**
     * First Deletes the existing DB
     * Then creates a Table summary with the following fields.
     * - Name of the Table in DB
     * - Name as will be shown to the User
     * - Type of Spectral information that the table contains
     */
	private void createSchema(boolean isUserDB) throws IOException {
				//DeleteDb();
		        try {
		    		// Increase the maximum size in disk.
		            //st = conn.createStatement();
		            _log.info("SETTING hsqldb.cache_file_scale=8");	
		            if(isUserDB && st_user!=null){
			            st_user.execute("SET PROPERTY \"hsqldb.cache_file_scale\" 8");
			            st_user.execute( "CHECKPOINT" );
		            }
		            else if(!isUserDB){
			            st.execute("SET PROPERTY \"hsqldb.cache_file_scale\" 8");
			            st.execute( "CHECKPOINT" );
		            }
			        _log.info("* Generating Table Summary");
		        	update("DROP TABLE TableIndex IF EXISTS", isUserDB);
		        	update("CREATE CACHED TABLE TableIndex (id_tablename CHAR(15) NOT NULL, id_tablelongname CHAR(30) NOT NULL, id_tabletype CHAR(15) NOT NULL)", isUserDB);
		        } catch (SQLException ex3) {
		        	ex3.printStackTrace();
		        }
	}
	
    /**
     * Deletes Database lines.db if exists
     * Aimed to restart DB 
     */
	private boolean deleteDb(boolean isUserDB, boolean onlyProperties, boolean askConfirm){
		boolean dbDeleted = false;
		File database;
		String dbname;
		if(isUserDB)
			dbname = db_name_user;
		else
			dbname = db_name;
		if(onlyProperties)
			database = new File(dbname + ".properties");
		else
			database = new File(dbname + ".data");
		if (database.exists())
		{
	        printOutput("TO DELETE\n"+database.getAbsolutePath(),false,false);
			int dialogResult = JOptionPane.NO_OPTION;
			if (!onlyProperties) {
				if(askConfirm)
					dialogResult = JOptionPane.showConfirmDialog (null, "Database \""+dbname+ "\" exists.\nProceed to Overwrite?","Warning",JOptionPane.YES_NO_OPTION);
				else 
					dialogResult = JOptionPane.YES_OPTION;
				if(dialogResult == JOptionPane.YES_OPTION){	
					// No need to shutdown, because we are not connected
					// And it is going to be deleted anyway
					//try {
					//	shutdown(isUserDB);
					//} catch (SQLException e) {
						// TODO Auto-generated catch block
					//	e.printStackTrace();
					//}
					printOutput("DELETING EXISTING DATABASE",false,false);
			        database = new File(dbname + ".script");
					database.delete();
					printOutput("     " + database.getAbsolutePath() + " DELETED",false,false);
			        database = new File(dbname + ".backup");
					database.delete();
					printOutput("     " + database.getAbsolutePath() + " DELETED",false,false);
			        database = new File(dbname + ".data");
					database.delete();
					printOutput("     " + database.getAbsolutePath() + " DELETED",false,false);
				}
				else
					dbDeleted = false;
			}
			if (onlyProperties || dialogResult == JOptionPane.YES_OPTION) {
		        database = new File(dbname + ".properties");
				database.delete();
				printOutput("     " + database.getAbsolutePath() + " DELETED",false,false);
		        dbDeleted = true;
		        //try{
		        	//connect();
		        	//} catch (SQLException e) {
		        		// TODO Auto-generated catch block
		        	//	e.printStackTrace();
		        	//} 
			}
		}
		// If it does not exist, it is considered deleted
		else {
	        printOutput("DATABASE \n"+database.getAbsolutePath()+"\n DOES NOT EXIST.",false,false);
			dbDeleted = true;
		}
		return dbDeleted;
	}
	
    /**
     * Add Lovas 1992 catalog to DB
     */
	private void slovas92() throws IOException {
		String pathdb;
		String dbreadline;
        printOutput("START: SmallLovas92", false, true);
		try {
			try{
//		    /**
				//TODO
//		     *  Check if Table SmallLovas92 is created and has the correct # of entries
//		     *  If correctly created, exit function. 
//		     */
	    	//ESTA COMPROBACION NO FUNCIONA!!!!
		    //Deberia haber 4576 elementos, pero falta uno ?!?!?!
//		    if (db.get_table("SELECT * FROM  SmallLovas92").nrows==(int)4575)
//		    	{
//		    	System.out.println("*Table was correctly created. Keeping existing table");
//		    	return;
//		    	}
//		    else
//		    	{
	    	_log.info("* Insert SmallLovas92 into TableIndex");
			update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( 'SmallLovas92' , 'Lovas 1992' , 'Molecular' )",false);
	    	_log.info("* Generating table for the catalog: Small Lovas 1992");
	    	update("DROP TABLE SmallLovas92 IF EXISTS",false);
	        _log.info("TABLE SmallLovas92 DELETED");
	    	update("CREATE CACHED TABLE SmallLovas92 (" +
	    			"id_frequency DOUBLE NOT NULL , " +
	    			"id_frequencyuncert REAL , " +
	    			"id_formula CHAR(15) NOT NULL, " +
	    			"id_transition CHAR(30) )",
	    			false);
	    	
	    	/**
	    	 * Reads the file SmallLovasOriginal.txt
	    	 * Extracts the values modifying them as required replacing 
	    	 * selected characters.
	    	 * INSERT the values in DataBase
	    	 */
	    	pathdb = pathdb_slovas92; //"/home/smartin/workspace/datos/SmallLovasOriginal.txt";
	    	String frequency, frequencyuncert, formula, transition;
	    	String sqlcommand, sqlcommand1, sqlcommand2;
	    	char auxchar;
	    	BufferedReader input = new BufferedReader(new FileReader(pathdb));
	    	while((dbreadline = input.readLine())!= null)
	    		if(dbreadline.length()>=90){
	    			frequency = dbreadline.substring(2,13).replace((String)"*",(String)" ").replace((String)"?",(String)" ").replace((String)"(",(String)" ").replace((String)",",(String)".").replace((String)". ",(String)".0");
	    			frequencyuncert = dbreadline.substring(14,18).replace((String)"*",(String)" ").replace((String)"(",(String)"").replace((String)")",(String)"").replace((String)" ",(String)"");
	    			formula = dbreadline.substring(20,33).replace((String)" ",(String)"");
	    			transition = dbreadline.substring(33,62).replace((String)" ",(String)"");
	    			auxchar = frequency.substring(0,1).toCharArray()[0];
	    			if(auxchar>='1' && auxchar<='9'){
	    				sqlcommand1 = "INSERT INTO SmallLovas92 (id_frequency, ";
	    				sqlcommand2 = "VALUES (" + frequency + ",";
	    				if (!frequencyuncert.equals("")){
	    					sqlcommand1 = sqlcommand1 + "id_frequencyuncert, ";
	    					sqlcommand2 = sqlcommand2 + frequencyuncert + "," ;
	    				}
	    				sqlcommand1 = sqlcommand1 + "id_formula";
	    				sqlcommand2 = sqlcommand2 + "'" + formula + "'"; 
	    				if (!transition.equals("                             ")){
	    					sqlcommand1 = sqlcommand1 + ", id_transition"; 
	    					sqlcommand2 = sqlcommand2 + ",'" + transition.replace("'","*") + "'";
	    				}
	    				sqlcommand1 = sqlcommand1 + ") " ;
	    				sqlcommand2 = sqlcommand2 + ") " ;
	    				sqlcommand = sqlcommand1 + sqlcommand2 ;
	    				update(sqlcommand, false);
	    			}
	    		}
	    	input.close();
			_log.info("*Done");
	        _log.info("Creating INDEX Tables");
	        _log.info("id_frequency INDEX:");
	        update("CREATE INDEX SmallLovas92indexFREQ ON SmallLovas92 (id_frequency)", false);
	        printOutput("*Done : SmallLovas92 catalog added", false, true);
	        _log.info("*Done : SmallLovas92 catalog added");
	    	} catch (SQLException ex3) {
	        	ex3.printStackTrace();
	    	}
	    	
		} catch (IOException e) {
			e.printStackTrace();
		}
}

    /**
     * Add Lovas 2003 catalog to DB
     */
	private void slovas03() throws IOException {
		String pathdb;
		String dbreadline;

	    printOutput("START: SmallLovas03", false, true);
		try {
			try{
	    	_log.info("* Insert SmallLovas03 into TableIndex");
        	update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( 'SmallLovas03' , 'Lovas 2003' , 'Molecular' )",false);	    	
	        _log.info("*Generating table for the catalog: Small Lovas 2003");
	    	update("DROP TABLE SmallLovas03 IF EXISTS",false);
	        _log.info("TABLE SmallLovas03 DELETED");
            update("CREATE CACHED TABLE SmallLovas03 (" +
            		"id_frequency DOUBLE NOT NULL , " +
            		"id_frequencyuncert REAL , " +
            		"id_formula CHAR(15) NOT NULL, " +
            		"id_transition CHAR(30) )",
            		false);
			
	    	/**
	    	 * Reads the file SmallLovasOriginal2003.txt
	    	 * Extracts the values modifying them as required replacing 
	    	 * selected characters.
	    	 * INSERT the values in DataBase
	    	 */
			pathdb = pathdb_slovas03; //"/home/smartin/workspace/datos/SmallLovasOriginal_2003.txt";
			String frequency, frequencyuncert, formula, transition;
			String sqlcommand, sqlcommand1, sqlcommand2;
			char auxchar;
			BufferedReader input = new BufferedReader(new FileReader(pathdb));
			while((dbreadline = input.readLine())!= null)
				if(dbreadline.length()>=90){
					frequency = dbreadline.substring(1,13).replace((String)"*",(String)" ").replace((String)"?",(String)" ").replace((String)"(",(String)" ").replace((String)". ",(String)".0").replace((String)",",(String)".").replace((String)" ",(String)"");
					frequencyuncert = dbreadline.substring(13,17).replace((String)"*",(String)" ").replace((String)"(",(String)"").replace((String)")",(String)"").replace((String)" ",(String)"");
					formula = dbreadline.substring(18,31).replace((String)" ",(String)"");
					transition = dbreadline.substring(31,60).replace((String)" ",(String)"");
					auxchar = frequency.substring(0,1).toCharArray()[0];
					if(auxchar>='1' && auxchar<='9'){
						sqlcommand1 = "INSERT INTO SmallLovas03 (id_frequency, ";
						sqlcommand2 = "VALUES (" + frequency + ",";
						if (!frequencyuncert.equals("")){
							sqlcommand1 = sqlcommand1 + "id_frequencyuncert, ";
							sqlcommand2 = sqlcommand2 + frequencyuncert + "," ;
						}
						sqlcommand1 = sqlcommand1 + "id_formula";
						sqlcommand2 = sqlcommand2 + "'" + formula + "'"; 
						if (!transition.equals("                             ")){
							sqlcommand1 = sqlcommand1 + ", id_transition"; 
							sqlcommand2 = sqlcommand2 + ",'" + transition + "'";
						}
						sqlcommand1 = sqlcommand1 + ") " ;
						sqlcommand2 = sqlcommand2 + ") " ;
						sqlcommand = sqlcommand1 + sqlcommand2 ;
						update(sqlcommand,false);
					}
				}
			input.close();
			_log.info("*Done");
	        _log.info("Creating INDEX Tables");
	        _log.info("id_frequency INDEX:");
	        update("CREATE INDEX SmallLovas03indexFREQ ON SmallLovas03 (id_frequency)",false);
	        printOutput("*Done : SmallLovas03 catalog added", false, true);
	        _log.info("*Done : SmallLovas03 catalog added");
	    } catch (SQLException ex3) {
        	ex3.printStackTrace();
		}
	 	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
     * Add BIG Lovas 1992 catalog to DB
     */
	private void blovas() throws IOException {
		String pathdb;
		String dbreadline;

		printOutput("START: BigLovas", false, true);
		try {
			try{
	    	_log.info("* Insert BigLovas into TableIndex");
	    	update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( 'BigLovas'     , 'Big  Lovas' , 'Molecular' )",false);	    	
	        _log.info("Generating table for the catalog: Big Lovas");
	    	update("DROP TABLE BigLovas IF EXISTS",false);
	        _log.info("TABLE BigLovas DELETED");
            update("CREATE CACHED TABLE BigLovas (" +
            		"id_frequency DOUBLE NOT NULL , " +
            		"id_formula CHAR(15) NOT NULL, " +
            		"id_transition CHAR(30), " +
            		"id_energy DOUBLE NOT NULL, " +
            		"id_aij DOUBLE )",
            		false);
			
	    	/**
	    	 * Reads the file BigLovasOriginal.txt
	    	 * Extracts the values modifying them as required replacing 
	    	 * selected characters.
	    	 * INSERT the values in DataBase
	    	 */
			pathdb = pathdb_blovas; //"/home/smartin/workspace/datos/BigLovasOriginal.txt";
			String frequency, formula, energy, transition, aij;
			String sqlcommand, sqlcommand1, sqlcommand2;
			char auxchar;
			BufferedReader input = new BufferedReader(new FileReader(pathdb));
			while((dbreadline = input.readLine())!= null)
				if(dbreadline.length()>=66){
					frequency = dbreadline.substring(2,12).replace((String)",",(String)".").replace((String)" ",(String)"");
					formula = dbreadline.substring(13,25).replace((String)" ",(String)"");
					energy = dbreadline.substring(25,34).replace((String)" ",(String)"");
					transition = dbreadline.substring(35,60).replace((String)" ",(String)"");
					aij = dbreadline.substring(61,69).replace((String)" ",(String)"");
					auxchar = frequency.substring(0,1).toCharArray()[0];
					if(auxchar>='0' && auxchar<='9'){
						sqlcommand1 = "INSERT INTO BigLovas (id_frequency, id_formula, id_transition, id_energy";
						sqlcommand2 = "VALUES (" + frequency + ", '" + formula + "', '" + transition + "'," + energy ;
						if (!aij.equals("1.0")){
							sqlcommand1 = sqlcommand1 + ", id_aij"; 
							sqlcommand2 = sqlcommand2 + "," + aij ;
						}
						sqlcommand1 = sqlcommand1 + ") " ;
						sqlcommand2 = sqlcommand2 + ") " ;
						sqlcommand = sqlcommand1 + sqlcommand2 ;
						update(sqlcommand,false);
					}
				}
			input.close();
			_log.info("*Done");
	        _log.info("Creating INDEX Tables");
	        _log.info("id_frequency INDEX:");
	        update("CREATE INDEX BigLovasindexFREQ ON BigLovas (id_frequency)",false);
	        printOutput("*Done : BigLovas catalog added", false, true);
	        _log.info("*Done : BigLovas catalog added");
	    } catch (SQLException ex3) {
        	ex3.printStackTrace();
		}
	 	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Add JPL catalog to DB
     */
	@Deprecated
	private void jpl() throws IOException {
		String pathdb, pathfile;
		String dbreadline, dbjplreadline;

		printOutput("START: JPL", false, true);
		try {
			try{
				/**
				 * First checks that for each molecule ID in catdir.cat: 
				 * - It exists a file c[ID].cat
				 * - The molecule ID in catdir.cat matches that in the c[ID].cat
				 * 
				 * If any inconsistency is found, exits program.
				 */
		        _log.info("* Checking JPL catalog files");
		        pathdb = pathdb_JPL;
		        BufferedReader input = new BufferedReader(new FileReader(pathdb + "catdir.cat"));
		        BufferedReader inputcats;
		        String formulatag;
				while((dbreadline = input.readLine())!= null)
				{
				/**
				 *  files are located in JPL/catalog/
				 */ 
				pathfile = pathdb + "catalog/c" + dbreadline.substring(0,6).replace(" ","0") + ".cat";
				File catjplfiles = new File(pathfile);
			      if(!catjplfiles.exists()) {
			    	  //IF it does not exist, ERROR, inconsistenc catdir.cat with the existing files 
			          _log.info("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK");
			          System.exit(1);
			      }
			      else
				      //IF it exist, we check the ID of the molecule in the file matches that of catdir.cat
			      {
						pathfile = pathdb + "catalog/c" +dbreadline.substring(0,6).replace(" ","0") + ".cat";
						inputcats = new BufferedReader(new FileReader(pathfile));
						dbjplreadline = inputcats.readLine();
						if(dbjplreadline==null)
						{
							System.out.println(" ERROR: File " + pathfile + "  likely empty");
							System.out.println(" Read line : " + dbjplreadline );
							System.exit(1);
						}
						if(!dbjplreadline.substring(45,51).replace("-","0").replace(" ","0").equals(dbreadline.substring(0,6).replace(" ","0")))
						{
							_log.info("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
							"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK");
					          System.exit(1);
						}
			      }
				}
	            input.close();
		        _log.info("* Checked successfully");
		    	_log.info("* Insert JPL into TableIndex");
		        update("DELETE FROM TableIndex WHERE  id_tablename='JPL'",false);
	        	update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( 'JPL'          , 'JPL       ' , 'Molecular' )",false);
		        _log.info("Generating table for the catalog: JPL");
		        /**
		         * Creates the JPLcat table where the Molecules are stored with 
		         * the Partition function at different temperatures.
		         */
		    	update("DROP TABLE JPLcat IF EXISTS",false);
		        _log.info("TABLE JPLcat DELETED");
	            update("CREATE CACHED TABLE JPLcat (" +
	            		"id_TAG INTEGER PRIMARY KEY, " +
	            		"id_formula CHAR(13) NOT NULL, " +
	            		"id_QNFMT SMALLINT NOT NULL, " +
	            		"id_QLOG300 DOUBLE NOT NULL, " +
	            		"id_QLOG225 DOUBLE NOT NULL, " +
	            		"id_QLOG150 DOUBLE NOT NULL, " +
	            		"id_QLOG75 DOUBLE NOT NULL, " +
	            		"id_QLOG37 DOUBLE NOT NULL," +
	            		"id_QLOG18 DOUBLE NOT NULL, " +
	            		"id_QLOG9  DOUBLE NOT NULL)",
	            		false);
		        /**
		         * Creates the JPL table with the spectroscopic information
		         * for each molecular transition 
		         */
	            // BORRAR FORMULA CUANDO SE HAGA UNA BUSQUEDA EN LAS DOS TABLAS
		    	update("DROP TABLE JPL IF EXISTS",false);
		        _log.info("TABLE JPL DELETED");
	            update("CREATE CACHED TABLE JPL (" +
	            		"id_TAG INTEGER NOT NULL, " +
	            		//"id_formula CHAR(13) NOT NULL, " +
	            		"id_frequency DOUBLE NOT NULL, " +
	            		"id_ERR  DOUBLE NOT NULL, " +
	            		"id_LGINT DOUBLE NOT NULL, " +
	            		"id_DR TINYINT NOT NULL, " +
	            		"id_ELO DOUBLE NOT NULL, " +
	            		"id_GUP SMALLINT NOT NULL," +
	            		//"id_QNFMT INTEGER NOT NULL, " +
	            		"id_QN1  SMALLINT NOT NULL, " +
	            		"id_QN2  SMALLINT, " +
	            		"id_QN3  SMALLINT, " +
	            		"id_QN4  SMALLINT, " +
	            		"id_QN5  SMALLINT, " +
	            		"id_QN6  SMALLINT, " +
	            		"id_QNN1  SMALLINT NOT NULL, " +
	            		"id_QNN2  SMALLINT, " +
	            		"id_QNN3  SMALLINT, " +
	            		"id_QNN4  SMALLINT, " +
	            		"id_QNN5  SMALLINT, " +
	            		"id_QNN6  SMALLINT)",
	            		false);
           
	            /**
	             * Adds each of the JPL catalogs adding the corresponging entry in 
	             * Table JPLcat (molecule and partition functions)
	             */
	            input = new BufferedReader(new FileReader(pathdb + "catdir.cat"));
		        String formula;
		        String q[] = new String[7]; // Array for the partition functions
		        String freq=" ", err=" ", lgint=" ", dr=" ", elo=" ", qnfmt=" ";
		        int gup;
		        String qn[] = new String[12]; // Array for the quantum numbers
		        printOutput("Adding individual JPL Catalogs:", false, false);
            	_log.info("Adding individual JPL Catalogs:");
		        while((dbreadline = input.readLine())!= null)
	            {
					formulatag = dbreadline.substring(0,6);
					printOutput(formulatag,false,true);
	            	//System.out.print(formulatag);
	            	formula = dbreadline.substring(7,20).replace((String)" ",(String)""); //Removes blanks from formula
	            	q[0] = dbreadline.substring(27,33);				
	            	q[1] = dbreadline.substring(34,40);
	            	q[2] = dbreadline.substring(41,47);
	            	q[3] = dbreadline.substring(48,54);
	            	q[4] = dbreadline.substring(55,61);
	            	q[5] = dbreadline.substring(62,68);
	            	q[6] = dbreadline.substring(69,75);
					pathfile = pathdb + "catalog/c" +dbreadline.substring(0,6).replace(" ","0") + ".cat";
					// Open the line catalog only to read the variable QNFMT (quantum number format)
					// It is included in JPLcat to avoid repetition of the same variable
					inputcats = new BufferedReader(new FileReader(pathfile));
					dbjplreadline = inputcats.readLine();
					qnfmt = dbjplreadline.substring(51,55);
					//update("INSERT INTO JPLcat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
	            	//		" VALUES ('" + formulatag + "', '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
	            	//		false);
					update("INSERT INTO JPLcat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
	            			" VALUES (" + formulatag + ", '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
	            			false);
					inputcats.close();
					
					inputcats = new BufferedReader(new FileReader(pathfile));
			        while((dbjplreadline = inputcats.readLine())!= null)
			        {
			        	dbjplreadline = dbjplreadline.replace(" .","0.").replace(" -.","-0."); //Modifies the lines to avoid problems with integers
			        	freq = dbjplreadline.substring(0,13);
			        	err = dbjplreadline.substring(13,21).replace("  ",""); // This replace avoids an error in the transition 311405.0182 of molecule 53001
			        	lgint = dbjplreadline.substring(21,29);
			        	dr = dbjplreadline.substring(29,31);
			        	elo = dbjplreadline.substring(31,41);
			        	gup = toIntegerModified(dbjplreadline.substring(41,44).replace(" ",""));
			        	formulatag = dbjplreadline.substring(45,51).replace("-"," "); //Removes the minus in some tags
			        	//qnfmt = dbjplreadline.substring(51,55);
			        	for(int ii=0, jj = 55; ii<=5; ii++, jj=jj+2)
			        		{
								if (dbjplreadline.length() >= jj+14)
								{
					        		qn[ii] = dbjplreadline.substring(jj,jj+2).replace((String)"  ",(String)"NULL");
									qn[ii+6] = dbjplreadline.substring(jj+12,jj+14).replace((String)"  ",(String)"NULL");
									// if one of them is not null both will not be null and we can
									// convert them into the right integer, but still strings to send to the SQL command
									if (!"NULL".equals(qn[ii]))
									{
										qn[ii]= Integer.toString(toIntegerModified(qn[ii].replace(" ","")));
										qn[ii+6]= Integer.toString(toIntegerModified(qn[ii+6].replace(" ","")));
									}
								}
								else
								{
									//qn[ii] = "";
									//qn[ii+6] = "";
									// CHANGE TO NULL JUST IN CASE NOW IT FAILS WITH THE INTEGER QUANTUM NUMBERS
									qn[ii] = "NULL";
									qn[ii+6] = "NULL";  
								}
		        			}
//						_log.info(
//								" VALUES ('" + formulatag + "'," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
//			            		", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')");
			        	try{
			        		/*update("INSERT INTO JPL (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
			        				" VALUES ('" + formulatag + "'," + freq + ", " + new Double(err) + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
			        				", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
			        				false);*/
			        		update("INSERT INTO JPL (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
			        				" VALUES ('" + formulatag + "'," + freq + ", " + new Double(err) + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
			        				", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] + ")",
			        				false);
			        	} catch (SQLException e) {
							System.out.println("ERROR WHEN ISSUING:");
							/*System.out.println("INSERT INTO JPL (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
			        				" VALUES ('" + formulatag + "'," + freq + ", " + new Double(err) + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
			        				", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')");*/
							System.out.println("INSERT INTO JPL (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
			        				" VALUES (" + formulatag + "," + freq + ", " + new Double(err) + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
			        				", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] + ")");
						}
			        }
			        inputcats.close();
//	            	_log.info(formulatag + " - added");
//}//end of IF
				} 
		        input.close();
		        _log.info("*Done");
		        _log.info("Creating INDEX Tables");
		        _log.info("id_TAG INDEX:");
		        update("CREATE INDEX JPLindexTAG ON JPL (id_TAG)",false);
		        _log.info("*Done");
		        _log.info("id_frequency INDEX:");
		        update("CREATE INDEX JPLindexFREQ ON JPL (id_frequency)",false);
		        printOutput("*Done : JPL catalog added", false, true);
		        _log.info("*Done : JPL catalog added");
		    } catch (SQLException ex3) {
	        	ex3.printStackTrace(); 
			}
		 	
			} catch (IOException e) {
				e.printStackTrace();
			}

		
	}

	/**
     * Add CDMS catalog to DB
     * @isUserDB True if method is used to create an user table
     */
	@Deprecated
	private void cdms(boolean isUserDB) throws IOException {
		String pathdb, pathfile;
		String dbreadline, dbjplreadline;
		String CDMSorUSER;
		if (isUserDB){
			CDMSorUSER = SlimConstants.CATALOG_USER;
		}
		else {
			CDMSorUSER = "CDMS";
		}
		printOutput("START: "+CDMSorUSER, false, true);

		try {
			try{
				/**
				 * First checks that for each molecule ID in partition_function.html 
				 * it exists a file c[ID].cat.
				 * If any inconsistency is found, exits program.
				 */
		        _log.info("* Checking " + CDMSorUSER + " catalog files");
				if (isUserDB)
			        pathdb = pathdb_USER; 
				else
			        pathdb = pathdb_CDMS;  
		        BufferedReader input = new BufferedReader(new FileReader(pathdb + "partition_function.html"));
		        BufferedReader inputcats;
				String formulatag =" ";
				while((dbreadline = input.readLine())!= null)
				{
					if (dbreadline.length()>100 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
					{
						/**
						 *  files are located in:
						 *  /home/smartin/workspace/datos/CDMS/catalog/ for CDMS
						 *  or
						 *  directly in userdbfilespath for USER
						 */ 
						if (isUserDB){
							pathfile = pathdb + "c" + dbreadline.substring(0,6).replace(" ","0") + ".cat";
						}
						else {
							pathfile = pathdb + "catalog/c" + dbreadline.substring(0,6).replace(" ","0") + ".cat";
						}
						File catjplfiles = new File(pathfile);
						//IF it does not exist, complain and exit
						if(!catjplfiles.exists()) {
							printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",false,false);
							printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",true,false);
							System.exit(1);
						}
						//IF it exist, we check the ID of the molecule in the file matches that of catdir.cat
						else
						{
							//pathfile = pathdb + "catalog/c" +dbreadline.substring(0,6).replace(" ","0") + ".cat";
							inputcats = new BufferedReader(new FileReader(pathfile));
							dbjplreadline = inputcats.readLine();
							if(!dbjplreadline.substring(45,51).replace("-","0").replace(" ","0").equals(dbreadline.substring(0,6).replace(" ","0")))
							{
								_log.info("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
										"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK");
								printOutput("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
										"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK",
										true, false);
								System.exit(1);
							}
						}
					}
				}
	            input.close();
		        _log.info("* Checked successfully");
	        	_log.info("* Insert " + CDMSorUSER + " into TableIndex");
		        update("DELETE FROM TableIndex WHERE  id_tablename='"+CDMSorUSER+"'", isUserDB);
	        	update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( '" + CDMSorUSER + "'         , '" + CDMSorUSER + "      ' , 'Molecular' )",
	        			isUserDB);
		        _log.info("Generating table for the catalog: " + CDMSorUSER);

		        /**
		         * Creates the CDMScat or USERcat table where the Molecules are stored with 
		         * the Partition function at different temperatures.
		         */
		        update("DROP TABLE IF EXISTS " + CDMSorUSER + "cat", isUserDB);
		        _log.info("TABLE " + CDMSorUSER + "cat DELETED");
	            update("CREATE CACHED TABLE " + CDMSorUSER + "cat (" +
	            		"id_TAG INTEGER PRIMARY KEY, " +
	            		"id_formula CHAR(13) NOT NULL, " +
	            		"id_QNFMT SMALLINT NOT NULL, " +
	            		"id_QLOG300 DOUBLE NOT NULL, " +
	            		"id_QLOG225 DOUBLE NOT NULL, " +
	            		"id_QLOG150 DOUBLE NOT NULL, " +
	            		"id_QLOG75 DOUBLE NOT NULL, " +
	            		"id_QLOG37 DOUBLE NOT NULL," +
	            		"id_QLOG18 DOUBLE NOT NULL, " +
	            		"id_QLOG9  DOUBLE NOT NULL)",
	            		isUserDB);

	            /**
		         * Creates the CDMS or USER table with the spectroscopic information
		         * for each molecular transition 
		         */
	            // BORRAR FORMULA CUANDO SE HAGA UNA BUSQUEDA EN LAS DOS TABLAS
	            update("DROP TABLE IF EXISTS " + CDMSorUSER + "", isUserDB);
		        _log.info("TABLE " + CDMSorUSER + " DELETED");
		        update("CREATE CACHED TABLE " + CDMSorUSER + " (" +
	            		"id_TAG INTEGER NOT NULL, " +
	            		//"id_formula CHAR(13) NOT NULL, " +
	            		"id_frequency DOUBLE NOT NULL, " +
	            		"id_ERR  DOUBLE NOT NULL, " +
	            		"id_LGINT DOUBLE NOT NULL, " +
	            		"id_DR TINYINT NOT NULL, " +
	            		"id_ELO DOUBLE NOT NULL, " +
	            		"id_GUP SMALLINT NOT NULL," +
	            		//"id_QNFMT INTEGER NOT NULL, " +
	            		"id_QN1  SMALLINT NOT NULL, " +
	            		"id_QN2  SMALLINT, " +
	            		"id_QN3  SMALLINT, " +
	            		"id_QN4  SMALLINT, " +
	            		"id_QN5  SMALLINT, " +
	            		"id_QN6  SMALLINT, " +
	            		"id_QNN1  SMALLINT NOT NULL, " +
	            		"id_QNN2  SMALLINT, " +
	            		"id_QNN3  SMALLINT, " +
	            		"id_QNN4  SMALLINT, " +
	            		"id_QNN5  SMALLINT, " +
	            		"id_QNN6  SMALLINT)",
	            		isUserDB);
		        
	            /**
	             * Adds each of the CDMS or USER catalogs adding the corresponging entry in 
	             * Table CDMScat or USERcat (molecule and partition functions)
	             */
	            input = new BufferedReader(new FileReader(pathdb + "partition_function.html"));
		        String formula;
		        String q[] = new String[7]; // Array for the partition functions
		        String freq=" ", err=" ", lgint=" ", dr=" ", elo=" ", qnfmt=" ";
		        int gup;
		        String qn[] = new String[12]; // Array for the quantum numbers
		        _log.info("Adding individual " + CDMSorUSER + " Catalogs:");

		        printOutput("Adding individual " + CDMSorUSER + " Catalogs:", false, false);
		        while((dbreadline = input.readLine())!= null)
	            {
					if (dbreadline.length()>100 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
					{
						formulatag = dbreadline.substring(0,6);
						printOutput(formulatag,false,true);
						//System.out.print(formulatag);
						//_log.info(" --- " + dbreadline);
						//if (!formulatag.equals(" 51505")) 
							//	PROBLEMA PORQUE FALTAN COLUMNAS!!! 51505
						//{
						formula = dbreadline.substring(7,30).replace((String)" ",(String)"").replace("'","_"); //Removes blanks from formula
						q[0] = dbreadline.substring(71,77);				
						q[1] = dbreadline.substring(84,90);
						q[2] = dbreadline.substring(97,103);
						q[3] = dbreadline.substring(110,116);
						q[4] = dbreadline.substring(123,129);
						q[5] = dbreadline.substring(136,142);
						q[6] = dbreadline.substring(149,155);
				        ////////////////////////////// PROBAR QUE ESTE IF FUNCIONA!!!!!!!!!!!!!!!!!!!!!!!!!
						// To deal with the molecules without partition function in CDMS
						//if (q[0].equals("   ---"))
						//	{
						//	for(int ii=0;ii<=6;ii++)
						//	{
						//		q[ii]=q[ii].replace("   ---","0.0000");
						//	}
						//	}
						//_log.info(q[0] + "**" + q[1] + "**" +q[2] + "**" +q[3] + "**" +q[4] + "**" +q[5] + "**" +q[6] + "**" );
						if (isUserDB){
							pathfile = pathdb + "c" + dbreadline.substring(0,6).replace(" ","0") + ".cat";
						}
						else {
							pathfile = pathdb + "catalog/c" + dbreadline.substring(0,6).replace(" ","0") + ".cat";
						}
						// Open the line catalog only to read the variable QNFMT (quantum number format)
						// It is included in JPLcat to avoid repetition of the same variable
						inputcats = new BufferedReader(new FileReader(pathfile));
						dbjplreadline = inputcats.readLine();
						qnfmt = dbjplreadline.substring(51,55);
						//update("INSERT INTO " + CDMSorUSER + "cat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
						//		" VALUES ('" + formulatag + "', '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
						//		isUserDB);
						update("INSERT INTO " + CDMSorUSER + "cat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
								" VALUES (" + formulatag + ", '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
								isUserDB);
						inputcats.close();
					
						inputcats = new BufferedReader(new FileReader(pathfile));
						while((dbjplreadline = inputcats.readLine())!= null)
						{
							dbjplreadline = dbjplreadline.replace(" .","0.").replace(" -.","-0."); //Modifies the lines to avoid problems with integers
							freq = dbjplreadline.substring(0,13);
							err = dbjplreadline.substring(13,21);
							lgint = dbjplreadline.substring(21,29);
							dr = dbjplreadline.substring(29,31);
							elo = dbjplreadline.substring(31,41);
							gup = toIntegerModified(dbjplreadline.substring(41,44).replace(" ",""));
							formulatag = dbjplreadline.substring(45,51).replace("-"," "); //Removes the minus in some tags
							//qnfmt = dbjplreadline.substring(51,55);
							for(int ii=0, jj = 55; ii<=5; ii++, jj=jj+2)
							{
								if (dbjplreadline.length() >= jj+14)
								{
									qn[ii] = dbjplreadline.substring(jj,jj+2).replace((String)"  ",(String)"NULL");
									qn[ii+6] = dbjplreadline.substring(jj+12,jj+14).replace((String)"  ",(String)"NULL");
									// if one of them is not null both will not be null and we can
									// convert them into the right integer, but still strings to send to the SQL command
									if (!"NULL".equals(qn[ii]))
									{
										qn[ii]= Integer.toString(toIntegerModified(qn[ii].replace(" ","")));
										qn[ii+6]= Integer.toString(toIntegerModified(qn[ii+6].replace(" ","")));
									}
								}
								else
								{
									//qn[ii] = "";
									//qn[ii+6] = "";
									// CHANGE TO NULL JUST IN CASE NOW IT FAILS WITH THE INTEGER QUANTUM NUMBERS
									qn[ii] = "NULL";
									qn[ii+6] = "NULL";  
								}
		        			}
//							_log.info(
//									" VALUES (\"" + formulatag + "\"," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
//									", \"" + qn[0]+ "\", \"" + qn[1] + "\", \"" + qn[2] + "\", \"" + qn[3] + "\", \"" + qn[4] + "\", \"" + qn[5] + "\", \"" + qn[6] + "\", \"" + qn[7] + "\", \"" + qn[8] + "\", \"" + qn[9] + "\", \"" + qn[10] + "\", \"" + qn[11] + "\")");
							try{
								/*update("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES ('" + formulatag + "'," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
										isUserDB);*/
								update("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] + ")",
										isUserDB);
							} catch (SQLException e) {
								e.printStackTrace();
								printOutput("ERROR WHEN ISSUING:",true,false);
								/*printOutput("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES ('" + formulatag + "'," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
										true,false);*/
								printOutput("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] + ")",
										true,false);
							}
						}
						inputcats.close();
//						_log.info(formulatag + " - added");
						//} //END IF EXCLUDING FILE 51505
		        	}
				}
		        input.close();
		        _log.info("*Done");
		        _log.info("Creating INDEX Tables");
		        _log.info("id_TAG INDEX:");
		        update("CREATE INDEX " + CDMSorUSER + "indexTAG ON " + CDMSorUSER + " (id_TAG)", isUserDB);
		        _log.info("*Done");
		        _log.info("id_frequency INDEX:");
		        update("CREATE INDEX " + CDMSorUSER + "indexFREQ ON " + CDMSorUSER + " (id_frequency)", isUserDB);
		        printOutput("*Done : "+CDMSorUSER+" catalog added", false, true);
		        _log.info("*Done : " + CDMSorUSER + " catalog added");
		    } catch (SQLException ex3) {
	        	ex3.printStackTrace(); 
	        	printOutput("Error SQL cdms:" +ex3.getMessage(), true, false);
			}
		 	
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}
	
	
	/**
     * Add CDMSHFS catalog to DB
     * @isUserDB True if method is used to create an user table
     */
	@Deprecated
	private void cdmshfs() throws IOException {
		String pathdb, pathfile;
		String dbreadline, dbjplreadline;

		printOutput("START: CDMSHFS", false, true);

		try {
			try{
				/**
				 * First checks that for each molecule ID in partition_function.html 
				 * it exists a file c[ID].cat.
				 * If any inconsistency is found, exits program.
				 */
		        _log.info("* Checking CDMSHFS catalog files");
		        pathdb = pathdb_CDMS;  //"/home/smartin/workspace/datos/CDMS/";
		        BufferedReader input = new BufferedReader(new FileReader(pathdb + "partition_function_hfs.html"));
		        BufferedReader inputcats;
				String formulatag =" ";
				while((dbreadline = input.readLine())!= null)
				{
					if (dbreadline.length()>100 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
					{
						/**
						 *  files are located in:
						 *  /home/smartin/workspace/datos/CDMS/catalog_hfs/ for CDMSHFS
						 */ 
						pathfile = pathdb + "catalog_hfs/c" + dbreadline.substring(0,6).replace(" ","0") + "_hfs.cat";
						File catjplfiles = new File(pathfile);
						//IF it does not exist, complain and exit
						if(!catjplfiles.exists()) {
							printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",false,false);
							printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",true,false);
							System.exit(1);
						}
						//IF it exist, we check the ID of the molecule in the file matches that of catdir.cat
						else
						{
							inputcats = new BufferedReader(new FileReader(pathfile));
							dbjplreadline = inputcats.readLine();
							if(!dbjplreadline.substring(45,51).replace("-","0").replace(" ","0").equals(dbreadline.substring(0,6).replace(" ","0")))
							{
								_log.info("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
										"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK");
								printOutput("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
										"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK",
										true, false);
								System.exit(1);
							}
						}
					}
				}
	            input.close();
		        _log.info("* Checked successfully");
	        	_log.info("* Insert CDMSHFS into TableIndex");
		        update("DELETE FROM TableIndex WHERE  id_tablename='CDMSHFS'",false);
	        	update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( 'CDMSHFS'          , 'CDMSHFS   ' , 'Molecular' )",false);
		        _log.info("Generating table for the catalog: CDMSHFS");

		        /**
		         * Creates the CDMSHFScat table where the Molecules are stored with 
		         * the Partition function at different temperatures.
		         */
		        update("DROP TABLE IF EXISTS CDMSHFScat", false);
		        _log.info("TABLE CDMSHFScat DELETED");
	            update("CREATE CACHED TABLE CDMSHFScat (" +
	            		"id_TAG INTEGER PRIMARY KEY, " +
	            		"id_formula CHAR(13) NOT NULL, " +
	            		"id_QNFMT SMALLINT NOT NULL, " +
	            		"id_QLOG300 DOUBLE NOT NULL, " +
	            		"id_QLOG225 DOUBLE NOT NULL, " +
	            		"id_QLOG150 DOUBLE NOT NULL, " +
	            		"id_QLOG75 DOUBLE NOT NULL, " +
	            		"id_QLOG37 DOUBLE NOT NULL," +
	            		"id_QLOG18 DOUBLE NOT NULL, " +
	            		"id_QLOG9  DOUBLE NOT NULL)",
	            		false);

	            /**
		         * Creates the CDMS or USER table with the spectroscopic information
		         * for each molecular transition 
		         */
	            // BORRAR FORMULA CUANDO SE HAGA UNA BUSQUEDA EN LAS DOS TABLAS
	            update("DROP TABLE IF EXISTS CDMSHFS", false);
		        _log.info("TABLE CDMSHFS DELETED");
		        update("CREATE CACHED TABLE CDMSHFS (" +
	            		"id_TAG INTEGER NOT NULL, " +
	            		//"id_formula CHAR(13) NOT NULL, " +
	            		"id_frequency DOUBLE NOT NULL, " +
	            		"id_ERR  DOUBLE NOT NULL, " +
	            		"id_LGINT DOUBLE NOT NULL, " +
	            		"id_DR TINYINT NOT NULL, " +
	            		"id_ELO DOUBLE NOT NULL, " +
	            		"id_GUP SMALLINT NOT NULL," +
	            		//"id_QNFMT INTEGER NOT NULL, " +
	            		"id_QN1  SMALLINT NOT NULL, " +
	            		"id_QN2  SMALLINT, " +
	            		"id_QN3  SMALLINT, " +
	            		"id_QN4  SMALLINT, " +
	            		"id_QN5  SMALLINT, " +
	            		"id_QN6  SMALLINT, " +
	            		"id_QNN1  SMALLINT NOT NULL, " +
	            		"id_QNN2  SMALLINT, " +
	            		"id_QNN3  SMALLINT, " +
	            		"id_QNN4  SMALLINT, " +
	            		"id_QNN5  SMALLINT, " +
	            		"id_QNN6  SMALLINT)",
	            		false);
		        
	            /**
	             * Adds each of the CDMSHFS catalogs adding the corresponging entry in 
	             * Table CDMSHFScat (molecule and partition functions)
	             */
	            input = new BufferedReader(new FileReader(pathdb + "partition_function_hfs.html"));
		        String formula;
		        String q[] = new String[7]; // Array for the partition functions
		        String freq=" ", err=" ", lgint=" ", dr=" ", elo=" ", qnfmt=" ";
		        int gup;
		        String qn[] = new String[12]; // Array for the quantum numbers
		        _log.info("Adding individual CDMSHFS Catalogs:");

		        printOutput("Adding individual CDMSHFS Catalogs:", false, false);
		        while((dbreadline = input.readLine())!= null)
	            {
					if (dbreadline.length()>100 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
					{
						formulatag = dbreadline.substring(0,6);
						printOutput(formulatag,false,true);
						//System.out.print(formulatag);
						//_log.info(" --- " + dbreadline);
						//if (!formulatag.equals(" 51505")) 
							//	PROBLEMA PORQUE FALTAN COLUMNAS!!! 51505
						//{
						formula = dbreadline.substring(7,30).replace((String)" ",(String)"").replace("'","_"); //Removes blanks from formula
						q[0] = dbreadline.substring(71,77);				
						q[1] = dbreadline.substring(84,90);
						q[2] = dbreadline.substring(97,103);
						q[3] = dbreadline.substring(110,116);
						q[4] = dbreadline.substring(123,129);
						q[5] = dbreadline.substring(136,142);
						q[6] = dbreadline.substring(149,155);
				        ////////////////////////////// PROBAR QUE ESTE IF FUNCIONA!!!!!!!!!!!!!!!!!!!!!!!!!
						// To deal with the molecules without partition function in CDMS
						//if (q[0].equals("   ---"))
						//	{
						//	for(int ii=0;ii<=6;ii++)
						//	{
						//		q[ii]=q[ii].replace("   ---","0.0000");
						//	}
						//	}
						//_log.info(q[0] + "**" + q[1] + "**" +q[2] + "**" +q[3] + "**" +q[4] + "**" +q[5] + "**" +q[6] + "**" );
						pathfile = pathdb + "catalog_hfs/c" + dbreadline.substring(0,6).replace(" ","0") + "_hfs.cat";
						// Open the line catalog only to read the variable QNFMT (quantum number format)
						// It is included in JPLcat to avoid repetition of the same variable
						inputcats = new BufferedReader(new FileReader(pathfile));
						dbjplreadline = inputcats.readLine();
						qnfmt = dbjplreadline.substring(51,55);
						//update("INSERT INTO " + CDMSorUSER + "cat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
						//		" VALUES ('" + formulatag + "', '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
						//		isUserDB);
						update("INSERT INTO CDMSHFScat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
								" VALUES (" + formulatag + ", '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
								false);
						inputcats.close();
					
						inputcats = new BufferedReader(new FileReader(pathfile));
						while((dbjplreadline = inputcats.readLine())!= null)
						{
							dbjplreadline = dbjplreadline.replace(" .","0.").replace(" -.","-0."); //Modifies the lines to avoid problems with integers
							freq = dbjplreadline.substring(0,13);
							err = dbjplreadline.substring(13,21);
							lgint = dbjplreadline.substring(21,29);
							dr = dbjplreadline.substring(29,31);
							elo = dbjplreadline.substring(31,41);
							gup = toIntegerModified(dbjplreadline.substring(41,44).replace(" ",""));
							formulatag = dbjplreadline.substring(45,51).replace("-"," "); //Removes the minus in some tags
							//qnfmt = dbjplreadline.substring(51,55);
							for(int ii=0, jj = 55; ii<=5; ii++, jj=jj+2)
							{
								if (dbjplreadline.length() >= jj+14)
								{
									qn[ii] = dbjplreadline.substring(jj,jj+2).replace((String)"  ",(String)"NULL");
									qn[ii+6] = dbjplreadline.substring(jj+12,jj+14).replace((String)"  ",(String)"NULL");
									// if one of them is not null both will not be null and we can
									// convert them into the right integer, but still strings to send to the SQL command
									if (!"NULL".equals(qn[ii]))
									{
										qn[ii]= Integer.toString(toIntegerModified(qn[ii].replace(" ","")));
										qn[ii+6]= Integer.toString(toIntegerModified(qn[ii+6].replace(" ","")));
									}
								}
								else
								{
									//qn[ii] = "";
									//qn[ii+6] = "";
									// CHANGE TO NULL JUST IN CASE NOW IT FAILS WITH THE INTEGER QUANTUM NUMBERS
									qn[ii] = "NULL";
									qn[ii+6] = "NULL";  
								}
		        			}
//							_log.info(
//									" VALUES (\"" + formulatag + "\"," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
//									", \"" + qn[0]+ "\", \"" + qn[1] + "\", \"" + qn[2] + "\", \"" + qn[3] + "\", \"" + qn[4] + "\", \"" + qn[5] + "\", \"" + qn[6] + "\", \"" + qn[7] + "\", \"" + qn[8] + "\", \"" + qn[9] + "\", \"" + qn[10] + "\", \"" + qn[11] + "\")");
							try{
								/*update("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES ('" + formulatag + "'," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
										isUserDB);*/
								update("INSERT INTO CDMSHFS (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] + ")",
										false);
							} catch (SQLException e) {
								printOutput("ERROR WHEN ISSUING:",true,false);
								/*printOutput("INSERT INTO " + CDMSorUSER + " (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES ('" + formulatag + "'," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
										true,false);*/
								printOutput("INSERT INTO CDMSHFS (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
										", '" + qn[0]+ "', '" + qn[1] + "', '" + qn[2] + "', '" + qn[3] + "', '" + qn[4] + "', '" + qn[5] + "', '" + qn[6] + "', '" + qn[7] + "', '" + qn[8] + "', '" + qn[9] + "', '" + qn[10] + "', '" + qn[11] + "')",
										true,false);
							}
						}
						inputcats.close();
//						_log.info(formulatag + " - added");
						//} //END IF EXCLUDING FILE 51505
		        	}
				}
		        input.close();
		        _log.info("*Done");
		        _log.info("Creating INDEX Tables");
		        _log.info("id_TAG INDEX:");
		        update("CREATE INDEX CDMSHFSindexTAG ON CDMSHFS (id_TAG)", false);
		        _log.info("*Done");
		        _log.info("id_frequency INDEX:");
		        update("CREATE INDEX CDMSHFSindexFREQ ON CDMSHFS (id_frequency)", false);
		        printOutput("*Done : CDMSHFS catalog added", false, true);
		        _log.info("*Done : CDMSHFS catalog added");
		    } catch (SQLException ex3) {
	        	ex3.printStackTrace(); 
	        	printOutput("Error SQL cdms:" +ex3.getMessage(), true, false);
			}
		 	
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}

	/**
     * Add the individual catalog taking into account which type it is
     * @catType LOVAS92 / LOVAS2003 / BLOVAS / JPL / CDMS / CDMSHFS / USER
     * @isUserDB True if method is used to create an user table
     */
     //PONER LAS CONSTANTES
	private void addMolecularCatalog(String catType) throws IOException {
		String tablename, tablenameUI;
		String pathdb, partitionfunctionfile, pathfile ="";
		boolean isStandard;  // For Lovas catalogs, which are in a non standard format
		String dbreadline, dbjplreadline;
		boolean isUserDB=false;
		int QNnumber=0;
		switch (catType) {
		case "LOVAS92":
			tablename = "SmallLovas92";
			tablenameUI="Lovas 1992";
			pathdb="";
			pathfile=pathdb_slovas92;
			partitionfunctionfile="";
			isStandard=false;
			break;
		case "LOVAS2003":
			tablename ="SmallLovas03";
			tablenameUI="Lovas 2003";
			pathdb="";
			pathfile=pathdb_slovas03;
			partitionfunctionfile="";
			isStandard=false;
			break;
		case "BLOVAS":
			tablename ="BigLovas";
			tablenameUI="Big  Lovas";
			pathdb="";
			pathfile=pathdb_blovas;
			partitionfunctionfile="";
			isStandard=false;
			break;
		case SlimConstants.CATALOG_JPL:
			tablename ="JPL";
			tablenameUI="JPL       ";
			pathdb=pathdb_JPL;
			partitionfunctionfile="catdir.cat";
			isStandard=true;
			break;
		case SlimConstants.CATALOG_CDMS:
			tablename ="CDMS";
			tablenameUI="CDMS      ";
			pathdb=pathdb_CDMS;
			partitionfunctionfile="partition_function.html";
			isStandard=true;
			break;
		/**
		 * Eduardo Toledo Sep 2024
		 * Changes to add the HITRAN catalog
		 */
		case SlimConstants.CATALOG_HITRAN:
			tablename ="HITRAN";
			tablenameUI ="HITRAN    ";
			pathdb=pathdb_HITRAN;
			partitionfunctionfile="partition_function.html";
			isStandard=true;
			break;
		/**
		 * Eduardo Toledo Dec 2024
		 * Changes to add the CDMS ortho para catalog
		 */
		case SlimConstants.CATALOG_CDMSOP:
			tablename ="CDMSOP";
			tablenameUI="CDMSOP   ";
			pathdb=pathdb_ORTPAR;
			partitionfunctionfile="partition_function.html";
			isStandard=true;
			break;
		case SlimConstants.CATALOG_CDMSHFS:
			tablename ="CDMSHFS";
			tablenameUI="CDMSHFS   ";
			pathdb=pathdb_CDMS;
			partitionfunctionfile="partition_function_hfs.html";
			isStandard=true;
			break;
		case SlimConstants.CATALOG_LSD:
			tablename ="LSD";
			tablenameUI="LSD       ";
			pathdb=pathdb_LSD;
			partitionfunctionfile="partition_function.html";
			isStandard=true;
			break;
		case SlimConstants.CATALOG_USER:
			tablename = SlimConstants.CATALOG_USER;
			tablenameUI=SlimConstants.CATALOG_USER;
			pathdb=pathdb_USER;
			partitionfunctionfile="partition_function.html";
			isStandard=true;
			isUserDB=true;
			break;
		default:
			tablename="";
			tablenameUI="";
			partitionfunctionfile="";
			isStandard=false;
			pathdb="";
			
			printOutput("ERROR: " + catType + " catalog type not known. PLEASE CHECK",false,false);
			printOutput("ERROR: " + catType + " catalog type not known. PLEASE CHECK",true,false);
			System.exit(1);
			//TODO Some exit exception
			break;
		}
		printOutput("START: "+tablename, false, true);
		/**
		 * First checks that for each molecule ID in partition_function.html 
		 * it exists a file c[ID].cat.
		 * If any inconsistency is found, exits program.
		 */
		try {
			try{
				/**
				 * IF standard catalog basic checks:
				 * First checks:
				 * - That for each molecule ID in partition_function.html it exists a file c[ID].cat.
				 * - That the tag is consistent
				 * - Determine the number of quantum numbers depending on lenght of the  first line of the .cat file
				 * If any inconsistency is found, exits program.
				 */
				BufferedReader input;
		        BufferedReader inputcats;
		        String formulatag=" ";
		        
				if (isStandard)
				{
			        input = new BufferedReader(new FileReader(pathdb + partitionfunctionfile));
					printOutput("INFO: Partition fuctions file: " + pathdb + partitionfunctionfile,false,false);

			        _log.info("* Checking "+ tablename +" catalog files");
					while((dbreadline = input.readLine())!= null)
					{
						// IF to ensure we read only the relevant lines without header or footer
						if (dbreadline.length()>75 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
						{
							formulatag = dbreadline.substring(0,6);
							//printOutput(formulatag,false,true);
							/**
							 *  files are located different for
							 *  HFS, also with different file name
							 *  USER, located in the same as partition function.
							 */ 
							switch (catType) {
							case "USER":
								pathfile = pathdb + "c" + formulatag.replace(" ","0") + ".cat";
								break;
							case "CDMSHFS":
								pathfile = pathdb + "catalog_hfs/c" + formulatag.replace(" ","0") + "_hfs.cat";
								break;
							default:
								pathfile = pathdb + "catalog/c" + formulatag.replace(" ","0") + ".cat";
								break;
							}
							File catjplfiles = new File(pathfile);
							//IF it does not exist, complain and exit
							if(!catjplfiles.exists()) {
								printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",false,false);
								printOutput("ERROR: " + catjplfiles + " does not exist exists. PLEASE CHECK",true,false);
								System.exit(1);
							}
							//IF it exist, we check the ID of the molecule in the partition file matches that of spectroscopic file
							else
							{
								inputcats = new BufferedReader(new FileReader(pathfile));
								dbjplreadline = inputcats.readLine();
								//Eduardo Toledo Oct 2024 Cambios para acomodar Hitran
								if (catType == SlimConstants.CATALOG_HITRAN)
								{
									if(!dbjplreadline.substring(46,52).replace("-","0").replace(" ","0").equals(dbreadline.substring(0,6).replace(" ","0")))
									{
										_log.info("ERROR: " + dbjplreadline.substring(46,52).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
												"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK");
										printOutput("ERROR: " + dbjplreadline.substring(46,52).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
												"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK",
												true, false);
										System.exit(1);
									}
								}
								else
								{
								if(!dbjplreadline.substring(45,51).replace("-","0").replace(" ","0").equals(dbreadline.substring(0,6).replace(" ","0")))
								{
									_log.info("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
											"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK");
									printOutput("ERROR: " + dbjplreadline.substring(45,51).replace("-","0").replace(" ","0") + " molecule ID in " + catjplfiles + 
											"\n    does not match the ID " + dbreadline.substring(0,6).replace(" ","0") + " in catdir.cat. PLEASE CHECK",
											true, false);
									System.exit(1);
								}
								}
								// IF file is ok, we check the line lenght to determine how many quantum numbers there are. 
								// REMOVED AND DEALT WITH WITH SOME BASIC LOGIC BELOW.
								// ONLY IF WE FIND SOME CRITICAL CASE WE CAN CHECK IT OUT BEFOREHAND
								/*else
									switch (dbjplreadline.length()) {
									case 79:
									case 80:
									case 81:
										break;
									case 83:
										break;
									default:
										printOutput("ERROR: " + pathfile + " line lenght of " + dbjplreadline.length()+ " is not a valid number",false,false);
										printOutput("ERROR: " + pathfile + " line lenght of " + dbjplreadline.length()+ " is not a valid number",true,false);
										System.exit(1);
										break;
									}
									*/
									
							}
						}
					}
		            input.close();
			        _log.info("* Checked successfully");
				}
				// If not standard, just check that the right files are there.
				else
				{
					File inputFile = new File(pathfile);
					if (!inputFile.exists())
					{
						printOutput("ERROR: " + pathfile + " does not exist exists. PLEASE CHECK",false,false);
						printOutput("ERROR: " + pathfile + " does not exist exists. PLEASE CHECK",true,false);
						System.exit(1);
					}

				}
				
				/* 
				 * POPULATING TABLE INDEX
				 */
		    	_log.info("* Insert "+tablename+"into TableIndex");
				update("INSERT INTO TableIndex (id_tablename, id_tablelongname , id_tabletype) VALUES ( '"+tablename+"' , '"+tablenameUI+"' , 'Molecular' )",isUserDB);
		    	_log.info("* Generating table for the catalog: "+tablenameUI);
		        /**
		         * Creates the table where the Molecules are stored with 
		         * the Partition function at different temperatures.
		         */
		        update("DROP TABLE IF EXISTS "+tablename+"cat", isUserDB);
		        _log.info("TABLE "+tablename+"cat DELETED");
	            update("CREATE CACHED TABLE "+tablename+"cat (" +
	            		"id_TAG INTEGER PRIMARY KEY, " +
	            		"id_formula CHAR(13) NOT NULL, " +
	            		"id_QNFMT SMALLINT NOT NULL, " +
	            		"id_QLOG300 DOUBLE NOT NULL, " +
	            		"id_QLOG225 DOUBLE NOT NULL, " +
	            		"id_QLOG150 DOUBLE NOT NULL, " +
	            		"id_QLOG75 DOUBLE NOT NULL, " +
	            		"id_QLOG37 DOUBLE NOT NULL," +
	            		"id_QLOG18 DOUBLE NOT NULL, " +
	            		"id_QLOG9  DOUBLE NOT NULL)",
	            		isUserDB);
	            /**
		         * Creates the CDMS or USER table with the spectroscopic information
		         * for each molecular transition 
		         */
	            // BORRAR FORMULA CUANDO SE HAGA UNA BUSQUEDA EN LAS DOS TABLAS
	            update("DROP TABLE IF EXISTS "+tablename+"", isUserDB);
		        _log.info("TABLE "+tablename+" DELETED");
		        update("CREATE CACHED TABLE "+tablename+" (" +
	            		"id_TAG INTEGER NOT NULL, " +
	            		//"id_formula CHAR(13) NOT NULL, " +
	            		"id_frequency DOUBLE NOT NULL, " +
	            		"id_ERR  DOUBLE NOT NULL, " +
	            		"id_LGINT DOUBLE NOT NULL, " +
	            		"id_DR TINYINT NOT NULL, " +
	            		"id_ELO DOUBLE NOT NULL, " +
	            		"id_GUP SMALLINT NOT NULL," +
	            		//"id_QNFMT INTEGER NOT NULL, " +
	            		"id_QN1  SMALLINT NOT NULL, " +
	            		"id_QN2  SMALLINT, " +
	            		"id_QN3  SMALLINT, " +
	            		"id_QN4  SMALLINT, " +
	            		"id_QN5  SMALLINT, " +
	            		"id_QN6  SMALLINT, " +
	            		"id_QN7  SMALLINT, " +
	            		"id_QNN1  SMALLINT NOT NULL, " +
	            		"id_QNN2  SMALLINT, " +
	            		"id_QNN3  SMALLINT, " +
	            		"id_QNN4  SMALLINT, " +
	            		"id_QNN5  SMALLINT, " +
	            		"id_QNN6  SMALLINT, " +
	            		"id_QNN7  SMALLINT)",
	            		isUserDB);
		        
	            /**
	             * Adds each of the Catalogs adding the corresponding entry in 
	             * Table for the XXXcat molecule and partition functions
	             */
		        String formula =" ";
		        String q[] = new String[7]; // Array for the partition functions
		        String freq=" ", err=" ", lgint=" ", dr=" ", elo=" ", qnfmt=" ";
		        int gup;
		        String qn[] = new String[14]; // Array for the quantum numbers
		        _log.info("Adding individual "+tablename+" Catalogs:");
		        printOutput("Adding individual "+tablename+" Catalogs:", false, false);
				if (isStandard)
				{
		            input = new BufferedReader(new FileReader(pathdb + partitionfunctionfile));

			        _log.info("Read partitionfile "+pathdb + partitionfunctionfile);
			        printOutput("Read partitionfile "+pathdb + partitionfunctionfile, false, false);
			        while((dbreadline = input.readLine())!= null)
		            {
						if (dbreadline.length()>75 && !dbreadline.substring(0,4).equals(" tag") && !dbreadline.substring(0,4).equals("===="))
						{
							formulatag = dbreadline.substring(0,6);
							//printOutput(formulatag,false,true);

							if(formulatag.contains("57998"))
								Log.getInstance().logger.debug(dbreadline);
							switch (tablename) {
								case SlimConstants.CATALOG_JPL:
					            	formula = dbreadline.substring(7,20).replace((String)" ",(String)""); //Removes blanks from formula
					            	q[0] = dbreadline.substring(27,33);				
					            	q[1] = dbreadline.substring(34,40);
					            	q[2] = dbreadline.substring(41,47);
					            	q[3] = dbreadline.substring(48,54);
					            	q[4] = dbreadline.substring(55,61);
					            	q[5] = dbreadline.substring(62,68);
					            	q[6] = dbreadline.substring(69,75);
									break;
								default:
									formula = dbreadline.substring(7,30).replace((String)" ",(String)"").replace("'","_"); //Removes blanks from formula
									q[0] = dbreadline.substring(71,77);				
									q[1] = dbreadline.substring(84,90);
									q[2] = dbreadline.substring(97,103);
									q[3] = dbreadline.substring(110,116);
									q[4] = dbreadline.substring(123,129);
									q[5] = dbreadline.substring(136,142);
									q[6] = dbreadline.substring(149,155);
									break;
							}
							
							/**
							 *  files are located different for
							 *  HFS, also with different file name
							 *  USER, located in the same as partition function.
							 */ 
							switch (catType) {
								case SlimConstants.CATALOG_USER:
									pathfile = pathdb + "c" + formulatag.replace(" ","0") + ".cat";
									break;
								case SlimConstants.CATALOG_CDMSHFS:
									pathfile = pathdb + "catalog_hfs/c" + formulatag.replace(" ","0") + "_hfs.cat";
									break;
								default:
									pathfile = pathdb + "catalog/c" + formulatag.replace(" ","0") + ".cat";
									break;
							}
							// Open the line first catalog only to read the variable QNFMT (quantum number format)
							// It is included in JPLcat to avoid repetition of the same variable
							inputcats = new BufferedReader(new FileReader(pathfile));
							dbjplreadline = inputcats.readLine();
							qnfmt = dbjplreadline.substring(51,55);
							// Eduardo Toledo Nov 2024
							// Commented out to check if fails
							// if(formulatag.contains("57998"))
								// Log.getInstance().logger.debug(dbreadline);
							update("INSERT INTO "+tablename+"cat (id_TAG, id_formula, id_QNFMT, id_QLOG300, id_QLOG225, id_QLOG150, id_QLOG75, id_QLOG37, id_QLOG18, id_QLOG9)" +
									" VALUES (" + formulatag + ", '" + formula + "', " + qnfmt + ", " + q[0] + ", " + q[1] +  ", " + q[2] + ", " + q[3] +  ", " + q[4] + ", " + q[5] + ", " + q[6] + ")",
									isUserDB);
							// And also to determine the number of quantum numbers
							// It will always be considered the basic 6 numbers unless the line is 83 characters (maybe to be modified if even longer cases are found)
							// A case which would have problems is one with 7 QN but truncated line, but this would make no sense, because otherwise it is expected to use the default of 6 .
							/*switch (dbjplreadline.length()) {
							case 79:
							case 80:
							case 81:
								QNnumber=6;
								break;
							case 83:
								QNnumber=7;
								break;
							default:
								// No default since it was previously checked.
								QNnumber=0;
								break;
							}*/
							if (dbjplreadline.length()==83)
								QNnumber=7;
							else
								QNnumber=6;
							
							inputcats.close();
							printOutput(formulatag+"  QN="+QNnumber,false,true);

					    
							inputcats = new BufferedReader(new FileReader(pathfile));
							try {
								while((dbjplreadline = inputcats.readLine())!= null)
								{
									if(dbjplreadline.isEmpty())
										break;
									dbjplreadline = dbjplreadline.replace(" .","0.").replace(" -.","-0."); //Modifies the lines to avoid problems with integers
									freq = dbjplreadline.substring(0,13);
									err = dbjplreadline.substring(13,21);
									lgint = dbjplreadline.substring(21,29);
									dr = dbjplreadline.substring(29,31);
									elo = dbjplreadline.substring(31,41);
									//Eduardo Toledo Octubre 2024 Cambios para acomodar Hitran
									if (catType == SlimConstants.CATALOG_HITRAN) {
										gup = toIntegerModified(dbjplreadline.substring(41,45).replace(" ",""));
										formulatag = dbjplreadline.substring(46,52).replace("-"," "); //Removes the minus in some tags
									} else {
									        gup = toIntegerModified(dbjplreadline.substring(41,44).replace(" ",""));
									        formulatag = dbjplreadline.substring(45,51).replace("-"," "); //Removes the minus in some tags
									}      
									//qnfmt = dbjplreadline.substring(51,55);
	//								for(int ii=0, jj = 55; ii<QNnumber; ii++, jj=jj+2)
	//								{
	//									if (dbjplreadline.length() >= jj+14)
									for(int ii=0, jj = 55; ii<7; ii++, jj=jj+2)
									{
										//System.out.print("-----"+QNnumber+"----"+ii  +"       ");
										// If smaller than number of QN in file and the line is long enough to contain more 
										//(some lines contain only 1 QN despite supposed to have space for 6 and not enough spaces after the end of the line after first QN.
										// example:
										if (ii<QNnumber && dbjplreadline.length() >= jj+QNnumber*2+2)
										{
											qn[ii] = dbjplreadline.substring(jj,jj+2).replace((String)"  ",(String)"NULL");
											// For QNnumber = 6  this is jj+12,jj+14
											// For QNnumber = 7  this is jj+14,jj+16
											qn[ii+7] = dbjplreadline.substring(jj+QNnumber*2,jj+QNnumber*2+2).replace((String)"  ",(String)"NULL");
											// if one of them is not null both will not be null and we can
											// convert them into the right integer, but still strings to send to the SQL command
											if (!"NULL".equals(qn[ii]))
											{
												qn[ii]= Integer.toString(toIntegerModified(qn[ii].replace(" ","")));
												qn[ii+7]= Integer.toString(toIntegerModified(qn[ii+7].replace(" ","")));
											}
											//System.out.println(qn[ii]+"---"+qn[ii+7]);
										}
										else
										{
											//qn[ii] = "";
											//qn[ii+6] = "";
											// CHANGE TO NULL JUST IN CASE NOW IT FAILS WITH THE INTEGER QUANTUM NUMBERS
											qn[ii] = "NULL";
											qn[ii+7] = "NULL";  
										}
										//System.out.println(qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] +  ", " + qn[12] +  ", " + qn[13]);
				        			}
									//System.exit(1);
									try{
										update("INSERT INTO "+tablename+" (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QN7, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6, id_QNN7)" +
												" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
												", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] +  ", " + qn[12] +  ", " + qn[13] + ")",
												isUserDB);
									} catch (SQLException e) {
										e.printStackTrace();
										printOutput("ERROR WHEN ISSUING:",true,false);
										printOutput("INSERT INTO "+tablename+" (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QN7, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6, id_QNN7)" +
												" VALUES (" + formulatag + "," + freq + ", " + err + ", " + lgint + ", " + dr + ", " + elo + ", " + gup +
												", " + qn[0]+ ", " + qn[1] + ", " + qn[2] + ", " + qn[3] + ", " + qn[4] + ", " + qn[5] + ", " + qn[6] + ", " + qn[7] + ", " + qn[8] + ", " + qn[9] + ", " + qn[10] + ", " + qn[11] +  ", " + qn[12] +  ", " + qn[13] + ")",
												true,false);
									}
								} 

							}catch (Exception e) {
								//_log.info("!!!! WARNING Error: "+e.getMessage()+"  Not can read file: "+pathfile);
								
					        	printOutput("!!!! Error: "+e.getMessage()+":  file: "+pathfile+ " in line '"+dbjplreadline+"'", true, false);
							//	e.printStackTrace();	
								throw e; 

							}
							inputcats.close();

			        	}
					}
			        input.close();
			        _log.info("*Done");
			        _log.info("Creating INDEX Tables");
			        _log.info("id_TAG INDEX:");
			        update("CREATE INDEX "+tablename+"indexTAG ON "+tablename+" (id_TAG)", isUserDB);
			        _log.info("*Done");
				}
				else
				{
					//TODO for NON JPL/CDMS CATALOGS
					// FALTA GENERAR EL CATALOGO DE FUNCIONES DE PARTICION!!!!
					SortedMap<String, Integer> formulatagmap = new TreeMap<String,Integer>();
					int tagcounter = 10000;
			        printOutput("START: " + tablename, false, true);
			    	input = new BufferedReader(new FileReader(pathfile));
			    	char auxchar;
			    	while((dbreadline = input.readLine())!= null)
			    	{
			    		formulatag = "";
						switch(catType) {
						case "LOVAS92":
				    		if(dbreadline.length()>=90){
				    			freq = dbreadline.substring(2,13).replace((String)"*",(String)" ").replace((String)"?",(String)" ").replace((String)"(",(String)" ").replace((String)",",(String)".").replace((String)". ",(String)".0");
				    			err = dbreadline.substring(14,18).replace((String)"*",(String)" ").replace((String)"(",(String)"").replace((String)")",(String)"").replace((String)" ",(String)"");
				    			formula = dbreadline.substring(20,33).replace((String)" ",(String)"");
				    			auxchar = freq.substring(0,1).toCharArray()[0];
				    			if(auxchar>='1' && auxchar<='9'){
				    				if (formulatagmap.containsKey(formula))
				    					formulatag = String.valueOf(formulatagmap.get(formula));
				    				else
				    				{
				    					tagcounter++;
				    					formulatag=String.valueOf(tagcounter);
				    					formulatagmap.put(formula, tagcounter);
				    				}
				    			}
				    		}
						case "LOVAS2003":
							// TBD
						case "BLOVAS":
							// TBD
						if (formulatag!="")
							try{
								update("INSERT INTO "+tablename+" (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QN7, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6, id_QNN7)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + "-1.0,1,0.0,1,1,null,null,null,null,null,null,1,null,null,null,null,null,null)",
										isUserDB);
							} catch (SQLException e) {
								e.printStackTrace();
								printOutput("ERROR WHEN ISSUING:",true,false);
								printOutput("INSERT INTO "+tablename+" (id_TAG,  id_frequency, id_ERR, id_LGINT, id_DR, id_ELO, id_GUP, id_QN1, id_QN2, id_QN3, id_QN4, id_QN5, id_QN6, id_QN7, id_QNN1, id_QNN2, id_QNN3, id_QNN4, id_QNN5, id_QNN6, id_QNN7)" +
										" VALUES (" + formulatag + "," + freq + ", " + err + ", " + "-1.0,1,0.0,1,1,null,null,null,null,null,null,1,null,null,null,null,null,null)",
										true,false);
							}
						}
			    	}
			    	input.close();
				}
		        _log.info("Creating INDEX Tables");
		        _log.info("id_frequency INDEX:");
		        update("CREATE INDEX "+tablename+"indexFREQ ON "+tablename+" (id_frequency)", isUserDB);
		        printOutput("*Done : "+tablename+" catalog added", false, true);
		        _log.info("*Done : "+tablename+" catalog added");
		    } catch (SQLException ex3) {
	        	ex3.printStackTrace(); 
				_log.info("Error SQL: "+ex3.getMessage()+"  not applied : "+pathfile);
	        	printOutput("Error SQL cdms:" +ex3.getMessage(), true, false);
			}
		} catch (IOException e) {
				_log.info("Error: "+e.getMessage()+"  Not can read file: "+pathfile);
	        	printOutput("Error: "+e.getMessage()+"  Not can read file: "+pathfile, true, false);
				e.printStackTrace();		

		}
	}
	
	/**
	 * Check if there are letters substituting numbers
	 * and convert it to the corresponding number
	 */
	private int toIntegerModified(String input){
		try{
			if ("+".equals(input))
				return 1;
			else if ("-".equals(input))
				return -1;
			else
				return Integer.parseInt(input);
		} catch (NumberFormatException e) {
			return Integer.parseInt(
//					input.toUpperCase()
					input
					.replace("A","10")
					.replace("B","11")
					.replace("C","12")
					.replace("D","13")
					.replace("E","14")
					.replace("F","15")
					.replace("G","16")
					.replace("H","17")
					.replace("I","18")
					.replace("J","19")
					.replace("K","20")
					.replace("L","21")
					.replace("M","22")
					.replace("N","23")
					.replace("O","24")
					.replace("P","25")
					.replace("Q","26")
					.replace("R","27")
					.replace("S","28")
					.replace("T","29")
					.replace("U","30")
					.replace("V","31")
					.replace("W","32")
					.replace("X","33")
					.replace("Y","34")
					.replace("Z","35")
					.replace("a","-1")
					.replace("b","-2")
					.replace("c","-3")
					.replace("d","-4")
					.replace("e","-5")
					.replace("f","-6")
					.replace("g","-7")
					.replace("h","-8")
					.replace("i","-9")
			);
		}
	}

    /**
     * Creates the Database No user
     * @throws IOException If database cannot be connected
     */
	
	public void CreateSLIMdatabase () throws IOException {
		boolean isUserDB = false;
		if(deleteDb(isUserDB, false,true)){
			printOutput("BEGINNING DATABASE CREATION",false,true);
			long startTime = System.nanoTime();
	        try {
	        	
	        	connect(isUserDB);
	        	setreadonly(isUserDB,false);
	        	createSchema(isUserDB);
	        	// compact(isUserDB);
	            slovas92();
	            //compact(isUserDB);
	            slovas03();
	            //compact(isUserDB);
	            blovas();
	            //compact(isUserDB);
	            
	            //OLD
	            /*
	            jpl();
	            compact(false);
	            cdms(false);
	            compact(false);
	            cdmshfs();
	            compact(false);
	            */

	            //NEW
	            ///*
	            // Oct 2024 Pruebas Hitran
	            // Commented out todas las bases de datos menos Hitran
	            addMolecularCatalog(SlimConstants.CATALOG_JPL);
	            //compact(false);
	            addMolecularCatalog(SlimConstants.CATALOG_CDMS);
	            //compact(false);
	            /**
	    		 * Eduardo Toledo Sep 2024
	    		 * Changes to add the HITRAN catalog
	    		 */
	            addMolecularCatalog(SlimConstants.CATALOG_HITRAN);
	            //compact(false);
	            /**
	             * Eduardo Toledo Dec 2024
	             * Changes to add the CDMS ortho para catalog
	             */
	            addMolecularCatalog(SlimConstants.CATALOG_CDMSOP);
	            addMolecularCatalog(SlimConstants.CATALOG_CDMSHFS);
	            //compact(false);
	            addMolecularCatalog(SlimConstants.CATALOG_LSD);
	            compact(false);
	            //*/
	            
	        	shutdown(isUserDB);
	        	setreadonly(isUserDB,true);
	        	connect(isUserDB);
	            //System.out.println("MAKE SURE TO MODIFY lines.properties");
	            //System.out.println("readonly=true");
	            //System.out.println("modified=no");
	        } catch (SQLException ex3) {
	        		ex3.printStackTrace();
	        }
	        printOutput("DATABASE CREATED",false,true);
	        long elapsedTime = System.nanoTime() - startTime;
	        System.out.println("Total DB Creation time in minutes: "
	                + elapsedTime/1000000000/60);
	    }
		else
	        printOutput("DATABASE CREATION CANCELLED",false,true);
	}
	/**
     *Disconnect  and connect BD because the files is changes
     * @throws IOException If database cannot be connected
     */
	public void reConnectNewDatabase (boolean isUserDB) throws IOException {

		boolean isExistBD = db_exists(isUserDB);

			if(isUserDB)
				printOutput("BEGINNING USER DATABASE RECCONECT",false,false);
			else
				printOutput("BEGINNING DATABASE RECCONECT",false,false);
	        try {
	        	//*

//	        	if(isExistBD)
//	        	{
//		        	connect(isUserDB);
//	        		shutdown(isUserDB);
//	        	}
	        	disconnect(isUserDB);
	        	setreadonly(isUserDB,false);
	        	connect(isUserDB);
	        	if(isExistBD)
	        		compact(isUserDB);
	        	shutdown(isUserDB);
	        	setreadonly(isUserDB,true);
	        	connect(isUserDB);
//	        	compact(isUserDB);
	            //cdms(true);
	            //System.out.println("MAKE SURE TO MODIFY lines.properties");
	            //System.out.println("readonly=true");
	            //System.out.println("modified=no");
	        	} catch (SQLException ex3) {
	        		ex3.printStackTrace();
	        	}
				printOutput("DATABASE RECCONECTED",false,false);
	}
	/**
	 * Creates USER table in database based on user directory with CDMS formatted files
     * @param dbPath Full path to db files
	 * 
	 */
	/*private DbHSQLDBCreate CreateUSERtableinDB (String dbPath) throws IOException {
        System.out.println("BEGINNING USER DATABASE CREATION");
        try {        	
        	shutdown();
        } catch (SQLException ex3) {
            ex3.printStackTrace();
        }
    	setreadonly(db_name,false);
        DbHSQLDBCreate finaldb = null;
        try {
			finaldb = new DbHSQLDBCreate(db_name);
            finaldb.cdms(true);
            finaldb.compact(true);
            cdms(false);
            compact(false);
            finaldb.shutdown();
            finaldb.setreadonly(db_name_user,true);
            setreadonly(db_name,true);
		} catch (Exception e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
        /*try {
			finaldb = new DbHSQLDBCreate(db_name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("DATABASE USER TABLE CREATED");
        return finaldb;

	}*/

    /**
     * Creates the USER Database
     * @param pathtoUSERfiles Full path to USER db files
     * @throws IOException If database cannot be connected
     */
	public void CreateUSERdatabase (String pathtoUSERfiles) throws IOException {
		pathdb_USER = pathtoUSERfiles;
		boolean isUserDB = true;
		if(deleteDb(true, false,true)){
			printOutput("BEGINNING USER DATABASE CREATION",false,false);
	        try {
	        	//*
	        	connect(isUserDB);
	        	shutdown(isUserDB);
	        	setreadonly(isUserDB,false);
	        	connect(isUserDB);
	        	createSchema(isUserDB);
	        	compact(isUserDB);
	            //cdms(true);
	        	addMolecularCatalog("USER");
	            compact(isUserDB);
	            //*/
	        	shutdown(isUserDB);
	        	setreadonly(isUserDB,true);
	        	connect(isUserDB);
	            //System.out.println("MAKE SURE TO MODIFY lines.properties");
	            //System.out.println("readonly=true");
	            //System.out.println("modified=no");
				printOutput("DATABASE CREATED",false,false);
	        } catch (SQLException ex3) {
	        		ex3.printStackTrace();
	        } catch (Exception e) {
				// TODO: handle exception
	        	//DELETE BD

	            try {
					shutdown(isUserDB);
					setreadonly(isUserDB, true);
		        	deleteDb(true,false,false);
		        //	connect(isUserDB);

		        //	compact(isUserDB);
		        //	shutdown(isUserDB);
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
				//	e1.printStackTrace();
				}
	        	//dis
			}
		}   
		else
			printOutput("DATABASE CREATION CANCELLED",false,false);
	}
	
	/**
	 * Method to check whether databases exists and can be queried
	 */
	public boolean db_exists(boolean isUserDB){
		try {
			if(query(isUserDB,"SELECT * FROM  TableIndex")==null) {
				throw new SQLException("Database is empty");
			}
		}catch (SQLException ex) {
			if (isUserDB)
				System.out.println("USER Database not available: " + "" + ex.getMessage());
			else
				System.out.println("MADCUBA Database not available: " + "" + ex.getMessage());
            return false;
        }
		return true;
	}

	public boolean existConnection(boolean isUserDB) {
		if(isUserDB)
			return conn_user !=null;
		else
			return conn !=null;
	}
	/**
     * Main program to create and test the DB
     */
	public static void main(String[] args) throws IOException {
        DbHSQLDBCreate finaldb = null;
        try {
            // user.dir is the working directory, from where the java vm was launched
            String dbPath = System.getProperty("user.dir") + "/" + "lines";
            //finaldb = new DbHSQLDBCreate(dbPath);
        	//finaldb.queryPrint("SELECT * FROM  TableIndex");
            //finaldb.shutdown();
            //finaldb.DeleteDb(false);
            finaldb = new DbHSQLDBCreate(dbPath,true);
            System.out.println("MAIN DATABASE "+finaldb.db_name);
            System.out.println("MAIN DATABASE "+finaldb.db_name_user);
            
            boolean createDatabase = true;
        	boolean performQuery = false;

        	// DATABASE CREATION
            if (createDatabase)
            {
              finaldb.CreateSLIMdatabase();
              //finaldb.CreateUSERdatabase("/home/smartin/workspace/extradb_victor201902/");
            }
        	
        	// DATABASE TEST CONNECTION
        	
        	//finaldb.connect(false);
        	//System.out.println(finaldb.db_exists(false));
        	/*finaldb.connect(true);
        	System.out.println(finaldb.db_exists(true));
        	*/
        	//finaldb.shutdown(false);
            //finaldb.shutdown(true);
        	
        	// TEST FOR SQL QUERY
        	if (performQuery)
        	{
    			long startTime = System.nanoTime();
        		finaldb = new DbHSQLDBCreate(dbPath);
        		finaldb.connect(false);
        		long elapsedTime = System.nanoTime() - startTime;
    	        System.out.println("Connect time in ms: "
    	                + elapsedTime/1000000);
        		startTime = System.nanoTime();
        		finaldb.queryPrint(false, "SELECT "
        				+ "ID_FREQUENCY, ID_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, 0.0  as lab_ELO, 0.0 as lab_AIJ  "
        				+ "FROM SmallLovas92 WHERE id_formula='CO' AND  id_frequency BETWEEN 80000.0 AND 300000.0");
	        	finaldb.queryPrint(false, "SELECT "
	        			+ "id_TAG,id_frequency,id_ERR,id_LGINT,id_DR,id_ELO,id_GUP,id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,id_TAG,id_formula, id_QNFMT,id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9"
	        			+ ",'jpl' as cata FROM JPLcat INNER JOIN JPL ON JPL.id_TAG=JPLcat.id_TAG "
	        			+ "WHERE  id_ELO BETWEEN 0.0 AND 100000. "
//	        			+ "AND id_formula='CCH' "
//	        			+ "AND  id_frequency BETWEEN 80000.0 AND 391000.0  ");
	        			+ "AND id_formula='CH3OH' "
    					+ "AND  id_frequency BETWEEN 25000.0 AND 27000.0  ");
	        	finaldb.queryPrint(false, "SELECT "
	        			+ "id_TAG,id_frequency,id_ERR,id_LGINT,id_DR,id_ELO,id_GUP,id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,id_TAG,id_formula, id_QNFMT,id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9"
	        			+ ",'cdms' as cata FROM CDMScat INNER JOIN CDMS ON CDMS.id_TAG=CDMScat.id_TAG "
	        			+ "WHERE  id_ELO BETWEEN 0.0 AND 100000. "
	//        			+ "AND id_formula='CCH,v=0' "
	//        			+ "AND id_formula='SiO,v=0-10' "
						+ "AND id_formula='AlO-17,v=0' "        			
	        			+ "AND  id_frequency BETWEEN 80000.0 AND 5391000.0  ");
	        	elapsedTime = System.nanoTime() - startTime;
    	        System.out.println("Query time in ms: "
    	                + elapsedTime/1000000);
        	}
        }
        catch (SQLException ex) {
            System.out.println(" SQLException: COULD NOT START DB ");
            ex.printStackTrace();    // could not start db
            return;                   // bye bye
        } catch (Exception ex1) {
            System.out.println(" COULD NOT START DB ");
            ex1.printStackTrace();    // could not start db
            return;                   // bye bye
        }
        
        // TO TESTS USER TABLE CREATION
        /*finaldb.setreadonly(false);
        try {
            // user.dir is the working directory, from where the java vm was launched
            String dbPath = System.getProperty("user.dir") + "/" + "lines";
            finaldb = new DbHSQLDBCreate(dbPath);
            finaldb.queryPrint("SELECT * FROM  TableIndex");
            finaldb = finaldb.CreateUSERtableinDB("/home/smartin/workspace/extradb/");
            finaldb.queryPrint("SELECT * FROM  TableIndex");
            //To check it is created correclty even if repeated
            finaldb = finaldb.CreateUSERtableinDB("/home/smartin/workspace/extradb/");
            finaldb.queryPrint("SELECT * FROM  TableIndex");
        	finaldb.shutdown();
        }
        catch (SQLException ex) {
            System.out.println(" SQLException: COULD NOT START DB ");
            ex.printStackTrace();    // could not start db
            return;                   // bye bye
        } catch (Exception ex1) {
            System.out.println(" COULD NOT START DB ");
            ex1.printStackTrace();    // could not start db
            return;                   // bye bye
        }*/

        
            // finaldb.update("CREATE INDEX CDMSindexTAG ON CDMS (id_TAG)");
            // finaldb.update("CREATE INDEX JPLindexTAG ON JPL (id_TAG)");
            // finaldb.update("CREATE INDEX CDMSindexFRE ON CDMS (id_frequency)");
            // finaldb.update("CREATE INDEX JPLindexFRE ON JPL (id_frequency)");
            
            //finaldb.dbquery("SELECT * FROM  SmallLovas92 WHERE id_frequency BETWEEN 104500 AND 105000 ");
            //finaldb.dbquery("SELECT * FROM  SmallLovas92 ");
            //finaldb.queryPrint("SELECT * FROM  SmallLovas03 ");
            //finaldb.queryPrint("SELECT * FROM  BigLovas ");
            //finaldb.dbquery("SELECT * FROM  JPLcat ");
            //finaldb.dbquery("SELECT * FROM JPL");
            //finaldb.dbquery("SELECT * FROM  CDMScat ");
            //finaldb.dbquery("SELECT * FROM CDMS");
            //finaldb.dbquery("SELECT id_tablelongname FROM  TableIndex WHERE id_tabletype = \"Molecular\"");
        	//finaldb.query("SELECT * FROM CDMS INNER JOIN CDMScat ON CDMS.id_TAG=CDMScat.id_TAG WHERE id_frequency BETWEEN 250000 AND 251000");
        	//finaldb.query("SELECT * FROM CDMS INNER JOIN CDMScat ON CDMS.id_TAG=CDMScat.id_TAG WHERE id_frequency BETWEEN 250000 AND 251000 AND id_formula='Benzyne'");
        	//finaldb.query("SELECT * FROM CDMS INNER JOIN CDMScat ON CDMS.id_TAG=CDMScat.id_TAG WHERE id_formula='Benzyne' AND id_frequency BETWEEN 250000 AND 251000 ");
        	//finaldb.query("SELECT * FROM CDMScat INNER JOIN CDMS ON CDMScat.id_TAG=CDMS.id_TAG WHERE id_formula='Benzyne' AND id_frequency BETWEEN 250000 AND 251000 ");
        	//finaldb.query("SELECT * FROM CDMScat INNER JOIN CDMS ON CDMScat.id_TAG=CDMS.id_TAG WHERE id_frequency BETWEEN 250000 AND 251000 ");

//        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 88000 AND 89000 ");
//        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 88000 AND 89000 ");
        	//finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_formula='SiO' AND id_frequency BETWEEN 10000 AND 251000 ");  //INVIABLE 
        	//finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_formula='SiO' AND id_frequency BETWEEN 10000 AND 251000 ");  //INVIABLE  
        	//finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 80000 AND 90000  AND id_formula='SiO'"); 
        	//finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 80000 AND 90000  AND id_formula='SiO'"); 
        	//finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_frequency BETWEEN 88000 AND 89000 ");  //INVIABLE
        	//finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_frequency BETWEEN 88000 AND 89000 ");  //INVIABLE
        	//finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_formula='SiO' AND id_frequency BETWEEN 10000 AND 251000 ");
//        	finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_formula='SiO' AND id_frequency BETWEEN 10000 AND 251000 ");
//        	finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_formula='SiO' AND id_frequency BETWEEN 10000 AND 251000 ");
//        	finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_frequency BETWEEN 10000 AND 251000  AND id_formula='SiO'");
//        	finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPLcat.id_TAG=JPL.id_TAG WHERE id_frequency BETWEEN 10000 AND 251000  AND id_formula='SiO'");
//        	finaldb.queryPrint("SELECT * FROM  TableIndex");
        	//finaldb.queryPrint("SELECT * FROM  SmallLovas92");
            // at end of program

        	/*System.out.println("Test 1");
        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 92000 AND 92200");
        	System.out.println("Test 2");
        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 92000 AND 92200" +
        			"OR id_frequency BETWEEN 94000 AND 94200");
        	System.out.println("Test 3");
        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE id_frequency BETWEEN 92000 AND 92200" +
        			"OR id_frequency BETWEEN 94000 AND 94200" +
        			"OR id_frequency BETWEEN 98000 AND 98200");
        	System.out.println("Test 4");
        	finaldb.query("SELECT * FROM JPL INNER JOIN JPLcat ON JPL.id_TAG=JPLcat.id_TAG WHERE ( id_frequency BETWEEN 92000 AND 92200" +
        			"OR id_frequency BETWEEN 94000 AND 94200" +
        			"OR id_frequency BETWEEN 98000 AND 98200 )");
        	System.out.println("Test 5");
        	finaldb.query("SELECT * FROM JPLcat INNER JOIN JPL ON JPL.id_TAG=JPLcat.id_TAG WHERE ( id_frequency BETWEEN 92000 AND 92200" +
        			"OR id_frequency BETWEEN 94000 AND 94200" +
        			"OR id_frequency BETWEEN 98000 AND 98200 )");
        	System.out.println("Test 6");*/
        	

	}



}