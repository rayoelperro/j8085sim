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
 *   Developer: Sinu John - http://www.sinujohn.wordpress.com
 *               email: sinuvian@gmail.com
 *
 *    Copyright 2010 Sinu John
 */

package sim;
import java.util.Scanner;
import java.util.HashMap;

import javax.swing.text.*;
import javax.swing.*;
import java.awt.Color;

/**
* @author sinu
*/
class SynAnalyser //Syntax Analyser
{
    //USAGE: use input() and then analyser()
	String pgm,str,opcode,op1,op2,label;
	Scanner scan;
	StatementLine stmt;
    int pc=0,line=1,startLocation=0,flag=0;
    OpcodeTable opt=new OpcodeTable();
    //Register reg;
    Memory mem;
    HashMap hm;
    String errorMsg;

    HashMap lineDetail=new HashMap();

    StyledDocument doc=null;
    int offset=0;
    JTextPane jtp;
    MutableAttributeSet labelattr = new SimpleAttributeSet();
    MutableAttributeSet opcattr = new SimpleAttributeSet();
    MutableAttributeSet oprattr1 = new SimpleAttributeSet();
    MutableAttributeSet oprattr2 = new SimpleAttributeSet();
    MutableAttributeSet commattr = new SimpleAttributeSet();
    MutableAttributeSet errorattr = new SimpleAttributeSet();
    MutableAttributeSet defaultattr = new SimpleAttributeSet();

    void setTextpane(JTextPane tp)
    {
        jtp=tp;
        doc=(StyledDocument)jtp.getDocument();

        StyleConstants.setBackground(defaultattr, Color.WHITE);
        StyleConstants.setForeground(defaultattr, Color.BLACK);

        StyleConstants.setBackground(labelattr, Color.WHITE);
        StyleConstants.setForeground(labelattr, Color.BLUE);

        StyleConstants.setBackground(opcattr, Color.WHITE);
        StyleConstants.setForeground(opcattr, Color.RED);

        StyleConstants.setBackground(oprattr1, Color.WHITE);
        StyleConstants.setForeground(oprattr1, Color.GREEN);

        StyleConstants.setBackground(oprattr2, Color.WHITE);
        StyleConstants.setForeground(oprattr2, Color.GREEN);

        StyleConstants.setBackground(commattr, Color.WHITE);
        StyleConstants.setForeground(commattr, Color.GRAY);

        StyleConstants.setBackground(errorattr, Color.RED);
        StyleConstants.setForeground(errorattr, Color.WHITE);
    }

	SynAnalyser(Memory m)
	{
		stmt=new StatementLine();
        //reg=r;
        mem=m;
        hm=new HashMap();
	}
    void input(String s)
    {
        pgm=s;
        scan=new Scanner(pgm);
    }

