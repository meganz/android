package nz.mega.android;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

import android.os.Handler;
import android.webkit.MimeTypeMap;
import android.widget.Toast;


public class MegaProxyServer
{
	/**
	 * HTTP response.
	 * Return one of these from serve().
	 */
	public class Response
	{
		long startFrom;
		long endAt;
		MegaNode node;
		public String status;
		public Properties header = new Properties();
		public String key;
		
		public Response()
		{
			this.status = HTTP_OK;
		}

		public Response( String status, String response )
		{
			this.status = status;
		}
		
		/**
		 * Basic constructor.
		 * @param endAt 
		 * @param startFrom 
		 */
		public Response(MegaNode node, String status,
				String key, long startFrom, long endAt )
		{
			this.node = node;
			this.status = status;
			this.key = key;
			this.startFrom = startFrom;
			this.endAt = endAt;
		}

		/**
		 * Adds given line to the header.
		 */
		public void addHeader( String name, String value )
		{
			header.put( name, value );
		}
	}

	/**
	 * Some HTTP response status codes
	 */
	public static final String
		HTTP_OK = "200 OK",
		HTTP_PARTIALCONTENT = "206 Partial Content",
		HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable",
		HTTP_REDIRECT = "301 Moved Permanently",
		HTTP_NOTMODIFIED = "304 Not Modified",
		HTTP_FORBIDDEN = "403 Forbidden",
		HTTP_NOTFOUND = "404 Not Found",
		HTTP_BADREQUEST = "400 Bad Request",
		HTTP_TOOMANY = "429 Too Many Requests",
		HTTP_INTERNALERROR = "500 Internal Server Error",
		HTTP_NOTIMPLEMENTED = "501 Not Implemented";


	// ==================================================
	// Socket & server code
	// ==================================================

	Handler guiHandler;
	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	ServerSocket myServerSocket;
	Thread myThread;
	
	boolean folderLink = false;
	
	/**
	 * Starts a HTTP server to given port.<p>
	 * Throws an IOException if the socket is already in use
	 */
	public MegaProxyServer( int port, MegaApiAndroid api, MegaApiAndroid apiFolder, MegaApplication app, Handler handler) throws IOException
	{
		this.guiHandler = handler;
		this.app = app;
		this.megaApi = api;
		this.megaApiFolder = apiFolder;
		myServerSocket = new ServerSocket( port );
		myThread = new Thread( new Runnable()
			{
				public void run()
				{					
					try
					{	
						while( true )
							new HTTPSession( myServerSocket.accept());
					}
					catch ( IOException ioe )
					{
						ioe.printStackTrace();
					}
				}
			});
		myThread.setDaemon( true );
		myThread.start();
	}

	/**
	 * Stops the server.
	 */
	public void stop()
	{
		try
		{
			myServerSocket.close();
			myThread.join();
		}
		catch ( IOException ioe ) {}
		catch ( InterruptedException e ) {}
	}


	/**
	 * Handles one session, i.e. parses the HTTP request
	 * and returns the response.
	 */
	private class HTTPSession implements Runnable
	{
		public HTTPSession( Socket s )
		{
			mySocket = s;
			Thread t = new Thread( this );
			t.setDaemon( true );
			t.start();
		}
		
		private InputStream is;
		private BufferedReader in;

