package mega.privacy.android.app;

import java.util.Arrays;

/**
 * 
 * @author fjaviersr
 *
 * Circular buffer with adaptative size, non blocking input and buffered and blocking output
 * It's used too to send control signals between the SDK and the HTTP proxy server 
 * (abort, buffer full, error, flush)
 */
public class StreamingBuffer
{
	byte[] buffer;
	int in;
	int out;
	int outputBufferLength;
	int availableData;
	boolean error;
	boolean flush;
	boolean aborted;
	
	final static int DEFAULT_MAX_BUFFER_SIZE = 1048576;
	final static int DEFAULT_BUFFER_SIZE = 16384;
	final static int DEFAULT_OUTPUT_LENGTH = 8192;
	
	StreamingBuffer()
	{
		buffer = new byte[DEFAULT_BUFFER_SIZE];
		outputBufferLength = DEFAULT_OUTPUT_LENGTH;
		in = out = availableData = 0;
		error = false;
		flush = false;
		aborted = false;
	}
	
	boolean isAborted()
	{
		return aborted;
	}
	
	void abort()
	{
		aborted = true;
	}
	
	boolean getErrorBit()
	{
		return error;
	}
	
	void resetErrorBit()
	{
		error = false;
	}
	
	synchronized void setErrorBit()
	{
		error = true;
		notify();
	}
	
	synchronized void flush()
	{
		flush = true;
		notify();	
	}
	
	int getAvailableData()
	{
		return availableData;
	}
	
	synchronized byte[] read()
	{
		int currentIndex;
		int remaining;
		int readSize;

		if(flush && (availableData == 0))
			flush = false;
		
		while(!flush && (availableData < outputBufferLength))
		{
			if(error) 
				return null;
			
			try { wait(); }
			catch (InterruptedException e) {}
		}
			
		readSize = outputBufferLength;
		if(flush && availableData < outputBufferLength)
		{
			readSize = availableData;
			flush = false;
		}
		
		availableData -= readSize;
		currentIndex = out;
		out += readSize;
		remaining = out - buffer.length;
		if(remaining >= 0)
			out = remaining;
		
		byte[] outputBuffer = Arrays.copyOfRange(buffer, currentIndex, currentIndex+readSize);
		if(remaining > 0)
			System.arraycopy(buffer, 0, outputBuffer, readSize-remaining, remaining);
		return outputBuffer;
	}
	
	synchronized public int write(byte[] data)
	{
		int currentIndex;
		int remaining;
		int inputSize;
		
		int availableSpace = buffer.length-availableData;
		inputSize = data.length;
		while(availableSpace < data.length)
		{
			if((buffer.length * 2) <= DEFAULT_MAX_BUFFER_SIZE)
			{
				currentIndex = out;
				out += availableData;
				remaining = out - buffer.length;
				byte[] newBuffer = 	Arrays.copyOfRange(buffer, currentIndex, currentIndex+buffer.length*2);
				if(remaining > 0)
					System.arraycopy(buffer, 0, newBuffer, availableData-remaining, remaining);
				
				out = 0;
				in = availableData;
				buffer = newBuffer;
				availableSpace = buffer.length-availableData;
				continue;
			}
			
			if(availableSpace < data.length)
			{
				inputSize = availableSpace;
				error = true;
				break;
			}
		}

		availableData += inputSize;
		currentIndex = in;
		in += inputSize;
		remaining = in - buffer.length;
		if(remaining >= 0)
			in = remaining;
				
		if(remaining <= 0)
		{	
			System.arraycopy(data, 0, buffer, currentIndex, inputSize);
		}
		else
		{
			int num = inputSize-remaining;
			System.arraycopy(data, 0, buffer, currentIndex, num);
			System.arraycopy(data, num, buffer, 0, remaining);
		}
			
		notify();
		return inputSize;
	}
}
