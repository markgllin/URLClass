package urlrequest;
/**
 * UrlCache Class
 * 
 * @author 	Majid Ghaderi
 * @version	1.1, Sep 30, 2016
 *
 */
import java.util.HashMap;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class UrlCache {

    private static HashMap<String,String> catalog;

    public static void main(String[] args){
        try{
            UrlCache test = new UrlCache();
            test.getObject("people.ucalgary.ca/~mghaderi/index.html");
        }catch(UrlCacheException e){
            e.printStackTrace();
        }

    }

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException {

		File cacheDir = new File("cache");
        File catalogFile = new File("cache/catalog");

        //create cache directory
        if(!cacheDir.isDirectory()) cacheDir.mkdir();

        //create/read catalog file
        try{
            catalogFile.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader("cache/catalog"));
            String line;
            
            catalog = new HashMap<String, String>();

            while((line = br.readLine()) != null){
                String[] parts = line.split("_");
                catalog.put(parts[0], parts[1]);
            }

        }catch(IOException e){
            e.printStackTrace();
        }

        
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException {
        String output;
        int HOST = 0;
        int PORT = 1;
        int REQ = 2;

        String[] parsedUrl = parseUrl(url);

        try{
            //open input+output streams
            Socket request = new Socket(parsedUrl[HOST], Integer.parseInt(parsedUrl[PORT]));
            PrintWriter out = new PrintWriter(request.getOutputStream(), true);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            //send http request to server
            out.println(parsedUrl[REQ]);

            //create directory structuce for objects to be downloaded
            File urlObject = new File("cache/"+url);
            File parent = urlObject.getParentFile();
            if(!parent.exists() && !parent.mkdirs()){
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }

            urlObject.createNewFile();

            PrintWriter urlOutStream = new PrintWriter(
                    new BufferedWriter(
                        new FileWriter("cache/"+url, true)));
 
            //write response from server to file
            while((output = in.readLine()) != null){
                urlOutStream.println(output);
            }
                
        
            urlOutStream.close();
            catalog.put(url, parseLastModified(url)); // change to date

        }catch(NumberFormatException | IOException e){
            e.printStackTrace();
        }
	}

    public String[] parseUrl(String url){
        String host, filepath, fullRequest, lastModified;
        String port = "80";

        //parse for filepath
		String[] parts = url.split("/", 2);
        filepath = "/" + parts[1];

        //check if port # provided
        if (parts[0].contains(":")){
            String[] hostAndPort = parts[0].split(":", 2);
            host = hostAndPort[0].trim();
            port = hostAndPort[1].trim();
        }else{
            host = parts[0].trim();
        }

        //get last modified date
        try{
            lastModified = catalog.get(url); 
        }catch (UrlCacheException | NullPointerException e){
            lastModified = "Thu, 01 Jan 1970 00:00:00 UTC";
        }

        //build http request
        fullRequest = "GET " + filepath + " HTTP/1.1\r\n"
                        + "Host: " + host + ":" + port + "\r\n"
                        + "If-Modified-Since: " + lastModified + "\r\n"
                        + "\r\n";
        
        return new String[] {host, port, fullRequest};
    }
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException {
        try{
            return new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz").parse(catalog.get(url)).getTime();
        }catch(ParseException e){
            throw new UrlCacheException();
        }
	}

    public String parseLastModified(String url){
        String line;

        try{
            BufferedReader br = new BufferedReader(new FileReader("cache/" + url));
            while((line = br.readLine()) != null){
                if(line.contains("Last-Modified:")){
                    String[] parts = line.split(":", 2);
                    return parts[1].trim();
                }
            }
        }catch(IOException e){
            System.out.println("incorrect path");
        }

        return null;
    }

}