		public void run()
		{
			try
			{
				is = mySocket.getInputStream();
				if ( is == null) return;

				
				final int bufsize = 8192;
				byte[] buf = new byte[bufsize];
				int splitbyte = 0;
				int rlen = 0;
				int read = 0;
				
				do
				{
					read = is.read(buf, rlen, bufsize - rlen);
					rlen += read;
					splitbyte = findHeaderEnd(buf, rlen);
					if (splitbyte > 0)
						break;	
				}
				while (read > 0);
				
				
				if(rlen==0)
				{
					sendError( HTTP_BADREQUEST, "");
					return;
				}

				// Create a BufferedReader for parsing the header.
				ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
				BufferedReader hin = new BufferedReader( new InputStreamReader( hbis ));
				Properties pre = new Properties();
				Properties parms = new Properties();
				Properties header = new Properties();

				// Decode the header into parms and header java properties
				decodeHeader(hin, pre, parms, header);
				String method = pre.getProperty("method");
				if((method == null) || 
						((!method.equalsIgnoreCase("GET")) && 
						(!method.equalsIgnoreCase("HEAD"))))
				{
					sendError(HTTP_BADREQUEST, "ERROR: null or invalid method." );
					return;
				}
				
				String uri = pre.getProperty("uri");
				
				long size = 0x7FFFFFFFFFFFFFFFl;
				String contentLength = header.getProperty("content-length");
				if (contentLength != null)
				{
					try { size = Integer.parseInt(contentLength); }
					catch (NumberFormatException ex) {}
				}

				// Write the part of body already read to ByteArrayOutputStream f
				ByteArrayOutputStream f = new ByteArrayOutputStream();
				if (splitbyte < rlen)
					f.write(buf, splitbyte, rlen-splitbyte);

				// While Firefox sends on the first read all the data fitting
				// our buffer, Chrome and Opera send only the headers even if
				// there is data for the body. We do some magic here to find
				// out whether we have already consumed part of body, if we
				// have reached the end of the data to be sent or we should
				// expect the first byte of the body at the next read.
				if (splitbyte < rlen)
					size -= rlen-splitbyte+1;
				else if (splitbyte==0 || size == 0x7FFFFFFFFFFFFFFFl)
					size = 0;

				// Now read all the body and write it to f
				buf = new byte[512];
				while ( rlen >= 0 && size > 0 )
				{
					rlen = is.read(buf, 0, 512);
					size -= rlen;
					if (rlen > 0)
						f.write(buf, 0, rlen);
				}

				// Get the raw body as a byte []
				byte [] fbuf = f.toByteArray();

				// Create a BufferedReader for easily reading it as string.
				ByteArrayInputStream bin = new ByteArrayInputStream(fbuf);
				in = new BufferedReader( new InputStreamReader(bin));

				Response r = serveFile( uri, header, parms);
				if ( r == null )
					sendError( HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response." );
				else
					sendResponse(r, method);
			}
			catch ( IOException ioe )
			{
				try
				{
					showText("Error: " + ioe.getMessage());
					sendError( HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
				}
				catch ( Throwable t ) {}
			}
		}

		/**
		 * Decodes the sent headers and loads the data into
		 * java Properties' key - value pairs
		**/
		private  void decodeHeader(BufferedReader in, Properties pre, Properties parms, Properties header)
		{
			try {
				// Read the request line
				String inLine = in.readLine();
				if (inLine == null)
				{
					sendError( HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html" );
					return;
				}
				
				StringTokenizer st = new StringTokenizer( inLine );
				if ( !st.hasMoreTokens())
				{	
					sendError( HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html" );
					return;
				}
				
				String method = st.nextToken();
				pre.put("method", method);
				if ( !st.hasMoreTokens())
				{
					sendError( HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html" );
					return;
				}
				
				String uri = st.nextToken();
				int qmi = uri.indexOf( '?' );
				if ( qmi >= 0 )
				{
					StringTokenizer tokenizer = new StringTokenizer( uri.substring( qmi+1 ), "&" );
					while ( tokenizer.hasMoreTokens())
					{
						String e = tokenizer.nextToken();
						int sep = e.indexOf( '=' );
						if ( sep >= 0 )
							parms.put( e.substring( 0, sep ).trim(),
								   e.substring( sep+1 ));
					}
					
					uri = uri.substring( 0, qmi );
				}

				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names lowercase since they are
				// case insensitive and vary by client.
				if ( st.hasMoreTokens())
				{
					String line = in.readLine();
					while (line != null && line.trim().length() > 0 )
					{
						int p = line.indexOf( ':' );
						if ( p >= 0 )
							header.put( line.substring(0,p).trim().toLowerCase(Locale.ENGLISH), line.substring(p+1).trim());
						line = in.readLine();
					}
				}

				pre.put("uri", uri);
			}
			catch ( IOException ioe )
			{
				showText("Error: " + ioe.toString());
				sendError( HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
			}
		}
		

		/**
		 * Find byte index separating header from body.
		 * It must be the last byte of the first two sequential new lines.
		**/
		private int findHeaderEnd(final byte[] buf, int rlen)
		{
			int splitbyte = 0;
			while (splitbyte + 3 < rlen)
			{
				if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
					return splitbyte + 4;
				splitbyte++;
			}
			return 0;
		}

		/**
		 * Returns an error message as a HTTP response and
		 */
		private void sendError( String status, String msg )
		{
			try
			{
				if ( status == null )
					throw new Error( "sendResponse(): Status can't be null." );

				final OutputStream out = mySocket.getOutputStream();
				PrintWriter pw = new PrintWriter( out );
				pw.print("HTTP/1.0 " + status + " \r\n");
				pw.print("Content-Type: text/plain\r\n");
				pw.print( "Date: " + gmtFrmt.format( new Date()) + "\r\n");
				pw.print("\r\n");
				pw.flush();
				
				out.flush();
				out.close();
				mySocket.close();
			}
			catch( IOException ioe )
			{
				try { mySocket.close(); } catch( Throwable t ) {}
			}
		}
		
		class MegaStreamReader implements MegaTransferListenerInterface
		{
			StreamingBuffer streamingBuffer = null;
			
			public MegaStreamReader(StreamingBuffer streamingBuffer)
			{
				this.streamingBuffer = streamingBuffer;
			}
			
			@Override
			public void onTransferStart(MegaApiJava api, MegaTransfer transfer)
			{
				System.out.println("Transfer start");	
			}

			@Override
			public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e)
			{
				streamingBuffer.flush();
				if(e.getErrorCode() != MegaError.API_OK)
					streamingBuffer.setErrorBit();
				
				System.out.println("Transfer finish");
			}

			@Override
			public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer)
			{
				//System.out.println("Transfer update: Received " + transfer.getTransferredBytes() + " of " + transfer.getTotalBytes());
			}

			@Override
			public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e)
			{
				System.out.println("Temporary Error");
			}

			@Override
			public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] data)
			{
				//System.out.println("Received " + data.length + " bytes");
				if(streamingBuffer.isAborted() || (streamingBuffer.write(data) != data.length))
				{
					if(streamingBuffer.isAborted())
						System.out.println("ABORTED");
					else
						System.out.println("PIPE FULL");
					System.out.println("STOPPING TRANSFER");
					return false;
				}
				
				return true;
			}
		}
		
		/**
		 * Sends given response to the socket.
		 * @param method 
		 */
		private void sendResponse(final Response response, String method)
		{
			if(response.node == null) 
			{
				try { mySocket.close(); } catch( Throwable t ) {}
				return;
			}
			
			try
			{
				if (response.status == null )
					throw new Error( "sendResponse(): Status can't be null." );
				
				final OutputStream o = mySocket.getOutputStream();
				PrintWriter pw = new PrintWriter( o );
				pw.print("HTTP/1.0 " + response.status + " \r\n");

				// Get MIME type from file name extension, if possible
				String mime = null;
				String fileName = response.node.getName();
				int extensionIndex = fileName.lastIndexOf( '.' );
				if (extensionIndex >= 0)
					mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(extensionIndex + 1 ).toLowerCase(Locale.ENGLISH));
				if ( mime == null )
					mime = "video/x-msvideo";
				pw.print("Content-Type: " + mime + "\r\n");

				if (response.header == null || response.header.getProperty( "Date" ) == null )
					pw.print( "Date: " + gmtFrmt.format( new Date()) + "\r\n");

				if (response.header != null )
				{
					Enumeration<Object> e = response.header.keys();
					while (e.hasMoreElements())
					{
						String key = e.nextElement().toString();
						String value = response.header.getProperty( key );
						pw.print( key + ": " + value + "\r\n");
						System.out.println("Response-> " + key + ": " + value);
					}
				}
				pw.print("\r\n");
				pw.flush();
				o.flush();
				
				long totalBytes = response.endAt-response.startFrom+1;
				long writtenBytes = 0;							

				StreamingBuffer streamingBuffer = new StreamingBuffer();
				streamingBuffer.setErrorBit();
				while(writtenBytes < totalBytes)
				{
					byte[] inputBuffer = streamingBuffer.read();
					if(inputBuffer == null)
					{
						System.out.println("Input pipe closed. Opening a new one");
						streamingBuffer.resetErrorBit();
						if (folderLink){
							megaApiFolder.startStreaming(response.node, response.startFrom + writtenBytes, 
									totalBytes - writtenBytes, new MegaStreamReader(streamingBuffer));	
						}
						else{
							megaApi.startStreaming(response.node, response.startFrom + writtenBytes, 
									totalBytes - writtenBytes, new MegaStreamReader(streamingBuffer));	
						}
						
						continue;
					}
					
					//System.out.println("Readed from buffer: " + inputBuffer.length + " bytes");
					try { o.write(inputBuffer); } 
					catch(Exception e) 
					{
						System.out.println("SOCKET CLOSED. ABORTING");
						streamingBuffer.abort();
						break;
					}
					
					writtenBytes += inputBuffer.length;
					//System.out.println("Written to socket " + writtenBytes + " of "  + totalBytes + " bytes");
				}
				
				System.out.println("Closing connection");
				try {
					o.flush();
					o.close();
					in.close();
					is.close();
				} catch (IOException ex) {
				}
				
				try { mySocket.close(); } catch( Throwable t ) {}
			}
			catch( IOException ioe )
			{
				ioe.printStackTrace();
				try { mySocket.close(); } catch( Throwable t ) {}
			}
		}

