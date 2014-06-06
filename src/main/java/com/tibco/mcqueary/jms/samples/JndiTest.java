package com.tibco.mcqueary.jms.samples;

import java.util.*;
import javax.jms.*;
import javax.naming.*;

public class JndiTest {

	JndiTest() {
		try {
			// Create an InitialContext with the no-arg constructor. You use this when
			// you can't or don't want to hard-code vendor-specific properties, but instead
			// wish to take advantage of System properties or a jndi.properties file, which
			// will be loaded automatically if found on your classpath.
			// http://docs.oracle.com/javase/7/docs/api/javax/naming/InitialContext.html

			InitialContext ctx = new InitialContext();
			System.out.println("SUCCESS: Created InitialContext");
			
			ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");
			if (factory != null)
				System.out.println("SUCCESS: Looked up ConnectionFactory");
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			System.err.println("Naming Exception:");
			e.printStackTrace();
		}	
	}

	public static void main(String[] args) {
		new JndiTest();
	}
}
