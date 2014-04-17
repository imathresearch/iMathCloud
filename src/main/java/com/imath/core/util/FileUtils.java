package com.imath.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.Stateful;

import org.apache.commons.io.IOUtils;


/**
 * Class or useful methods for physical files treatment 
 * @author iMath
 *
 */
@Stateful
public class FileUtils {

    /**
     * Generates a zip file 
     * @param outputFile The output zip file name to generate
     * @param fileNames The list of filenames (URI) that must be
     * @throws IOException if some error is produced  
     */
    public void generateZip(String outputFile, List<String> fileNames) throws IOException{
        byte[] buffer = new byte[1024];
        
        URI u = URI.create(outputFile);
        java.nio.file.Path outputFilePath = Paths.get(u.getPath());
        
        FileOutputStream fos = new FileOutputStream(outputFilePath.toString());
        ZipOutputStream zos = new ZipOutputStream(fos);
    
        for(String file : fileNames){
            URI uFile = URI.create(file);
            java.nio.file.Path filePath = Paths.get(uFile.getPath());
            
            ZipEntry ze= new ZipEntry(filePath.getFileName().toString());
            zos.putNextEntry(ze);
            FileInputStream in = new FileInputStream(filePath.toString());
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            in.close();
        }
        zos.closeEntry();
        zos.close();
    }
    
