package com.tibco.mcqueary.jms.samples;
/* 
 * Copyright (c) TIBCO Software Inc. 
 * All rights reserved.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 * 
 */

/*
 * This is a simple sample of a basic JmsEchoClient.
 *
 * This sample publishes specified message(s) on a specified
 * destination and quits.
 *
 * Notice that the specified destination should exist in your configuration
 * or your topics/queues configuration file should allow
 * creation of the specified topic or queue. Sample configuration supplied with
 * the TIBCO Enterprise Message Service distribution allows creation of any
 * destination.
 *
 * If this sample is used to publish messages into
 * tibjmsMsgConsumer sample, the tibjmsMsgConsumer
 * sample must be started first.
 *
 * If -topic is not specified this sample will use a topic named
 * "topic.sample".
 *
 * Usage:  java JmsEchoClient  [options]
 *                               <message-text1>
 *                               ...
 *                               <message-textN>
 *
 *  where options are:
 *
 *   -server    <server-url>  JMS Provider URL.
 *                            If not specified this sample assumes a
 *                            providerUrl of "tibjmsnaming://localhost:7222"
 *   -cf		<class>	      Initial Context Factory of provider                         
 *   -user      <user-name>   User name. Default is null.
 *   -password  <password>    User password. Default is null.
 *   -dest      <dest name>   Destination name. Default value is "queue.sample"
 *
 */


import java.util.*;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;

public class JmsEchoClient implements MessageListener, ExceptionListener
{

    static final String  defaultProviderContextFactory =
            "com.tibco.tibjms.naming.TibjmsInitialContextFactory";

    static final String  defaultProviderURL =
                            "tibjmsnaming://localhost:7222";

    /*-----------------------------------------------------------------------
     * Parameters
     *----------------------------------------------------------------------*/

    String          providerUrl    = defaultProviderURL;
    String			providerContextFactory = defaultProviderContextFactory;
    String          userName     = null;
    String          password     = null;
    String          name         = "queue.sample";
    Vector<String>  data         = new Vector<String>();
    boolean			running		 = true;

    /*-----------------------------------------------------------------------
     * Variables
     *----------------------------------------------------------------------*/
    Connection      connection   = null;
    Session         session      = null;
    MessageProducer msgProducer  = null;
    Destination     destination  = null;

    HashMap<String, TextMessage> rMap = new HashMap<String, TextMessage>();

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
		try {
			String corrID = msg.getJMSCorrelationID();
			if (rMap.containsKey(corrID))
			{
				System.out.println("Received response to ID:" + corrID + ": \"" + ((TextMessage) msg).getText() +"\"");
				rMap.remove(corrID);
			}
			else
			{
				System.err.println("Error: received response with unrecognized JMSCorrelationID: " + corrID);
			}
		} catch (JMSException e)
		{
			System.err.println("MessageListener encountered an exception while retrieving message body:");
			e.printStackTrace();
		}

		// All requests received?
		if (rMap.size()==0)
		{
			running=false;
		}			
	}
	
    public JmsEchoClient(String[] args)
    {
        parseArgs(args);
 
        /* print parameters */
        System.err.println("\n------------------------------------------------------------------------");
        System.err.println("JmsEchoClient SAMPLE");
        System.err.println("------------------------------------------------------------------------");
        System.err.println("Server....................... "+providerUrl);
        System.err.println("InitialContextFactory........ "+providerContextFactory);
        System.err.println("User......................... "+((userName != null)?userName:"(null)"));
        System.err.println("Destination.................. "+name);
        System.err.println("Message Text(s).............. ");
        for (int i=0;i<data.size();i++)
        {
            System.err.println(data.elementAt(i));
        }
        System.err.println("------------------------------------------------------------------------\n");

        try 
        {
            TextMessage msg;
            int         i;

            if (data.size() == 0)
            {
                System.err.println("***Error: must specify at least one message text\n");
                usage();
            }

            System.err.println("Publishing to destination '"+name+"'\n");

            /*
            * Init JNDI Context.
            */
            
           Hashtable<String,String> env = new Hashtable<String,String>();
           env.put(Context.INITIAL_CONTEXT_FACTORY, providerContextFactory);
           env.put(Context.PROVIDER_URL, providerUrl);

           if (userName != null)
           {
              env.put(Context.SECURITY_PRINCIPAL, userName);

              if (password != null)
                 env.put(Context.SECURITY_CREDENTIALS, password);
           }

           InitialContext jndiContext = new InitialContext(env);

           /*
            * Lookup connection factory which must exist in the factories
            * config file.
            */
           ConnectionFactory factory =
               (ConnectionFactory)jndiContext.lookup("ConnectionFactory");
           
            connection = factory.createConnection(userName,password);

            /* create the session */
            session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

            /* set the exception listener */
            connection.setExceptionListener(this);

            /* create the destination */
            destination = (Destination)jndiContext.lookup(name);

            /* create the producer */
            msgProducer = session.createProducer(destination);

            /* create the reply-to destination */
            Destination myReplyQueue = session.createTemporaryQueue();

            /* create the reply consumer */
            MessageConsumer replyConsumer = session.createConsumer(myReplyQueue);
            replyConsumer.setMessageListener(this);

            connection.start();
            
            /* publish messages */
            for (i = 0; i<data.size(); i++)
            {
                /* create text message */
                msg = session.createTextMessage();

                /* set message text */
                msg.setText((String)data.elementAt(i));
                
                // Set message ReplyTo
                msg.setJMSReplyTo(myReplyQueue);

                // Set correlation ID
                String matchID = UUID.randomUUID().toString();
                msg.setJMSCorrelationID(matchID);
                
                // Send the message
                msgProducer.send(msg);
                
                rMap.put(matchID, msg);

                System.err.println("Produced request ID:" + matchID + " Body:\"" +data.elementAt(i) + "\"" );
            }
            
            // Wait until the MessageListener has received/processed all the replies
            while (running)
            {
            	Thread.sleep(10);
            }
            
            /* close the connection */
            System.err.print("Closing connection...");
            connection.close();
            System.err.println("DONE.");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /*-----------------------------------------------------------------------
    * usage
    *----------------------------------------------------------------------*/
    private void usage()
    {
        System.err.println("\nUsage: java JmsEchoClient [options] " +
        					"<message-text-1> [<message-text-2>] ...\n");
        System.err.println("   where options are:\n");
        System.err.println("   -server   <server URL>  - EMS server URL, default is local server");
        System.err.println("   -cf       <class name>  - JMS provider's initial ContextFactory");
        System.err.println("   -user     <user name>   - user name, default is null");
        System.err.println("   -password <password>    - password, default is null");
        System.err.println("   -dest     <dest name>   - destination name, default is \"queue.sample\"");
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
            else
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
            if (args[i].startsWith("-"))
            {
                System.err.println("Unrecognized parameter: "+args[i]);
            	usage();
            }
            else
            {
                data.addElement(args[i]);
                i++;
            }
        }
    }

    /*-----------------------------------------------------------------------
     * main
     *----------------------------------------------------------------------*/
    public static void main(String[] args)
    {
        new JmsEchoClient(args);
    }
}

