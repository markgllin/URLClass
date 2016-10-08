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

/*		File cacheDir = new File("cache");

        //create cache directory
        if(!cacheDir.isDirectory()) cacheDir.mkdir();

        try{
            PrintWriter catalog = 
                new BufferedWriter(
                    new FileWriter("cache/catalog", true));
        }catch(IOException e){
            e.printStackTrace();
        }
*/
        catalog = new HashMap();
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
            
            //read response from server
            while((output = in.readLine()) != null)
                System.out.println(output);
        
        }catch(NumberFormatException | IOException e){
            e.printStackTrace();
        }
	}

    public String[] parseUrl(String url){
        String host, filepath;
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

        //build http request
        String fullRequest = "GET " + filepath + " HTTP/1.1\r\n"
                            + "Host: " + host + ":" + port + "\r\n"
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
		return -1;
	}

}
