package MyApp.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RFID {
	//Used to check if valid floor is inputed
	
	
	public String getFloorById(final String id){
		String floor = "na";
		try{
     	   final File database = new File("etc/RFID_DB");
     	   try (final BufferedReader br = new BufferedReader(new FileReader(database))) {
     		   String line;
     		   // Read from the database file
     		   // unless content matches data to get the floor 
     		   while ((line = br.readLine()) != null) {
     			   if (line.contains(id)) {
     				   if(line.split(",")[0].contentEquals(id))
     					   floor = line.split(",")[1];
     				   break;
     			   }
     		   }
     	   }
           }catch (final Exception ex){
        		  System.out.println("File not found");
           }     
		System.out.println(floor);
		return floor;
	}
	
	public ArrayList<String> getAllTheId(){
		final ArrayList<String> id = new ArrayList<>();
		
		try{
	     	   final File database = new File("etc/RFID_DB");
	     	   try (final BufferedReader br = new BufferedReader(new FileReader(database))) {
	     		   String line;
	     		   int count = 0;
	     		   // Read from the database file
	     		   // Get all the RFID
	     		   while ((line = br.readLine()) != null) {
	     			   if(count != 0){
	     				  id.add(line.split(",")[0]);
	     			   }
	     			   count++;
	     		   }
	     	   }
	           }catch (final Exception ex){
	        		  System.out.println("File not found");
	           }     
		
		return id;
	}
	
	public void insertData(final String data){
		//Append the data in the RFID_DB
        try(final FileWriter fw = new FileWriter("etc/RFID_DB", true);
        	    final BufferedWriter bw = new BufferedWriter(fw);
        	    final PrintWriter out = new PrintWriter(bw))
        	{ 
        	    out.println(data);
        	    System.out.println("Insert data sucessfully");
        	} catch (final IOException ex) {
        	    System.out.println("Data cannot insert to the databse.");
        	}
	}
	
	public void updateData(final String id, final String data){
		try{
     	   final File originalFile = new File("etc/RFID_DB");
     	   final File tempFile = new File("etc/myTempFile");
     	   try (final BufferedReader br = new BufferedReader(new FileReader(originalFile));
     			    final PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {
     		   String line;
     		   // Read from the original file and write to the new
     		   // unless content matches data to be removed.
     		   while ((line = br.readLine()) != null) {
     			   if (line.contains(id)) {
     				   line = data;
     			   }
     			   pw.println(line);
     			   pw.flush();
     		   }
     		   System.out.println("Update data sucessfully");
     	   }
     	   // Delete the original file
     	   if (!originalFile.delete()) {
     		   System.out.println("Could not delete file");
     		   return;
     	   }
     	   // Rename the new file to the filename the original file had.
     	   if (!tempFile.renameTo(originalFile))
     		   System.out.println("Could not rename file");
        		}catch (final Exception ex){
        		   System.out.println("Update error");
        		} 
	}
	
	public void deleteData(final String data){
		 //Delete data in the RFID_DB 
        final File inputFile = new File("etc/RFID_DB");
        final File tempFile = new File("etc/myTempFile");

        try{
        try (final BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        		final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
        	final String lineToRemove = data;
        	String currentLine;

        	while((currentLine = reader.readLine()) != null) {
        		// trim newline when comparing with lineToRemove
        		final String trimmedLine = currentLine.trim();
        		if(!trimmedLine.equals(lineToRemove)) {
        			writer.write(currentLine + System.getProperty("line.separator"));
        		}
        	}
        }
        final boolean successful = tempFile.renameTo(inputFile);
        System.out.println("Delete data " + successful);
        }catch (final Exception ex){
        	System.out.println("Delete Error");
        }
	}
	
	public void backUp() throws IOException{
		final Path FROM = Paths.get("etc/RFID_DB");
	    final Path TO = Paths.get("etc/RFID_Backup");
	  //overwrite existing file, if exists
	    final CopyOption[] options = new CopyOption[]{
	      StandardCopyOption.REPLACE_EXISTING,
	      StandardCopyOption.COPY_ATTRIBUTES
	    }; 
		Files.copy(FROM,TO,options);
	}
}