	void analyser()
	{
        try{
		for(str=scan.nextLine();str!=null;str=scan.nextLine(),line++)
		{
            str=str.toLowerCase();
			stmt.split(str);
            label=stmt.getLabel();
            opcode=stmt.getOpcode();
            op1=stmt.getOp1();
            op2=stmt.getOp2();
            
            if(stmt.opcodePresent())
            {
                if(stmt.labelPresent()&&!opcode.equalsIgnoreCase("@equ"))
                {
                    //label=label.substring(0, label.length()-1);
                    //System.out.println("XXXX: "+label);
                    if(hm.containsKey(label)) errorDuplicateLabel(line);
                    else
                    {
                        String strPC=Integer.toHexString(pc);
                        int tmp=4-strPC.length();
                        String dup="";
                        for(int j=0;j<tmp;j++) dup=dup+"0";
                        strPC=dup+strPC;
                        hm.put(label, strPC);
                    }
                }

                if(isAssemblerDirective(label,opcode,op1,op2))
                {
                    if(opcode.equalsIgnoreCase("@next"))
                    {
                        try{
                            pc=Integer.parseInt(op1, 16);
                        }
                        catch(NumberFormatException q)
                        {
                        }

                    }
                    if(opcode.equalsIgnoreCase("@equ"))
                    {
                        if(hm.containsKey(label)) errorDuplicateLabel(line);
                        else
                        {
                            if(op1==null) errorNumberOfOperands(line, opcode);
                            else if(!validHex(op1)) errorInvalidOperand1(line);
                            else
                            {
                                hm.put(label, op1);
                            }
                        }
                    }
                }
                else pc+=opt.getSizeOf(opcode);
            }
		}
        }catch(java.util.NoSuchElementException e)
        {

        }

        scan=new Scanner(pgm);
        line=1;
        pc=0;

        offset=0;
        String offsetCalc="";
        try{
		for(str=scan.nextLine();str!=null;str=scan.nextLine(),line++,offset+=offsetCalc.length()+1)
		{
            str=str.toLowerCase();
			stmt.split(str);
            label=stmt.getLabel();
            opcode=stmt.getOpcode();
            op1=stmt.getOp1();
            op2=stmt.getOp2();

            int labend=0,opcend=0,opend1=0,opend2=0;
            offsetCalc=str;
            String dupli=str+"                ";
            //System.out.println("Opcode: "+opcode+" Op1: "+op1+" Op2: "+op2);

            String strPC=Integer.toHexString(pc);
            int tmp=4-strPC.length();
            String dup="";
            for(int j=0;j<tmp;j++) dup=dup+"0";
            strPC=dup+strPC;
            int lineLimits[]={offset,offsetCalc.length()};
            lineDetail.put(strPC, lineLimits);

            if(stmt.commentPresent()) {
                doc.setCharacterAttributes(offset+labend+opcend+opend1+opend2+dupli.indexOf(';'), dupli.length()-dupli.indexOf(';'), commattr, false);
            }
            if(label!=null)
            {
                doc.setCharacterAttributes(offset+dupli.indexOf(label), label.length()+1, labelattr, false);
                labend=label.length()+1;
                dupli=dupli.substring(labend);
            }
            if(opcode!=null) {
                doc.setCharacterAttributes(offset+labend+dupli.indexOf(opcode), opcode.length(), opcattr, false);
                opcend=opcode.length()+1;
                dupli=dupli.substring(opcend);
            }
            if(op1!=null) {
                doc.setCharacterAttributes(offset+labend+opcend+dupli.indexOf(op1), op1.length(), oprattr1, false);
                opend1=op1.length()+1;
                dupli=dupli.substring(opend1);
            }
            if(op2!=null) {
                doc.setCharacterAttributes(offset+labend+opcend+opend1+dupli.indexOf(op2), op2.length(), oprattr2, false);
                opend2=op2.length()+1;
                dupli=dupli.substring(opend2);
            }
            for(int i=0;i<str.length();i++)
                if(str.charAt(i)==' '||str.charAt(i)==','||str.charAt(i)=='\n')
                    doc.setCharacterAttributes(offset+i, 1, defaultattr, false);

            if(stmt.opcodePresent())
            {
            if(isAssemblerDirective(label,opcode,op1,op2))
            {
                if(op1==null) errorNumberOfOperands(line, opcode);
                else
                {
                if(opcode.equalsIgnoreCase("@next"))
                    {
                        try{
                        pc=Integer.parseInt(op1, 16);
                        if(flag==0) startLocation=pc;
                        }
                        catch(NumberFormatException q)
                        {
                            errorInvalidOperand1(line);
                        }
                    }
                if(opcode.equalsIgnoreCase("@begin"))
                {
                    try{
                    startLocation=Integer.parseInt(op1, 16);
                    flag=1;
                    }
                    catch(NumberFormatException q)
                    {
                        errorInvalidOperand1(line);
                    }
                }
                }
            }
            else
            {
            if(stmt.operand1Present())
            {
                if(op1.equals("xx") || op1.equals("xxxx"))
                        errorInvalidOperand1(line);
                if(hm.containsKey(op1))
                {
                    op1=(String)hm.get(op1);
//                    int tmp=4-op1.length();
//                    String dup="";
//                    for(int j=0;j<tmp;j++) dup=dup+"0";
//                    op1=dup+op1;
                }
            }
            if(stmt.operand2Present())
            {
                if(op2.equals("xx") || op2.equals("xxxx"))
                        errorInvalidOperand2(line);
                if(hm.containsKey(op2))
                    op2=(String) hm.get(op2);
            }
            if(stmt.opcodePresent())
            {
                processOpcode();
            }
            }
            }

		}
        }catch(java.util.NoSuchElementException e)
        {

        }
	}
    /*void processLabel()
    {
        System.out.println("Label is "+label);
    }*/
    String getStartLocation()
    {
        String sl=""+Integer.toHexString(startLocation);
        int tmp=4-sl.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
                    sl=dup+sl;
        return sl;
    }
    void processOpcode()
    {
        String hx;
        try
        {
        if(!opt.isOpcodePresent(opcode))
        {
            errorInvalidOpcode(line); //Opcode is not present at Line no. 'line'
        }
        else if(stmt.numberOfOperands()!=opt.getNumberOfOperands(opcode))
        {
            errorNumberOfOperands(line,opcode); //mismatch in number of operands required by opcode.
                               //report Line no- 'line' and No.of operands required by opcode='opcode'
        }
        else if((hx=opt.getHex(opcode, op1, op2))!=null)
        {
            mem.write(hx,pc); //got hex code of entire statement
        }
        else if((hx=opt.getHex(opcode, op1))!=null)
        {
            mem.write(hx,pc); //got hex code of opcode and op1;
            if(opt.getOp2(opcode).length()!=op2.length())
            {
                errorInvalidOperand2(line); //Invalid Operand2 at Line no.- 'line'
            }
            else
            {
                if(op2.length()==2)
                {
                    //op2 is 8bit. so write it at pc+1
                    //String rhex=reverseHex(op2); //we call this function rather than using op2 directly because we want to have exception handling
                    if(validateHex(op2))
                        mem.write(op2,pc+1);
                }
                else if(op2.length()==4) //op2 is 16bit. We should reverse it and then split it
                {
                    //String rhex=reverseHex(op2);
                    if(validateHex(op2))
                    {
                    mem.write(op2.substring(2, 4),pc+1); //write 1st byte at pc+1
                    mem.write(op2.substring(0, 2),pc+2); //write 2nd byte at pc+2
                    }
                }
            }
        }
        else if((hx=opt.getHex(opcode))!=null)
        {
            mem.write(hx,pc); //write hex code of opcode at pc
            if(opt.getOp1(opcode).length()!=op1.length())
            {
                errorInvalidOperand1(line); //Invalid Operand1 at Line no.- 'line'
            }
            else
            {
                if(op1.length()==2)
                {
                    //op1 is 8bit. so write it at pc+1
                    //String rhex=reverseHex(op1); //we call this function rather than using op2 directly because we want to have exception handling
                    if(validateHex(op1))
                    {
                    mem.write(op1,pc+1);
                    }
                }
                else if(op1.length()==4) //op1 is 16bit. We should reverse it and then split it
                {
                    //String rhex=reverseHex(op1);
                    if(validateHex(op1))
                    {
                    mem.write(op1.substring(2, 4),pc+1); //write 1st byte at pc+1
                    mem.write(op1.substring(0, 2),pc+2); //write 2nd byte at pc+2
                    }
                }
            }
        }
        else errorInvalidOperand(line);
        pc+=opt.getSizeOf(opcode);
    }
        catch(ArrayIndexOutOfBoundsException qq)
        {
            errorMemoryOverflow(line);
        }
    }
    boolean validateHex(String h)
    {
        int num;
        String rhex;
        try
        {
        num=Integer.parseInt(h, 16);
        rhex=Integer.toHexString(Integer.reverseBytes(num));
        }
        catch(java.lang.NumberFormatException e)
        {
            errorInvalidOperand(line);
            return false;
        }
        return true;
    }
    boolean validHex(String h)
    {
        if(h==null) return false;
        try
        {
            if(h.length()!=2 && h.length()!=4) return false;
            int n=Integer.parseInt(h, 16);
        }
        catch(NumberFormatException e)
        {
            return false;
        }
        return true;
    }
    boolean isAssemblerDirective(String lab,String opc,String opr1,String opr2)
    {
        if(opc.equalsIgnoreCase("@equ"))
        {
            if(lab==null||opr2!=null) return false;
            else return true;
        }

        if(lab!=null||opr2!=null) return false;
        else if(opc.equalsIgnoreCase("@next"))
        {
            //if(validateHex(op1))
                return true;
            //else return false;
        }
        else if(opc.equalsIgnoreCase("@begin"))
        {
            //if(validateHex(op1))
                return true;
            //else return false;
        }
        else return false;
    }

