/*
 *  This file is part of j8085sim.
 *
 *   j8085sim is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   j8085sim is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with j8085sim.  If not, see <http://www.gnu.org/licenses/>. 
 */

/*
 *   Developers: Sinu John - http://www.sinujohn.wordpress.com
 *               email: sinuvian@gmail.com
 *
 *              Deepu Devassy
 *               email: nautilusd007@gmail.com
 *
 *    Copyright 2010 Sinu John, Deepu Devassy
 */

package sim;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
* @author sinu, deepu
*/
public class XMLRead
{
    String[] getHelp(String opcodeName)
    {
        String help[]=new String[3];
        try
        {
            java.io.InputStream is = getClass().getResourceAsStream("/sim/help.xml");
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);

            // normalize text representation
            doc.getDocumentElement().normalize();

            NodeList instrList=doc.getElementsByTagName("instruction");
            for(int i=0;i<instrList.getLength();i++)
            {
                Node instrNode=instrList.item(i);
                Element instrElement=(Element)instrNode;

                NodeList opcodeList=instrElement.getElementsByTagName("opcode");
                Node opcodeNode=opcodeList.item(0);
                if (opcodeNode.getTextContent().trim().equals(opcodeName))
                {
                    NodeList operandList=instrElement.getElementsByTagName("operand");
                    Node operandNode=operandList.item(0);
                    help[0]=operandNode.getTextContent().trim();

                    NodeList shortList=instrElement.getElementsByTagName("short");
                    Node shortNode=shortList.item(0);
                    help[1]=shortNode.getTextContent().trim();

                    NodeList longList=instrElement.getElementsByTagName("long");
                    Node longNode=longList.item(0);
                    help[2]=longNode.getTextContent().trim();
                    return help;
                }
             }

        }catch (SAXParseException err) {
        System.out.println ("** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
        System.out.println(" " + err.getMessage ());

        }catch (SAXException e) {
        Exception x = e.getException ();
        ((x == null) ? e : x).printStackTrace ();

        }catch (Throwable t) {
        t.printStackTrace ();
        }
        return null;
    }
    String[] getAllOpcode()
    {
         String allOpList[]=null;
         int count=0;
         try
         {
             java.io.InputStream is = getClass().getResourceAsStream("/sim/help.xml");
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);

            // normalize text representation
            doc.getDocumentElement ().normalize ();
          
            NodeList instrList=doc.getElementsByTagName("instruction");
            allOpList=new String[instrList.getLength()];
            for(int i=0;i<instrList.getLength();i++)
            {
                Node instrNode=instrList.item(i);
                Element instrElement=(Element)instrNode;
                NodeList opcodeList=instrElement.getElementsByTagName("opcode");
                Node opcodeNode=opcodeList.item(0);
                allOpList[count]=opcodeNode.getTextContent().trim().toUpperCase();
                count++;
           }
         }
         catch (SAXParseException err) {
          System.out.println ("** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
          System.out.println(" " + err.getMessage ());

         }catch (SAXException e) {
          Exception x = e.getException ();
         ((x == null) ? e : x).printStackTrace ();

         }catch (Throwable t) {
         t.printStackTrace ();
         }
         for(int i=0;i<count;i++) //Sorting the list
         {
             for(int j=1;j<count-i;j++)
             {
                 if(allOpList[j-1].compareToIgnoreCase(allOpList[j])>0)
                 {
                     String t=allOpList[j];
                     allOpList[j]=allOpList[j-1];
                     allOpList[j-1]=t;
                 }
             }
         }
         return allOpList;       
    }
}

