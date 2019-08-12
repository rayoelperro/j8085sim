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
 *    Copyright 2010 Sinu John, Jinsmon Kurian
 */

package sim;

/**
* @author sinu, jins
*/
class Register
{
    String a,b,c,d,e,h,l,sp,pc,f;
    int cy,p,ac,z,s;
    int ieff=1,m75,m65,m55,i7,i6,i5;
    String reset75=null; //reset75 can have 3 values - null, 0 and 1

    Register()
    {
        a="00"; f="00";
        b="00"; c="00";
        d="00"; e="00";
        h="00"; l="00";
        sp="ffff";
        pc="0000";
    }

    int getIEFF()
    {
        return ieff;
    }

    int getI7()
    {
        return i7;
    }

    int getI6()
    {
        return i6;
    }

    int getI5()
    {
        return i5;
    }

    int getM75()
    {
        return m75;
    }

    int getM65()
    {
        return m65;
    }

    int getM55()
    {
        return m55;
    }

    void setIEFF(int n)
    {
        ieff=n;
    }

    void setI7(int n)
    {
        i7=n;
    }

    void setI6(int n)
    {
        i6=n;
    }

    void setI5(int n)
    {
        i5=n;
    }

    void setM75(int n)
    {
        m75=n;
    }

    void setM65(int n)
    {
        m65=n;
    }

    void setM55(int n)
    {
        m55=n;
    }

    void setReset75(String n)
    {
        reset75=n;
    }

    String getReset75()
    {
        return reset75;
    }

    String getA()
    {
        return a;
    }

    String getF()
    {
        String fstring;
        fstring=Integer.toString(s)+Integer.toString(z)+"0"+Integer.toString(ac)+"0"+Integer.toString(p)+"0"+Integer.toString(cy);
        f=Integer.toHexString(Integer.parseInt(fstring, 2));
        return f;
    }

    String getB()
    {
        return b;
    }

    String getC()
    {
        return c;
    }

    String getD()
    {
        return d;
    }

    String getE()
    {
        return e;
    }

    String getH()
    {
        return h;
    }

    String getL()
    {
        return l;
    }

    String getSP()
    {
        return sp;
    }

    String getPC()
    {
        return pc;
    }

    String getBC()
    {
        return b+c;
    }

    String getDE()
    {
        return d+e;
    }

    String getHL()
    {
        return h+l;
    }

    int getSign()
    {
        return s;
    }

    int getZero()
    {
        return z;
    }

    int getAuxCarry()
    {
        return ac;
    }

    int getParity()
    {
        return p;
    }

    int getCarry()
    {
        return cy;
    }


  ////
    void setA(String A)
    {
        //Covert to 2digit hex
        int tmp=2-A.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              A=dup+A;
        ////////////////////////////////
        a=A;
    }

    void setF(String F)
    {
        f=F;
        String bin_f=Integer.toBinaryString(Integer.parseInt(f, 16));
        //Covert to 8digit binary
        int tmp=8-bin_f.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              bin_f=dup+bin_f;
        ////////////////////////////////
        setSign(Integer.parseInt(bin_f.substring(0, 1)));
        setZero(Integer.parseInt(bin_f.substring(1, 2)));
        setAuxCarry(Integer.parseInt(bin_f.substring(3, 4)));
        setParity(Integer.parseInt(bin_f.substring(5, 6)));
        setCarry(Integer.parseInt(bin_f.substring(7, 8)));
    }

    void setB(String B)
    {
        //Covert to 2digit hex
        int tmp=2-B.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              B=dup+B;
        ////////////////////////////////
        b=B;
    }

    void setC(String C)
    {
        //Covert to 2digit hex
        int tmp=2-C.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              C=dup+C;
        ////////////////////////////////
        c=C;
    }

    void setD(String D)
    {
        //Covert to 2digit hex
        int tmp=2-D.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              D=dup+D;
        ////////////////////////////////
        d=D;
    }

    void setE(String E)
    {
        //Covert to 2digit hex
        int tmp=2-E.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              E=dup+E;
        ////////////////////////////////
        e=E;
    }

    void setH(String H)
    {
        //Covert to 2digit hex
        int tmp=2-H.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              H=dup+H;
        ////////////////////////////////
        h=H;
    }

    void setL(String L)
    {
        //Covert to 2digit hex
        int tmp=2-L.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              L=dup+L;
        ////////////////////////////////
        l=L;
    }

    void setSP(String SP)
    {
        //Covert to 4digit hex
        int tmp=4-SP.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              SP=dup+SP;
        ////////////////////////////////
        sp=SP;
    }

    void setPC(String PC)
    {
        //Covert to 4digit hex
        int tmp=4-PC.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              PC=dup+PC;
        ////////////////////////////////
        pc=PC;
    }

    void setSign(int signbit)
    {
        s=signbit;
    }

    void setZero(int zerobit)
    {
        z=zerobit;
    }

    void setAuxCarry(int ACbit)
    {
        ac=ACbit;
    }

    void setParity(int paritybit)
    {
        p=paritybit;
    }

    void setCarry(int carrybit)
    {
        cy=carrybit;
    }

