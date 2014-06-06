/* 
 * Copyright (c) TIBCO Software Inc. 
 * All rights reserved.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 * 
 */

/*
 * This is a simple sample of a basic asynchronous
 * JMS echo service.
 *
 * This sample subscribes to specified destination and
 * receives and replies to all received messages with an echo
 * of their message body (assumes a text message).
 *
 * Notice that the specified destination should exist in your configuration
 * or your topics/queues configuration file should allow
 * creation of the specified destination.
 *
 * Usage:  java JmsEchoService [options]
 *
 *    where options are:
 *
 *      -server     Provider URL.
 *                  If not specified this sample assumes a
 *                  providerUrl of "tibjmsnaming://localhost:7222"
 *
 *      -user       User name. Default is null.
 *      -password   User password. Default is null.
 *      -dest       Destination name. Default is "queue.sample"
 *
 */

import java.io.*;
import java.util.*;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

public class JmsEchoService
       implements ExceptionListener, MessageListener
{

//	static final String  defaultProviderContextFactory =
//            "com.tibco.tibjms.naming.TibjmsInitialContextFactory";
//
//    static final String  defaultProviderURL =
//                            "tibjmsnaming://localhost:7222";

    /*-----------------------------------------------------------------------
     * Parameters
     *----------------------------------------------------------------------*/

    String    providerUrl   = null;
    String	  providerContextFactory = null;
    String    userName    = null;
    String    password    = null;
    String    name        = "queue.sample";
    
    /*-----------------------------------------------------------------------
     * Variables
     *----------------------------------------------------------------------*/
    Connection      connection  = null;
    Session         session     = null;
    MessageConsumer msgConsumer = null;
	MessageProducer msgProducer = null;
    Destination     destination = null;


    public JmsEchoService(String[] args)
    {
    	String propFileName = "jndi.properties";
    	Properties props = new Properties();
    	props.putAll(System.getProperties());
    	props.putAll(readPropertyFileFromClasspath(propFileName));
    	
        parseArgs(args);

//    	System.err.println("Loaded JNDI properties from " + propFileName);
    	
//    	providerContextFactory = props.getProperty(Context.INITIAL_CONTEXT_FACTORY);
//    	providerUrl = props.getProperty(Context.PROVIDER_URL);
//    	userName = props.getProperty(Context.SECURITY_PRINCIPAL);
//    	password = props.getProperty(Context.SECURITY_CREDENTIALS);

        try
        {
            /*
            * Init JNDI Context.
            */
            
//           Hashtable<String,String> env = new Hashtable<String,String>();
//           env.put(Context.INITIAL_CONTEXT_FACTORY, providerContextFactory);
//           env.put(Context.PROVIDER_URL, providerUrl);
//
//           if (userName != null)
//           {
//              env.put(Context.SECURITY_PRINCIPAL, userName);
//
//              if (password != null)
//                 env.put(Context.SECURITY_CREDENTIALS, password);
//           }
//
//           InitialContext jndiContext = new InitialContext(env);
          InitialContext ctx = new InitialContext();

          providerUrl = props.getProperty(Context.PROVIDER_URL);
          providerContextFactory = props.getProperty(Context.INITIAL_CONTEXT_FACTORY);
          userName = props.getProperty(Context.SECURITY_PRINCIPAL);
          password = props.getProperty(Context.SECURITY_CREDENTIALS);
          
          /* print parameters */
          System.err.println("------------------------------------------------------------------------");
          System.err.println("JmsEchoService");
          System.err.println("------------------------------------------------------------------------");
          System.err.println("Server....................... "+providerUrl);
          System.err.println("InitialContextFactory........ "+providerContextFactory);
          System.err.println("User......................... "+((userName != null)?userName:"(null)"));
          System.err.println("Destination.................. "+name);
          System.err.println("------------------------------------------------------------------------");

           /*
            * Lookup connection factory which must exist in the factories
            * config file.
            */
           ConnectionFactory factory =
               (ConnectionFactory)ctx.lookup("ConnectionFactory");

            /* create the connection */
            connection = factory.createConnection(userName,password);

            /* create the session */
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            /* set the exception listener */
            connection.setExceptionListener(this);

            /* create the destination */
            destination = (Destination)ctx.lookup(name);

            System.err.println("Listening for requests on destination: "+name);

            /* create the consumer */
            msgConsumer = session.createConsumer(destination);

            /* create the producer */
            msgProducer = session.createProducer(null);

            /* set the message listener */
            msgConsumer.setMessageListener(this);

            /* start the connection */
            connection.start();

            // Note: when message callback is used, the session
            // creates the dispatcher thread which is not a daemon
            // thread by default. Thus we can quit this method however
            // the application will keep running. It is possible to
            // specify that all session dispatchers are daemon threads.
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*-----------------------------------------------------------------------
     * usage
     *----------------------------------------------------------------------*/
    void usage()
    {
        System.err.print("\nUsage: java JmsEchoService [options]");
        System.err.print("\n");
        System.err.print("   where options are:\n");
        System.err.print("\n");
        System.err.print(" -server   <server URL> - EMS server URL, default is local server\n");
        System.err.print(" -cf       <class name> - JMS provider's initial ContextFactory\n");
        System.err.print(" -user     <user name>  - user name, default is null\n");
        System.err.print(" -password <password>   - password, default is null\n");
        System.err.print(" -dest     <dest name>  - Destination name, default is \"queue.sample\"\n");
        System.exit(0);
    }

    /*-----------------------------------------------------------------------
     * parseArgs
     *----------------------------------------------------------------------*/
    void parseArgs(String[] args)
    {
        int i=0;

        while (i < args.length)
        {
            if (args[i].compareTo("-server")==0)
            {
                if ((i+1) >= args.length) usage();
                providerUrl = args[i+1];
                i += 2;
            }
            if (args[i].compareTo("-cf")==0)
            {
                if ((i+1) >= args.length) usage();
                providerContextFactory = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-dest")==0)
            {
                if ((i+1) >= args.length) usage();
                name = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-user")==0)
            {
                if ((i+1) >= args.length) usage();
                userName = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-password")==0)
            {
                if ((i+1) >= args.length) usage();
                password = args[i+1];
                i += 2;
            }
            else
            if (args[i].compareTo("-help")==0)
            {
                usage();
            }
            else
            {
                System.err.println("Unrecognized parameter: "+args[i]);
                usage();
            }
        }
    }


    /*---------------------------------------------------------------------
     * onException
     *---------------------------------------------------------------------*/
    public void onException(JMSException e)
    {
        /* print the connection exception status */
        System.err.println("CONNECTION EXCEPTION: "+ e.getMessage());
    }

    /*---------------------------------------------------------------------
     * onMessage
     *---------------------------------------------------------------------*/
    public void onMessage(Message msg)
    {
    	TextMessage requestMsg = (TextMessage)msg;
    	String requestID = null;
    	TextMessage replyMsg = null;
    	Destination replyTo = null;
    	
    	try
        {
            // Get the JMSCorrelationID of the request for later use
            requestID = requestMsg.getJMSCorrelationID();

            // If no JMSCorrelationID was specified, just use the JMSMessageID
            if (requestID == null)
            	requestID = requestMsg.getJMSMessageID();
            
            System.err.println("Received request ID:" + requestID
            		+ " Body:\"" + requestMsg.getText() + "\"");

            replyMsg = session.createTextMessage();

            replyMsg.setJMSCorrelationID(requestID);
            replyMsg.setText("ECHO: " + ((TextMessage)requestMsg).getText());
            
            // set the reply Destination to the request message's JMSReplyTo
            replyTo = requestMsg.getJMSReplyTo();
            replyMsg.setJMSDestination(replyTo);

            // send the reply
            msgProducer.send(replyTo, replyMsg);
        }
        catch (Exception e)
        {
            System.err.println("Unexpected exception in the message callback!");
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    /**
     * read property file from classpath
     * @param propertyFileName
     * @throws IOException
     */
    private Properties readPropertyFileFromClasspath(String propertyFileName) 
    {
        Properties prop = null;
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(propertyFileName);
        if (is != null)
        {
        	prop = new Properties();
        	try {
        		prop.load(is);
		        System.out.println(propertyFileName +" loaded from Classpath::"
		        		+Context.PROVIDER_URL +" = "+prop.getProperty(Context.PROVIDER_URL));
		        System.out.println(propertyFileName +" loaded from Classpath::"
		        		+ Context.INITIAL_CONTEXT_FACTORY +" = "+prop.getProperty(Context.INITIAL_CONTEXT_FACTORY));
		        System.out.println(propertyFileName +" loaded from Classpath::"
		        		+ Context.SECURITY_PRINCIPAL +" = "+prop.getProperty(Context.SECURITY_PRINCIPAL));
		        System.out.println(propertyFileName +" loaded from Classpath::"
		        		+ Context.SECURITY_CREDENTIALS +" = "+prop.getProperty(Context.SECURITY_CREDENTIALS));
        	} catch (IOException e)
        	{
        		prop=null;
        	}
        }        
        return prop;
    }

    /*-----------------------------------------------------------------------
     * main
     *----------------------------------------------------------------------*/
    public static void main(String[] args)
    {
        new JmsEchoService(args);
    }

}