    public HashMap getSymbolInfo()
    {
        return hm;
    }
    public HashMap getLineInfo()
    {
        return lineDetail;
    }

    public String getOpcode()
    {
        return opcode;
    }

    void errorMemoryOverflow(int l)
    {
        doc.setCharacterAttributes(offset, str.length(), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nMemory Overflow statement at Line "+l;
    }
    void errorDuplicateLabel(int l)
    {
        //System.out.println("Duplicate label at Line "+l);
        doc.setCharacterAttributes(offset+str.indexOf(label), label.length()+1, errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nDuplicate label at Line "+l;
    }
    void errorInvalidOpcode(int l)
    {
        doc.setCharacterAttributes(offset+str.indexOf(opcode), opcode.length(), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nInvalid Opcode at Line "+l;
    }
    void errorNumberOfOperands(int l,String op)
    {
        doc.setCharacterAttributes(offset, str.length(), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nInvalid Number of Operands at Line "+l;
    }
    void errorInvalidOperand1(int l)
    {
        doc.setCharacterAttributes(offset+str.indexOf(op1), op1.length(), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nInvalid Operand at Line "+l;
    }
    void errorInvalidOperand2(int l)
    {
        doc.setCharacterAttributes(offset+str.indexOf(op2), op2.length(), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nInvalid Operand at Line "+l;
    }
    void errorInvalidOperand(int l)
    {
        doc.setCharacterAttributes(offset+str.indexOf(op1), str.length()-str.indexOf(op1), errorattr, false);
        if(errorMsg==null) errorMsg="";
        errorMsg=errorMsg+"\nInvalid Operand at Line "+l;
    }
}