    void setAwithFlags(String A)
    {
        //Set carry flag
        if(A.length()>2)
        {
            setCarry(1);
            String tmp=A.charAt(A.length()-2)+"";
            tmp=tmp+A.charAt(A.length()-1);
            A=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(A, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-A.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    A=dup+A;

        a=A;
    }

    void setBwithFlags(String B)
    {
        //Set carry flag
        if(B.length()>2)
        {
            setCarry(1);
            String tmp=B.charAt(B.length()-2)+"";
            tmp=tmp+B.charAt(B.length()-1);
            B=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(B, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-B.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    B=dup+B;
        b=B;
    }

    void setCwithFlags(String C)
    {
        //Set carry flag
        if(C.length()>2)
        {
            setCarry(1);
            String tmp=C.charAt(C.length()-2)+"";
            tmp=tmp+C.charAt(C.length()-1);
            C=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(C, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-C.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    C=dup+C;
        c=C;
    }

    void setDwithFlags(String D)
    {
        //Set carry flag
        if(D.length()>2)
        {
            setCarry(1);
            String tmp=D.charAt(D.length()-2)+"";
            tmp=tmp+D.charAt(D.length()-1);
            D=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(D, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-D.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    D=dup+D;
        d=D;
    }

    void setEwithFlags(String E)
    {
        //Set carry flag
        if(E.length()>2)
        {
            setCarry(1);
            String tmp=E.charAt(E.length()-2)+"";
            tmp=tmp+E.charAt(E.length()-1);
            E=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(E, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-E.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    E=dup+E;
        e=E;
    }

    void setHwithFlags(String H)
    {
        //Set carry flag
        if(H.length()>2)
        {
            setCarry(1);
            String tmp=H.charAt(H.length()-2)+"";
            tmp=tmp+H.charAt(H.length()-1);
            H=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(H, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-H.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    H=dup+H;
        h=H;
    }

    void setLwithFlags(String L)
    {
        //Set carry flag
        if(L.length()>2)
        {
            setCarry(1);
            String tmp=L.charAt(L.length()-2)+"";
            tmp=tmp+L.charAt(L.length()-1);
            L=tmp;
        }
        else setCarry(0);

        //set Sign flag
        int num=Integer.parseInt(L, 16);
        String tmp=Integer.toBinaryString(num);
        if(tmp.length()>=8)
        {
            if(tmp.charAt(0)=='0') setSign(0);
            else setSign(1);
        }
        else setSign(0);

        //set Zero flag
        if(num==0) setZero(1);
        else setZero(0);

        //set Parity flag
        int ones=0;
        for(int i=0;i<tmp.length();i++)
        {
            if(tmp.charAt(i)=='1') ones++;
        }
        if(ones%2==0) setParity(1);
        else setParity(0);

        int temp=2-L.length();
        String dup="";
        for(int j=0;j<temp;j++) dup=dup+"0";
                    L=dup+L;
        l=L;
    }

    String additionEffectingAC(String opr1,String opr2)
    {
        int num1=Integer.parseInt(opr1, 16);
        int num2=Integer.parseInt(opr2, 16);
        String bopr1=Integer.toBinaryString(num1);
        String bopr2=Integer.toBinaryString(num2);

        //convert bopr1 to 8bit binary
        int tmp=8-bopr1.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
                    bopr1=dup+bopr1;

        //convert bopr2 to 8bit binary
        tmp=8-bopr2.length();
        dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
                    bopr2=dup+bopr2;
        //////////////////////////////////////
        bopr1=bopr1.substring(4, 8);
        bopr2=bopr2.substring(4, 8);
        num1=Integer.parseInt(bopr1, 2);
        num2=Integer.parseInt(bopr2, 2);
        int temp=num1+num2;
        String result=Integer.toBinaryString(temp);
        if(result.length()>4) setAuxCarry(1);
        else setAuxCarry(0);

        num1=Integer.parseInt(opr1, 16);
        num2=Integer.parseInt(opr2, 16);
        temp=num1+num2;
        return Integer.toHexString(temp);
    }

    String subtractionEffectingAC(String opr1,String opr2)
    {
        int num1=Integer.parseInt(opr1, 16);
        int num2=Integer.parseInt(opr2, 16);
        String bopr1=Integer.toBinaryString(num1);
        String bopr2=Integer.toBinaryString(num2);

        //convert bopr1 to 8bit binary
        int tmp=8-bopr1.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
                    bopr1=dup+bopr1;

        //convert bopr2 to 8bit binary
        tmp=8-bopr2.length();
        dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
                    bopr2=dup+bopr2;
        //////////////////////////////////////
        bopr1=bopr1.substring(4, 8);
        bopr2=bopr2.substring(4, 8);
        num1=Integer.parseInt(bopr1, 2);
        num2=Integer.parseInt(bopr2, 2);
        if(num1<num2)
           setAuxCarry(1);
        else
            setAuxCarry(0);
        num1=Integer.parseInt(opr1, 16);
        num2=Integer.parseInt(opr2, 16);
        int temp=num1-num2;
        String retString=Integer.toHexString(temp);

        //if(retString.length()>2) retString=retString.substring(retString.length()-2, retString.length());
        //else
        if(retString.length()<=2)
        {
        tmp=2-retString.length();
        dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              retString=dup+retString;
        }
        return retString;//Integer.toHexString(temp);
    }


}