		private Socket mySocket;
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only).
	 * Uses only URI, ignores all headers and HTTP parameters.
	 */
	public Response serveFile( String uri, Properties header, Properties params)
	{
		Response res = null;
		String handler = null;
		String key = null;
		String fileName = null;

		// Remove URL arguments
		if ( uri.indexOf( '?' ) >= 0 )
			uri = uri.substring(0, uri.indexOf( '?' ));
		
		if(uri.charAt(0)=='/')
			uri = uri.substring(1);
		
		int indexFilename = uri.lastIndexOf('/')+1;
		if((indexFilename!=0) && (indexFilename<uri.length()))
		{
			fileName = uri.substring(indexFilename);
			uri = uri.substring(0, indexFilename-1);
		}
		else
		{
			return new Response( HTTP_FORBIDDEN,
					"Forbidden." );
		}
		
		String[] parts = uri.split("!");               	
    	if((parts.length == 0) || (parts.length>2))
    	{
    		return new Response( HTTP_FORBIDDEN,
    						"Forbidden." );
    	}		    
    	
    	handler = parts[0];	
    	if(parts.length == 2)
    	{
    		key = parts[1]; 
    		if(key.length() < 43)
    		{
				showText("Invalid link");
    			return new Response( HTTP_NOTFOUND,
    				"Error 404, file not found." );
    		}
    	}
 
		
		MegaNode node = megaApi.getNodeByHandle(MegaApiAndroid.base64ToHandle(handler));
		if(node == null){
			node = megaApiFolder.getNodeByHandle(MegaApiAndroid.base64ToHandle(handler));
			if (node == null){
				showText("File not found");
				return new Response( HTTP_NOTFOUND,
	    				"Error 404, file not found." );
			}
			else{
				folderLink = true;
			}
		}
		else{
			folderLink = false;
		}
				
		try { fileName = URLDecoder.decode(fileName, "UTF-8"); } 
		catch (UnsupportedEncodingException e) {}
		String nodeName = node.getName();
		
		if(fileName.compareTo(nodeName)!=0)
		{
			//Support for subtitles
			int extensionIndex = nodeName.lastIndexOf('.');
			if(extensionIndex < 0)
				return new Response( HTTP_NOTFOUND, "Error 404, file not found." );

			if (fileName.length() <= extensionIndex){
				return new Response( HTTP_NOTFOUND, "Error 404, file not found." );
			}
			
			String baseName = fileName.substring(0, extensionIndex+1);
			if (folderLink){
				node = megaApiFolder.getChildNode(megaApiFolder.getParentNode(node), fileName);
			}
			else{
				node = megaApi.getChildNode(megaApi.getParentNode(node), fileName);
			}
			
			if((node == null) || !node.getName().startsWith(baseName))
				return new Response( HTTP_NOTFOUND, "Error 404, file not found." );
			showText("SUBTITLE found!");
		}

		// Calculate etag
		String etag = handler;

		// Support (simple) skipping:
		long startFrom = 0;
		long endAt = -1;
		String range = header.getProperty( "range" );
		if ( range != null )
		{
			if ( range.startsWith( "bytes=" ))
			{
				range = range.substring( "bytes=".length());
				int minus = range.indexOf( '-' );
				try {
					if ( minus > 0 )
					{
						startFrom = Long.parseLong( range.substring( 0, minus ));
						endAt = Long.parseLong( range.substring( minus+1 ));
					}
				}
				catch ( NumberFormatException nfe ) {
				}
			}
		}

		// Change return code and add Content-Range header when skipping is requested
		long fileLen = node.getSize();
		if (range != null && startFrom >= 0)
		{
			if ( startFrom >= fileLen)
			{
				res = new Response( HTTP_RANGE_NOT_SATISFIABLE, "" );
				res.addHeader( "Content-Range", "bytes 0-0/" + fileLen);
				res.addHeader( "ETag", etag);
			}
			else
			{
				if ( endAt < 0 )
					endAt = fileLen-1;
				long newLen = endAt - startFrom + 1;
				if ( newLen < 0 ) newLen = 0;

				final long dataLen = newLen;
				res = new Response( node, HTTP_PARTIALCONTENT, key, startFrom,  endAt);
				res.addHeader( "Content-Length", "" + dataLen);
				res.addHeader( "Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
				res.addHeader( "ETag", etag);
			}
		}
		else
		{
			if (etag.equals(header.getProperty("if-none-match")))
				res = new Response( HTTP_NOTMODIFIED, "");
			else
			{
				res = new Response( node, HTTP_OK, key, 0, fileLen-1);
				res.addHeader( "Content-Length", "" + fileLen);
				res.addHeader( "ETag", etag);
			}
		}
		
		res.addHeader( "Accept-Ranges", "bytes");
		return res;
	}

	
	public void showText(String text)
	{
		final String message = text;
		
		guiHandler.post(new Runnable() {
			public void run() {
	
			Toast.makeText(app, 
					message, 
					Toast.LENGTH_LONG).show();
			}
		});
	}
	
	/**
	 * GMT date formatter
	 */
	private static java.text.SimpleDateFormat gmtFrmt;
	static
	{
		gmtFrmt = new java.text.SimpleDateFormat( "E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	};
}