    public void generateGeneralZip(com.imath.core.model.File file, String pathNameZipFile) throws Exception{
         	
        FileOutputStream fos = new FileOutputStream(pathNameZipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
               
        //Create a java File
        URI aux = URI.create(file.getUrl());
    	java.nio.file.Path pathFile = Paths.get(aux.getPath()); 
        File fileToDownload = new File(pathFile.toString());
        addFileToZip("", fileToDownload, zos);
        
        zos.close();
        
    	
    }
    
    public void addFileToZip(String path, File srcFile, ZipOutputStream zos) throws Exception {
		    
    	if (srcFile.isDirectory()){
    		addDirToZip(path, srcFile, zos);
		}
		else{
			byte[] buf = new byte[1024];
		    int len;
		    FileInputStream in = new FileInputStream(srcFile);
		    String relative_path = new String();
		    if(path.equals("")){
		    	relative_path = srcFile.getName();
		    }
		    else{
		    	relative_path = path + "/" + srcFile.getName();
		    }
		    zos.putNextEntry(new ZipEntry(relative_path));
		    while ((len = in.read(buf)) > 0){
		    	zos.write(buf, 0, len);
		    }
		}
    }
	
	public void addDirToZip(String path, File srcDir, ZipOutputStream zos) throws Exception {

		for (String fileName : srcDir.list()){
			if (path.equals("")){
				addFileToZip(srcDir.getName(), new File(srcDir.getPath() + "/" + fileName), zos);
		    } 
		    else{
		    	addFileToZip(path + "/" + srcDir.getName(), new File(srcDir.getPath() + "/" + fileName), zos);
		    }
		}
	}
	
	
    
    /**
     * Writes the content into the specified uri file
     * @param content The array of bytes to be written
     * @param uri The String containing the uri of the destination file
     * @throws IOException if some error is produced  
     */
    public void writeFile(byte[] content, String uri) throws IOException {
 
        URI u = URI.create(uri);
        java.nio.file.Path path = Paths.get(u.getPath());
        
        File file = new File(path.toString());
 
        if (!file.exists()) {
            file.createNewFile();
        }
 
        FileOutputStream fop = new FileOutputStream(file);
 
        fop.write(content);
        fop.flush();
        fop.close();
    }
    
    /**
     * Returns the content bytes coming from the specified InputStream
     * @param inputStream The input stream
     * @return an array of bytes
     * @throws IOException if a reading error is produced
     */
    public byte [] getBytesFromInputStream(InputStream inputStream) throws IOException {
        byte [] bytes = IOUtils.toByteArray(inputStream);
        return bytes;
    }
    
    public String trashFile(com.imath.core.model.File file){
  		
		File trashDirectory = new File(Constants.iMathTRASH);
		String [] trashFiles = trashDirectory.list();
		List <String> listTrashFiles = Arrays.asList(trashFiles);
		
		String fileName = file.getName();
		boolean present = true;
		String trashlocation = new String();
		while(present){
			if(listTrashFiles.contains(fileName)){
				String uid = "_" + UUID.randomUUID().toString();
				fileName = fileName.concat(uid);
			}
			else{
				trashlocation = Constants.iMathTRASH + "/" + fileName;
				/*if(!this.moveFile(file, trashlocation, false)){
					trashlocation = null;
				}*/
				if(!this.moveFile(file.getUrl(), trashlocation)){
					trashlocation = null;
				}
				present = false;
			}
		}
		return trashlocation;
    }
    
    public boolean restoreFile(com.imath.core.model.File file, String trashLocation){
    	
    	/*if(this.moveFile(file, trashLocation, true)){
    		return true;
    	}*/
    	if(this.moveFile(trashLocation, file.getUrl())){
    		return true;
    	}
    	return false;
    }
    
    
    /**
     * Physically move an imath File to a different destination
     * @param file - imath file to be moved
     * @param destination - absolute path where the file is going to be moved
     * @return true is the move is performed, false in the other case
     */
    /*public boolean moveFile(com.imath.core.model.File file, String location, boolean inverse){
    	URI aux = URI.create(file.getUrl());
		java.nio.file.Path path = Paths.get(aux.getPath());
		
		File file_source; 
		File file_destination;
		
		if (!inverse){
			file_source = new File(path.toString());
			file_destination = new File(location);
		}
		else{ // to restore a file in its original location
			file_source = new File(location);
			file_destination = new File(path.toString());			
		}
		
		if(file_source.exists()){
			return file_source.renameTo(file_destination);
		}
		else{
			return false;
		}  
    }
    */
    /**
     * Physically move an imath File to a different destination
     * @param file - imath file to be moved
     * @param destination - absolute path where the file is going to be moved
     * @return true is the move is performed, false in the other case
     */
    public boolean moveFile(String source_location, String destination_location){
    	URI uri_source = URI.create(source_location);
		java.nio.file.Path path_source = Paths.get(uri_source.getPath());
    	
		URI uri_destination = URI.create(destination_location);
		java.nio.file.Path path_destination = Paths.get(uri_destination.getPath());
  	
		File file_source = new File(path_source.toString());
		File file_destination = new File(path_destination.toString());
		
		if(file_source.exists()){
			return file_source.renameTo(file_destination);
		}
		else{
			return false;
		}  
    }
    
    
    /**
     * Physically create a new directory
     * @param String urlDirectory - url of the directory to be created
     * @return true if the directory is created, false in the other case (cannot be created o already exists)
     */
    public boolean createDirectory(String urlDirectory){
    	
    	URI uri_directory = URI.create(urlDirectory);
		java.nio.file.Path path_directory = Paths.get(uri_directory.getPath());
    	
    	File directory = new File(path_directory.toString());
    	
    	return directory.mkdir();
    }
    
    /**
     * Physically create a new file/directory
     * @param String urlParentDir - url of the parent directory
     * @param type - file type (directory or regular file)
     * @return "dir" of the file is a directory, the extension if the file is a regular file, or null if the file cannot be created
     * @throws IOException 
     */
    public String createFile(String urlParentDir, String name, String type) throws IOException{
    	
    	String urlFile = urlParentDir + "/" + name;
    	URI uriFile= URI.create(urlFile);
		java.nio.file.Path pathFile = Paths.get(uriFile.getPath());
    	
    	File file = new File(pathFile.toString());
    	
    	boolean success = false;
    	String typeFile = null;
    	if(type.equals("directory")){
    		success = file.mkdir();
    		if(success)
    			typeFile = "dir";
    	}
    	else{
    		if(type.equals("regular")){  			
				success = file.createNewFile();			
    			if(success){
    				String [] nameParts = name.split("\\.");
    				typeFile = nameParts[nameParts.length-1];
    			}
    		}
    	}
    	
    	return typeFile;
    }
    
    public String getAbsolutePath(String file_url, String userName){
    
    	String [] parts = file_url.split("/");
    	
    	int start_index = 0;
		for(int i = 0; i < parts.length; i++){
			if(parts[i].equals(userName)){
				start_index = i + 1;
				break;
			}
		}
		
		String absolutePath = new String("/");
		
		//No ROOT
		if(start_index != parts.length){
			for(int h = start_index; h < parts.length-1; h++){
				absolutePath = absolutePath.concat(parts[h]);
				absolutePath = absolutePath.concat("/");
			}
		
			absolutePath = absolutePath.concat(parts[parts.length-1]);
		}
		
    	return absolutePath;
    	
    }
    
   
}