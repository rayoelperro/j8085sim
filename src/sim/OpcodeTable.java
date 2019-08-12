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
 *              Jinsmon Kurian
 *               email: jins1223@gmail.com
 *
 *              Deepu Devassy
 *               email: nautilusd007@gmail.com
 *
 *    Copyright 2010 Sinu John, Jinsmon Kurian, Deepu Devassy
 */

package sim;

/**
* @author sinu, jins, deepu
*/
class Details
{
    int size;
    String op1,op2,hex;
    Details(int s,String o1,String o2,String hx)
    {
        size=s;
        op1=o1; op2=o2; hex=hx;
    }
}
class OpcodeTable
{
    //Table: Opcode; No.OfOperands; SizeOfInstruction; Op1; Op2; Hex
    //For 8 bit operands: XX
    //For 16bit operands: XXXX

    //Column Numbers
    final int OPC=0;
    final int OP1=1;
    final int OP2=2;
    final int SIZE=3;
    final int HEX=4;
    final int TST=5;

    final String table[][]={
{"aci","xx",null,"2","ce","7"},
{"adc","a",null,"1","8f","4"},
{"adc","b",null,"1","88","4"},
{"adc","c",null,"1","89","4"},
{"adc","d",null,"1","8a","4"},
{"adc","e",null,"1","8b","4"},
{"adc","h",null,"1","8c","4"},
{"adc","l",null,"1","8d","4"},
{"adc","m",null,"1","8e","7"},
{"add","a",null,"1","87","4"},
{"add","b",null,"1","80","4"},
{"add","c",null,"1","81","4"},
{"add","d",null,"1","82","4"},
{"add","e",null,"1","83","4"},
{"add","h",null,"1","84","4"},
{"add","l",null,"1","85","4"},
{"add","m",null,"1","86","7"},
{"adi","xx",null,"2","c6","7"},
{"ana","a",null,"1","a7","4"},
{"ana","b",null,"1","a0","4"},
{"ana","c",null,"1","a1","4"},
{"ana","d",null,"1","a2","4"},
{"ana","e",null,"1","a3","4"},
{"ana","h",null,"1","a4","4"},
{"ana","l",null,"1","a5","4"},
{"ana","m",null,"1","a6","7"},
{"ani","xx",null,"2","e6","7"},
{"call","xxxx",null,"3","cd","18"},
{"cc","xxxx",null,"3","dc","9"},
{"cm","xxxx",null,"3","fc","9"},
{"cma",null,null,"1","2f","4"},
{"cmc",null,null,"1","3f","4"},
{"cmp","a",null,"1","bf","4"},
{"cmp","b",null,"1","b8","4"},
{"cmp","c",null,"1","b9","4"},
{"cmp","d",null,"1","ba","4"},
{"cmp","e",null,"1","bb","4"},
{"cmp","h",null,"1","bc","4"},
{"cmp","l",null,"1","bd","4"},
{"cmp","m",null,"1","be","7"},
{"cnc","xxxx",null,"3","d4","9"},
{"cnz","xxxx",null,"3","c4","9"},
{"cp","xxxx",null,"3","f4","9"},
{"cpe","xxxx",null,"3","ec","9"},
{"cpi","xx",null,"2","fe","7"},
{"cpo","xxxx",null,"3","e4","9"},
{"cz","xxxx",null,"3","cc","9"},
{"daa",null,null,"1","27","4"},
{"dad","b",null,"1","09","10"},
{"dad","d",null,"1","19","10"},
{"dad","h",null,"1","29","10"},
{"dad","sp",null,"1","39","10"},
{"dcr","a",null,"1","3d","4"},
{"dcr","b",null,"1","05","4"},
{"dcr","c",null,"1","0d","4"},
{"dcr","d",null,"1","15","4"},
{"dcr","e",null,"1","1d","4"},
{"dcr","h",null,"1","25","4"},
{"dcr","l",null,"1","2d","4"},
{"dcr","m",null,"1","35","10"},
{"dcx","b",null,"1","0b","6"},
{"dcx","d",null,"1","1b","6"},
{"dcx","h",null,"1","2b","6"},
{"dcx","sp",null,"1","3b","6"},
{"di",null,null,"1","f3","4"},
{"ei",null,null,"1","fb","4"},
{"hlt",null,null,"1","76","5"},
{"in","xx",null,"2","db","10"},
{"inr","a",null,"1","3c","4"},
{"inr","b",null,"1","04","4"},
{"inr","c",null,"1","0c","4"},
{"inr","d",null,"1","14","4"},
{"inr","e",null,"1","1c","4"},
{"inr","h",null,"1","24","4"},
{"inr","l",null,"1","2c","4"},
{"inr","m",null,"1","34","10"},
{"inx","b",null,"1","03","6"},
{"inx","d",null,"1","13","6"},
{"inx","h",null,"1","23","6"},
{"inx","sp",null,"1","33","6"},
{"jc","xxxx",null,"3","da","7"},
{"jm","xxxx",null,"3","fa","7"},
{"jmp","xxxx",null,"3","c3","10"},
{"jnc","xxxx",null,"3","d2","7"},
{"jnz","xxxx",null,"3","c2","7"},
{"jp","xxxx",null,"3","f2","7"},
{"jpe","xxxx",null,"3","ea","7"},
{"jpo","xxxx",null,"3","e2","7"},
{"jz","xxxx",null,"3","ca","7"},
{"lda","xxxx",null,"3","3a","13"},
{"ldax","b",null,"1","0a","7"},
{"ldax","d",null,"1","1a","7"},
{"lhld","xxxx",null,"3","2a","16"},
{"lxi","b","xxxx","3","01","10"},
{"lxi","d","xxxx","3","11","10"},
{"lxi","h","xxxx","3","21","10"},
{"lxi","sp","xxxx","3","31","10"},
{"mov","a","a","1","7f","4"},
{"mov","a","b","1","78","4"},
{"mov","a","c","1","79","4"},
{"mov","a","d","1","7a","4"},
{"mov","a","e","1","7b","4"},
{"mov","a","h","1","7c","4"},
{"mov","a","l","1","7d","4"},
{"mov","a","m","1","7e","7"},
{"mov","b","a","1","47","4"},
{"mov","b","b","1","40","4"},
{"mov","b","c","1","41","4"},
{"mov","b","d","1","42","4"},
{"mov","b","e","1","43","4"},
{"mov","b","h","1","44","4"},
{"mov","b","l","1","45","4"},
{"mov","b","m","1","46","7"},
{"mov","c","a","1","4f","4"},
{"mov","c","b","1","48","4"},
{"mov","c","c","1","49","4"},
{"mov","c","d","1","4a","4"},
{"mov","c","e","1","4b","4"},
{"mov","c","h","1","4c","4"},
{"mov","c","l","1","4d","4"},
{"mov","c","m","1","4e","7"},
{"mov","d","a","1","57","4"},
{"mov","d","b","1","50","4"},
{"mov","d","c","1","51","4"},
{"mov","d","d","1","52","4"},
{"mov","d","e","1","53","4"},
{"mov","d","h","1","54","4"},
{"mov","d","l","1","55","4"},
{"mov","d","m","1","56","7"},
{"mov","d","a","1","57","4"},
{"mov","e","a","1","5f","4"},
{"mov","e","b","1","58","4"},
{"mov","e","c","1","59","4"},
{"mov","e","d","1","5a","4"},
{"mov","e","e","1","5b","4"},
{"mov","e","h","1","5c","4"},
{"mov","e","l","1","5d","4"},
{"mov","e","m","1","5e","7"},
{"mov","h","a","1","67","4"},
{"mov","h","b","1","60","4"},
{"mov","h","c","1","61","4"},
{"mov","h","d","1","62","4"},
{"mov","h","e","1","63","4"},
{"mov","h","h","1","64","4"},
{"mov","h","l","1","65","4"},
{"mov","h","m","1","66","7"},
{"mov","l","a","1","6f","4"},
{"mov","l","b","1","68","4"},
{"mov","l","c","1","69","4"},
{"mov","l","d","1","6a","4"},
{"mov","l","e","1","6b","4"},
{"mov","l","h","1","6c","4"},
{"mov","l","l","1","6d","4"},
{"mov","l","m","1","6e","7"},
{"mov","m","a","1","77","7"},
{"mov","m","b","1","70","7"},
{"mov","m","c","1","71","7"},
{"mov","m","d","1","72","7"},
{"mov","m","e","1","73","7"},
{"mov","m","h","1","74","7"},
{"mov","m","l","1","75","7"},
{"mvi","a","xx","2","3e","7"},
{"mvi","b","xx","2","06","7"},
{"mvi","c","xx","2","0e","7"},
{"mvi","d","xx","2","16","7"},
{"mvi","e","xx","2","1e","7"},
{"mvi","h","xx","2","26","7"},
{"mvi","l","xx","2","2e","7"},
{"mvi","m","xx","2","36","10"},
{"nop",null,null,"1","00","4"},
{"ora","a",null,"1","b7","4"},
{"ora","b",null,"1","b0","4"},
{"ora","c",null,"1","b1","4"},
{"ora","d",null,"1","b2","4"},
{"ora","e",null,"1","b3","4"},
{"ora","h",null,"1","b4","4"},
{"ora","l",null,"1","b5","4"},
{"ora","m",null,"1","b6","7"},
{"ori","xx",null,"2","f6","7"},
{"out","xx",null,"2","d3","10"},
{"pchl",null,null,"1","e9","6"},
{"pop","b",null,"1","c1","10"},
{"pop","d",null,"1","d1","10"},
{"pop","h",null,"1","e1","10"},
{"pop","psw",null,"1","f1","10"},
{"push","b",null,"1","c5","12"},
{"push","d",null,"1","d5","12"},
{"push","h",null,"1","e5","12"},
{"push","psw",null,"1","f5","12"},
{"ral",null,null,"1","17","4"},
{"rar",null,null,"1","1f","4"},
{"rc",null,null,"1","d8","6"},
{"ret",null,null,"1","c9","10"},
{"rim",null,null,"1","20","4"},
{"rlc",null,null,"1","07","4"},
{"rm",null,null,"1","f8","6"},
{"rnc",null,null,"1","d0","6"},
{"rnz",null,null,"1","c0","6"},
{"rp",null,null,"1","f0","6"},
{"rpe",null,null,"1","e8","6"},
{"rpo",null,null,"1","e0","6"},
{"rrc",null,null,"1","0f","4"},
{"rst","0",null,"1","c7","12"},
{"rst","1",null,"1","cf","12"},
{"rst","2",null,"1","d7","12"},
{"rst","3",null,"1","df","12"},
{"rst","4",null,"1","e7","12"},
{"rst","5",null,"1","ef","12"},
{"rst","6",null,"1","f7","12"},
{"rst","7",null,"1","ff","12"},
{"rz",null,null,"1","c8","6"},
{"sbb","a",null,"1","9f","4"},
{"sbb","b",null,"1","98","4"},
{"sbb","c",null,"1","99","4"},
{"sbb","d",null,"1","9a","4"},
{"sbb","e",null,"1","9b","4"},
{"sbb","h",null,"1","9c","4"},
{"sbb","l",null,"1","9d","4"},
{"sbb","m",null,"1","9e","7"},
{"sbi","xx",null,"2","de","7"},
{"shld","xxxx",null,"3","22","16"},
{"sim",null,null,"1","30","4"},
{"sphl",null,null,"1","f9","6"},
{"sta","xxxx",null,"3","32","13"},
{"stax","b",null,"1","02","7"},
{"stax","d",null,"1","12","7"},
{"stc",null,null,"1","37","4"},
{"sub","a",null,"1","97","4"},
{"sub","b",null,"1","90","4"},
{"sub","c",null,"1","91","4"},
{"sub","d",null,"1","92","4"},
{"sub","e",null,"1","93","4"},
{"sub","h",null,"1","94","4"},
{"sub","l",null,"1","95","4"},
{"sub","m",null,"1","96","7"},
{"sui","xx",null,"2","d6","7"},
{"xchg",null,null,"1","eb","4"},
{"xra","a",null,"1","af","4"},
{"xra","b",null,"1","a8","4"},
{"xra","c",null,"1","a9","4"},
{"xra","d",null,"1","aa","4"},
{"xra","e",null,"1","ab","4"},
{"xra","h",null,"1","ac","4"},
{"xra","l",null,"1","ad","4"},
{"xra","m",null,"1","ae","7"},
{"xri","xx",null,"2","ee","7"},
{"xthl",null,null,"1","e3","16"},


};

