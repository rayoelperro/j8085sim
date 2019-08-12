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

/**
* @author sinu
*/
class StatementLine
{
	String label,op1,op2,opc,tmp,str;
	int c=0;

    public void separator()
	{
		int p=0,j,i;
        label=null;opc=null;op1=null;op2=null;
	boolean labpresent=false;
	if(str.indexOf(';')>=0)
	{
		c=1;
		str=str.substring(0,str.indexOf(';'));
	}
		for(i=0;i<str.length();i++)
		{
			tmp="";
			for(j=i;j<str.length();j++)
			{
				if(str.charAt(j)!=' '&&str.charAt(j)!=','&&str.charAt(j)!=':')
				{
					tmp=tmp+str.charAt(j);
					/*if(str.charAt(j)==';')
					{
						c=1;
						return;
					}*/
				}
				else
				{
					if(str.charAt(j)==':') labpresent=true;
					break;
				}
			}
			i=j;
			if(!tmp.equals(""))
			{
				p++;
				switch(p)
				{
					case 1: //if(tmp.charAt(tmp.length()-1)==':') label=tmp;
						if(labpresent) label=tmp;
						else
						{ opc=tmp; p++; }
						break;
					case 2: opc=tmp; break;
					case 3: op1=tmp; break;
					case 4: op2=tmp; break;
				}
			}
		}
		/* System.out.println("Label: "+label);
		System.out.println("Opcode: "+opc);
		System.out.println("Op1: "+op1);
		System.out.println("Op2: "+op2); */
	}

    /*
	public void separator()
	{
		int p=0,j,i;
        label=null;opc=null;op1=null;op2=null;
		for(i=0;i<str.length();i++)
		{
			tmp="";
			for(j=i;j<str.length();j++)
			{
				if(str.charAt(j)!=' '&&str.charAt(j)!=',')
				{
					tmp=tmp+str.charAt(j);
					if(str.charAt(j)==';')
					{
						c=1;
						return;
					}
				}
				else break;
			}
			i=j;
			if(!tmp.equals(""))
			{
				p++;
				switch(p)
				{
					case 1: if(tmp.charAt(tmp.length()-1)==':') label=tmp;
						else
						{ opc=tmp; p++; }
						break;
					case 2: opc=tmp; break;
					case 3: op1=tmp; break;
					case 4: op2=tmp; break;
				}
			}
		}
		/* System.out.println("Label: "+label);
		System.out.println("Opcode: "+opc);
		System.out.println("Op1: "+op1);
		System.out.println("Op2: "+op2); */
	//}
	public void split(String s)
	{
		str=s;
		separator();
	}
	public String getLabel()
	{
		return label;
	}
	public String getOpcode()
	{
		return opc;
	}
	public String getOp1()
	{
		return op1;
	}
	public String getOp2()
	{
		return op2;
	}
	public boolean labelPresent()
	{
		if(label==null) return false;
		else return true;
	}
	public int numberOfOperands()
	{
		if(op1==null&&op2==null) return 0;
		else if(op1!=null&&op2!=null) return 2;
		else return 1;
	}
    public boolean opcodePresent()
    {
        if(opc==null) return false;
        else return true;
    }
	public boolean operand1Present()
	{
		if(op1==null) return false;
		else return true;
	}
	public boolean operand2Present()
	{
		if(op2==null) return false;
		else return true;
	}
	public boolean commentPresent()
	{
		if(c==0) return false;
		else return true;
	}
}
