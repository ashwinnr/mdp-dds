package util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

public interface CommandLineOptionable< T >  {
	//this class needs to tell how to create options and parse arg
	//and how to retrieve them and return constructed object
	public Options createOptions();
	public CommandLine parseOptions( String[] args, Options opts );
	public T instantiateMe( String[] args );
	
	
}
