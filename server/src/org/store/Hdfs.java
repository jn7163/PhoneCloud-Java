package org.store; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException; 
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList; 
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path; 
import org.apache.hadoop.io.IOUtils; 
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Text; 
import org.bean.MyFileInfo;
/**
 * Hdfs操作类，包括上传文件、获取文件等待。
 * @author xqs
 * 
 */
public class Hdfs {
    private Configuration conf;
    private FileSystem fs;
    private FileSystem localFS;
    private Path mapFile; 
	public Hdfs(){
		conf=new Configuration();
		conf.set("fs.default.name", "hdfs://"+Constants.HostName+":9000");
		
		try {
			fs=FileSystem.get(conf);
			localFS = FileSystem.getLocal(conf);  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void addSmallFile(String mapFilePath,String localPath,ArrayList<MyFileInfo> list){
		try{   
			mapFile=new Path(mapFilePath);
			if(!fs.exists(mapFile)){
				fs.mkdirs(mapFile);
			} 
			MapFile.Writer writer=new MapFile.Writer(conf,fs,mapFile.toString(),Text.class,Text.class); 
			String filePath;
			File file;
			for(MyFileInfo fi:list){
				filePath=localPath+"/"+fi.getFileId(); 
				byte[] data=Utils.file2byte(filePath);   
				System.out.println(data.length);
				writer.append(new Text(fi.getFileId()), new Text(data));
				System.out.println(fi.getFileId()+" 成功上传到HDFS MapFile中");
				file=new File(filePath);
				if(file.exists()){
					file.delete();
				}
			} 
			IOUtils.closeStream(writer);//关闭write流 
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public boolean getSmallFile(String mapFilePath,String clientId,String fileId,String fileName) {
		
		
		HBaseUtil hu=new HBaseUtil();
		String val=hu.getValue(clientId+fileId, Constants.Family, "fileuploaded");
		System.out.println("uploaded: "+val);
		if(val!=null&&val!=""){
			if(val.equals("yes")){
				return getSmallFileFromHdfs(mapFilePath,fileId,fileName);
			}else{
				return getSmallFileFromLocal(fileId,fileName);
			}
		}else{
			return false;
		}
	}
	
	public boolean getSmallFileFromHdfs(String mapFilePath,String fileId,String fileName){
		try{
			mapFile=new Path(mapFilePath);
			try{
				if(!fs.exists(mapFile)){
					return false;
				} 
			}catch(Exception e){
				e.printStackTrace();
			}
			MapFile.Reader reader=new MapFile.Reader(fs,mapFile.toString(),conf);                
			Text fk=new Text();
			reader.finalKey(fk);   
			Text val=new Text();
			reader.get(new Text(fileId),val);  
			Utils.byte2File(val.getBytes(), Constants.LocalDownloadDir+fileId); 
			IOUtils.closeStream(reader);//close reader
			System.out.println("获取小文件"+fileName+"成功");
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean getSmallFileFromLocal(String fileId,String fileName){
		try{
		    InputStream in=new FileInputStream(Constants.LocalUploadDir+fileId); 
		    OutputStream out=new FileOutputStream(Constants.LocalDownloadDir+fileId);  
		    IOUtils.copyBytes(in, out, conf);
		    if(in!=null){
				 in.close();
			 }
			 if(out!=null){
				 out.close();
			 }
			 System.out.println("获取小文件"+fileName+"成功");
			 return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		 
	}
	
	public boolean addBigFile(String fileId,String filePath){ 
		Path hdfsDir = new Path(Constants.HdfsDir);
		Path localDir = new Path(filePath);
		File file;
		try{
			if(!fs.exists(hdfsDir)){ 
				fs.mkdirs(hdfsDir);
			}
			FSDataInputStream in = localFS.open(localDir);
			FSDataOutputStream out = fs.create(new Path(Constants.HdfsDir+fileId));
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			if(out!=null){
				out.close();
				out=null;
			}
			if(in!=null){
				in.close();
				in=null;
			} 
			//delete the temp file
			file=new File(filePath);
			if(file.exists()){
				file.delete();
			}
			System.out.println(fileId+" 成功上传到HDFS");
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean getBigFile(String fileId,String fileName){
		try{
			 InputStream in=fs.open(new Path(Constants.HdfsDir+fileId));  
			 OutputStream out=new FileOutputStream(Constants.LocalDownloadDir+fileId);  
			 IOUtils.copyBytes(in, out, conf);  
			 if(in!=null){
				 in.close();
			 }
			 if(out!=null){
				 out.close();
			 }
			 System.out.println("获取大文件"+fileName+"成功");
			 return true;
		}catch(Exception e){
			 e.printStackTrace();
			 return false;
		}
		

	}
}
