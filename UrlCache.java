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

    private static HashMap<String,String> catalog = new HashMap<String, String>();
	public static void main(String[] args) {

		// include whatever URL you like
		// these are just some samples
		String[] url = {"people.ucalgary.ca/~mghaderi/index.html",
						"people.ucalgary.ca/~mghaderi/test/uc.gif",
						"people.ucalgary.ca/~mghaderi/test/a.pdf",
						"people.ucalgary.ca:80/~mghaderi/test/test.html"};
		
		// this is a very basic tester
		// the TAs will use a more comprehensive set of tests
		try {
			UrlCache cache = new UrlCache();
			
			for (int i = 0; i < url.length; i++)
				cache.getObject(url[i]);
			
			System.out.println("Last-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]));
			cache.getObject(url[0]);
			System.out.println("Last-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]));
		}
		catch (UrlCacheException e) {
			System.out.println("There was a problem: " + e.getMessage());
		}
	}

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException {
        String date, line;
		File cacheDir = new File("cache");
        File catalogFile = new File("cache/catalog");

        //create cache directory
        if(!cacheDir.isDirectory()) cacheDir.mkdir();

        //create/read catalog file
        try{
            catalogFile.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader("cache/catalog"));
            
            //read cached data into hashmap
            while((line = br.readLine()) != null){
                String[] parts = line.split("_");
                date = catalog.get(parts[0]);

                //update object with newest date if multiple dates for one object present
                if (date==null || convertDateToLong(date) < convertDateToLong(parts[1]))
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
        int HOST = 0;
        int PORT = 1;
        int REQ = 2;
        String date = "Thu, 01 Jan 1970 00:00:00 UTC";

        String[] parsedUrl = parseUrl(url);

        try{
            //open input+output streams
            Socket request = new Socket(parsedUrl[HOST], Integer.parseInt(parsedUrl[PORT]));
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            //send http request to server
            request.getOutputStream().write(parsedUrl[REQ].getBytes("US-ASCII"));
            request.getOutputStream().flush();

            //create directory structuce for objects to be downloaded
            File urlObject = new File("cache/"+url);
            File parent = urlObject.getParentFile();
            if(!parent.exists() && !parent.mkdirs())
                throw new IllegalStateException("Couldn't create dir: " + parent);

            //check html header to see if cached data is up to date
            String output;
            String header = "";

            while((output = in.readLine()) != null){
                header += output + "\r\n";

                if(output.contains("Last-Modified: ")){
                    String[] parts = output.split(":", 2);
                    date =  parts[1].trim();      
                }
                if(header.contains("\r\n\r\n"))
                    break;
            }

            if(!header.contains("304 Not Modified")){
                //delete old and/or create new file if data not up to date or non-existent
                urlObject.delete();
                urlObject.createNewFile();

                PrintWriter urlOutStream = new PrintWriter(
                    new BufferedWriter(
                        new FileWriter("cache/"+url, true)));
            
                //write response from server to file
                while((output = in.readLine()) != null)
                    urlOutStream.println(output);
                
                urlOutStream.close();

                //update hashmap and catalog
                catalog.put(url, date);

                PrintWriter catalogOutstream = new PrintWriter(
                        new BufferedWriter(
                            new FileWriter("cache/catalog", true)));

                catalogOutstream.println(url + "_"+ date);
                catalogOutstream.close();
            }

        }catch(NumberFormatException | IOException e){
            e.printStackTrace();
        }
	}

    /**
     *
     * Returns a String array containing a parsed url. The array is separated into the host,
     * port, and the full http request (in that order).
     *
     * @params url   url to be parsed
     * @returns a string array containing the host name, port number, and http request (in that order)
     */
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
        lastModified = catalog.get(url); 
        
        if(lastModified == null)
            lastModified = "Thu, 01 Jan 1970 00:00:00 UTC";

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

    /**
     * Returns time in milliseconds from a date in the format of 'EEE, dd MMM yyyy hh:mm:ss zzz.'
     *
     * @param date   date in the format specified above as a String
     * @returns time in milliseconds as in Date.getTime()
     * @returns 0 if date cannot be converted
     */
    public long convertDateToLong(String date){
        try{
            return new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz").parse(date).getTime();
        }catch(ParseException e){
            return 0;
        }
    }

    /**
     * Returns date in the form of a string after parsing it from the local file structure
     * specified by 'url'.
     * 
     * @param url   urlpath in local cache that points to the url object
     * @returns time in the format 'EEE, dd MMM yyyy hh:mm:ss zzz'
     * @returns null if date cannot be found in file or if IOException occurs
     */
    public String parseLastModified(String url){
        String line;

        try{
            //read file and parse for last-modified date
            BufferedReader br = new BufferedReader(new FileReader("cache/" + url));
            while((line = br.readLine()) != null){
                if(line.contains("Last-Modified:")){
                    System.out.println(line);
                    String[] parts = line.split(":", 2);
                    return parts[1].trim();
                }
            }

            br.close();

        }catch(IOException e){
            System.out.println("incorrect path");
        }

        return null;
    }

}