    String elementAt(int row,int col)
    {
        return table[row][col];
    }

    public boolean isOpcodePresent(String opc)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc)) return true;
        }
        return false;
    }

        public boolean isHexcodePresent(String hex)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,HEX).equals(hex)) return true;
        }
        return false;
    }


    public int getSizeOf(String opc)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc)) return Integer.valueOf(elementAt(i,SIZE));
        }
        return 0;
    }

    public String getOpcode(String h)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,HEX).equals(h)) return elementAt(i,OPC);
        }
        return null;
    }

    public int getTstates(String h)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,HEX).equals(h)) return Integer.valueOf(elementAt(i,TST));
        }
        return 0;
    }

    public int getNumberOfOperands(String opc)
    {
        int r=-1;
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc))
            {
                r=i;
                break;
            }
        }
        if(r<0) return -1;
        if(elementAt(r,OP1)!=null&&elementAt(r,OP2)!=null) return 2;
        else if(elementAt(r,OP1)==null&&elementAt(r,OP2)==null) return 0;
        else return 1;
    }

    public String getHex(String opc) //return null if not found
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc))
            {
                if(elementAt(i,OP1)==null||elementAt(i,OP1).equals("xxxx")||elementAt(i,OP1).equals("xx")) return elementAt(i,HEX);
            }
        }
        return null;
    }

    public String getHex(String opc,String op1) //return null if not found
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc)&&elementAt(i,OP1).equals(op1))
            {
                if(elementAt(i,OP2)==null||elementAt(i,OP2).equals("xxxx")||elementAt(i,OP2).equals("xx")) return elementAt(i,HEX);
            }
        }
        return null;
    }

    public String getHex(String opc,String op1,String op2) //return null if not found
    {
        for(int i=0;i<table.length;i++)
        {
            try{
                if(elementAt(i,OPC).equals(opc)&&elementAt(i,OP1).equals(op1)&&elementAt(i,OP2).equals(op2)) return elementAt(i,HEX);
            }catch(java.lang.NullPointerException e)
            {
                return elementAt(i,HEX);
            }
        }
        return null;
    }


     
    public String getOp1(String opc)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc)) return elementAt(i,OP1);
        }
        return null;
    }
    public String getOp2(String opc)
    {
        for(int i=0;i<table.length;i++)
        {
            if(elementAt(i,OPC).equals(opc)) return elementAt(i,OP2);
        }
        return null;
    }
}