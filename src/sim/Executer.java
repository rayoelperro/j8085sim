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
import javax.swing.table.*;

/**
 *
 * @author sinu, jins
 */
public class Executer {
    
    Register reg;
    Memory mem;
    Ports port;
    String addr,hex,errorMessage;
    OpcodeTable opt;

    String stackData[][],stackColNames[]={"SP","Register Pair","Value"};
    TableModel stackModel;
    int stackViewerRowCount;
    int tstate;

    Executer(Register r,Memory m,Ports p)
    {
        reg=new Register();
        mem=new Memory();
        opt=new OpcodeTable();

        stackViewerRowCount=0;
        tstate=-1;

        reg=r;
        mem=m;
        port=p;
    }

    void setStartAddress(String startAdd)
    {
        addr=startAdd;

        int tmp=4-addr.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              addr=dup+addr;
    }
    
    void incrementAddr()
    {
        if(addr.length()>4) { 
            addr="0000";
            return;
        }

        int ad=Integer.parseInt(addr, 16);
        ad++;
        addr=Integer.toHexString(ad);

        int tmp=4-addr.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              addr=dup+addr;
    }
    
    String getOperand(String h)
    {
        String opc=opt.getOpcode(h);
        int size=opt.getSizeOf(opc);
        String opr;
        if(size==2)
        {
            incrementAddr();
            opr=mem.read(addr);
            return opr;
        }
        else if(size==3)
        {
            String tmp;
            incrementAddr();
            tmp=mem.read(addr);
            incrementAddr();
            opr=mem.read(addr);
            opr=opr+tmp;
            return opr;
        }
        else return null;
    }

    public void insertIntoStackViewer(String sp,String rp,String val)
    {
        String temp[][]=stackData;
        stackData=new String[stackViewerRowCount+1][3];
        for(int i=0;i<stackViewerRowCount;i++)
        {
            stackData[i]=temp[i];
        }
        stackData[stackViewerRowCount][0]=sp.toUpperCase();
        stackData[stackViewerRowCount][1]=rp.toUpperCase();
        stackData[stackViewerRowCount][2]=val.toUpperCase();
        stackModel=new DefaultTableModel(stackData,stackColNames)
                {
                    boolean[] canEdit = new boolean [] {
                        false, false,false
                    };

                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return canEdit [columnIndex];
                    }
                };
        stackViewerRowCount++;
    }

    public void deleteFromStackViewer(String sp)
    {
        String temp[][]=stackData;
        int count=0;
        for(int i=0;i<stackViewerRowCount;i++)
        {
            if(temp[i][0].equalsIgnoreCase(sp))
            {
                count++;
            }
        }
        int newCount=stackViewerRowCount-count,row=0;
        stackData=new String[newCount][3];
        for(int i=0;i<stackViewerRowCount;i++)
        {
            if(!temp[i][0].equalsIgnoreCase(sp))
            {
                stackData[row]=temp[i];
                row++;
            }
        }
        stackViewerRowCount-=count;
        stackModel=new DefaultTableModel(stackData,stackColNames)
                {
                    boolean[] canEdit = new boolean [] {
                        false, false,false
                    };

                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return canEdit [columnIndex];
                    }
                };
    }

    public TableModel getStackModel()
    {
        return stackModel;
    }

    void jumpAction()
    {
        reg.setPC(getOperand(hex));
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
    }

    void pushAction(String higher,String lower,String rp)
    {
        String sp1=reg.getSP();
        int val=Integer.parseInt(sp1, 16)-1;
        mem.write(higher, val); System.out.println("val: "+val);
        val--;
        mem.write(lower,val);
        reg.setSP(Integer.toHexString(val));
        insertIntoStackViewer(reg.getSP(), rp, higher+lower);
    }

    String[] popAction()
    {
        String sp1=reg.getSP();
        deleteFromStackViewer(sp1);
        String lower=mem.read(sp1);
        int val=Integer.parseInt(sp1, 16)+1;
        reg.setSP(Integer.toHexString(val));
        sp1=reg.getSP();
        String higher=mem.read(sp1);
        val=Integer.parseInt(sp1, 16)+1;
        reg.setSP(Integer.toHexString(val));

        String temp[]={higher,lower};
        return temp;
    }

    String convertTo4(String tstr)
    {
        int tmp=4-tstr.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              tstr=dup+tstr;
        return tstr;
    }

    String convertTo8(String tstr)
    {
        int tmp=8-tstr.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              tstr=dup+tstr;
        return tstr;
    }

    void callAction()
    {
        String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+3);
        address=convertTo4(address);
        pushAction(address.substring(0,2),address.substring(2,4),"PC");
        jumpAction();
    }

    void returnAction()
    {
        String address[]=popAction();
        reg.setPC(address[0]+address[1]);
        int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
    }

    String execute(String passedAddress)
    {
        addr=passedAddress;
        tstate=-1;

        try
            {
                hex=mem.read(addr);hex=hex.toLowerCase();
            }
            catch(ArrayIndexOutOfBoundsException qq)
            {
                errorMemoryOverflow();
                return null;
            }
        if(!hex.equalsIgnoreCase("76"))
        {

             //////////////////////////
            int incrVal = opt.getSizeOf(opt.getOpcode(hex));
            //////////////////////////

            if(!opt.isHexcodePresent(hex))
            {
                errorInvalidHex(addr);
                return null;
            }

            else if (hex.equals("ce"))//aci xx
            {
                String operand=getOperand(hex);
                String result=reg.additionEffectingAC(reg.getA(), operand);
                reg.setAwithFlags(result);
            }

            else if (hex.equals("8f"))//adc a
            {
                String result=reg.getA();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(result, result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("88"))//adc b
            {
                String result=reg.getB();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("89"))//adc c
            {
                String result=reg.getC();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("8a"))//adc d
            {
                String result=reg.getD();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("8b"))//adc e
            {
                String result=reg.getE();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }


            else if (hex.equals("8c"))//adc h
            {
                String result=reg.getH();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("8d"))//adc l
            {
                String result=reg.getL();
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if (hex.equals("8e"))//adc m
            {
                String result=mem.read(reg.getHL());
                int temp=reg.getCarry();
                String t=Integer.toHexString(temp);

                result=reg.additionEffectingAC(reg.getA(), result);
                if(reg.getAuxCarry()==0)
                {
                    result=reg.additionEffectingAC(result, t);
                }
                else
                {
                    int r=Integer.parseInt(result, 16)+temp;
                    result=Integer.toHexString(r);
                }
                reg.setAwithFlags(result);
            }

            else if(hex.equals("87")) //add a
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getA());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("80")) //add b
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getB());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("81")) //add c
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getC());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("82")) //add d
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getD());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("83")) //add e
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getE());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("84")) //add h
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getH());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("85")) //add l
            {
                String result=reg.additionEffectingAC(reg.getA(), reg.getL());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("86")) //add m
            {
                String result=reg.additionEffectingAC(reg.getA(), mem.read(reg.getHL()));
                reg.setAwithFlags(result);
            }

            else if(hex.equals("c6")) //adi xx
            {
                String operand=getOperand(hex);
                String result=reg.additionEffectingAC(reg.getA(), operand);
                reg.setAwithFlags(result);
            }

            else if(hex.equals("a7")) //ana a
            {
                reg.setAwithFlags(reg.getA());
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a0")) //ana b
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getB(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a1")) //ana c
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getC(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a2")) //ana d
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getD(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a3")) //ana e
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getE(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a4")) //ana h
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getH(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a5")) //ana l
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getL(), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("a6")) //ana m
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(mem.read(reg.getHL()), 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("e6")) //ani xx
            {
                String operand=getOperand(hex);
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(operand, 16);
                int num3=num1&num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(1);
                reg.setCarry(0);
            }

            else if(hex.equals("cd")) //call xxxx
            {
                callAction();
            }

            else if(hex.equals("dc")) //cc xxxx
            {
                if(reg.getCarry()==1)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("fc")) //cm xxxx
            {
                if(reg.getSign()==1)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("2f")) //cma
            {
                int num1=~(Integer.parseInt(reg.getA(), 16));
                String num2=Integer.toHexString(num1);
                int length=num2.length();
                if(length>2)
                    num2=num2.substring(length-2);
                 reg.setA(num2);
            }

            else if(hex.equals("3f")) //cmc
            {
                if(reg.getCarry()==1) reg.setCarry(0);
                else reg.setCarry(1);
            }

            else if(hex.equals("bf")) //cmp a
            {
                String tmp;
                tmp=reg.getA();
                reg.setAwithFlags("0");
                reg.setA(tmp);
            }

            else if(hex.equals("b8")) //cmp b
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getB());
                reg.setAwithFlags(result);
                reg.setA(tmp);

            }

            else if(hex.equals("b9")) //cmp c
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getC());
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

             else if(hex.equals("ba")) //cmp d
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getD());
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

            else if(hex.equals("bb")) //cmp e
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getE());
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

             else if(hex.equals("bc")) //cmp h
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getH());
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

             else if(hex.equals("bd")) //cmp l
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getL());
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

             else if(hex.equals("be")) //cmp m
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),mem.read(reg.getHL()));
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

            else if(hex.equals("d4")) //cnc xxxx
            {
                if(reg.getCarry()==0)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("c4")) //cnz xxxx
            {
                if(reg.getZero()==0)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("f4")) //cp xxxx
            {
                if(reg.getSign()==0)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("ec")) //cpe xxxx
            {
                if(reg.getParity()==1)
                  {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("fe")) //cpi xx
            {
                String tmp;
                tmp=reg.getA();
                String result=reg.subtractionEffectingAC(reg.getA(),getOperand(hex));
                reg.setAwithFlags(result);
                reg.setA(tmp);
            }

            else if(hex.equals("e4")) //cpo xxxx
            {
                if(reg.getParity()==0)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("cc")) //cz xxxx
            {
                if(reg.getZero()==1)
                {
                    tstate=18;
                    callAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("27")) //daa
            {
                String acc=reg.getA();
                String low=acc.substring(1);
                String high=""+acc.charAt(0);
                int l=Integer.parseInt(low, 16);
                int h=Integer.parseInt(high, 16);
                String add="";
                if(h>9||reg.getCarry()==1)
                {
                    add=add+"6";
                }
                else add=add+"0";
                if(l>9||reg.getAuxCarry()==1)
                {
                    add=add+"6";
                }
                else add=add+"0";
                String result=reg.additionEffectingAC(add, reg.getA());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("09")) //dad b
            {
                int result;
                String sum;
                result=Integer.parseInt(reg.getHL(), 16)+Integer.parseInt(reg.getBC(), 16);
                sum=Integer.toHexString(result);
                if(sum.length()>4)
                {
                  sum=sum.substring(1);
                  reg.setCarry(1);
                }
                sum=convertTo4(sum);
                reg.setH(sum.substring(0,2));
                reg.setL(sum.substring(2));
            }

            else if(hex.equals("19")) //dad d
            {
                int result;
                String sum;
                result=Integer.parseInt(reg.getHL(), 16)+Integer.parseInt(reg.getDE(), 16);
                sum=Integer.toHexString(result);
                if(sum.length()>4)
                {
                  sum=sum.substring(1);
                  reg.setCarry(1);
                }
                sum=convertTo4(sum);
                reg.setH(sum.substring(0,2));
                reg.setL(sum.substring(2));
            }

            else if(hex.equals("29")) //dad h
            {
                int result;
                String sum;
                result=Integer.parseInt(reg.getHL(), 16)+Integer.parseInt(reg.getHL(), 16);
                sum=Integer.toHexString(result);
                if(sum.length()>4)
                {
                  sum=sum.substring(1);
                  reg.setCarry(1);
                }
                sum=convertTo4(sum);
                reg.setH(sum.substring(0,2));
                reg.setL(sum.substring(2));
            }


            else if(hex.equals("39")) //dad sp
            {
                int result;
                String sum;
                result=Integer.parseInt(reg.getHL(), 16)+Integer.parseInt(reg.getSP(), 16);
                sum=Integer.toHexString(result);
                if(sum.length()>4)
                {
                  sum=sum.substring(1);
                  reg.setCarry(1);
                }
                sum=convertTo4(sum);
                reg.setH(sum.substring(0,2));
                reg.setL(sum.substring(2));
            }

            else if(hex.equals("3d")) //dcr a
            {
                String operand=reg.subtractionEffectingAC(reg.getA(),"1");
                reg.setAwithFlags(operand);
            }

            else if(hex.equals("05")) //dcr b
            {
                String operand=reg.subtractionEffectingAC(reg.getB(),"1");
                reg.setBwithFlags(operand);

            }

            else if(hex.equals("0d")) //dcr c
            {
                String operand=reg.subtractionEffectingAC(reg.getC(),"1");
                reg.setCwithFlags(operand);

            }

             else if(hex.equals("15")) //dcr d
            {
                String operand=reg.subtractionEffectingAC(reg.getD(),"1");
                reg.setDwithFlags(operand);

            }

            else if(hex.equals("1d")) //dcr e
            {
                String operand=reg.subtractionEffectingAC(reg.getE(),"1");
                reg.setEwithFlags(operand);

            }

             else if(hex.equals("25")) //dcr h
            {
                String operand=reg.subtractionEffectingAC(reg.getH(),"1");
                reg.setHwithFlags(operand);

            }

             else if(hex.equals("2d")) //dcr l
            {
                String operand=reg.subtractionEffectingAC(reg.getL(),"1");
                reg.setLwithFlags(operand);

            }

             else if(hex.equals("35")) //dcr m
            {
                String adr=reg.getHL();
                String memory=mem.read(adr);
                memory=reg.subtractionEffectingAC(memory, "1");
                mem.write(memory,Integer.parseInt(adr,16));
                System.out.println("mem: "+memory+" adr: "+adr);
                //set Sign flag
                int num=Integer.parseInt(memory, 16);
                String tmp=Integer.toBinaryString(num);
                if(tmp.length()>=8)
                {
                    if(tmp.charAt(0)=='0') reg.setSign(0);
                    else reg.setSign(1);
                }
                else reg.setSign(0);

                //set Zero flag
                if(num==0) reg.setZero(1);
                else reg.setZero(0);

                //set Parity flag
                int ones=0;
                for(int i=0;i<tmp.length();i++)
                {
                    if(tmp.charAt(i)=='1') ones++;
                }
                if(ones%2==0) reg.setParity(1);
                else reg.setParity(0);

            }

            else if(hex.equals("0b")) //dcx b
            {
                String pair=reg.getBC();
                int dcr=Integer.parseInt(pair, 16)-1;
                pair=Integer.toHexString(dcr);

                 int tmp=4-pair.length();
                 String dup="";
                 for(int j=0;j<tmp;j++) dup=dup+"0";
                 pair=dup+pair;

                reg.setB(pair.substring(0,2));
                reg.setC(pair.substring(2,4));
            }

            else if(hex.equals("1b")) //dcx d
            {
                String pair=reg.getDE();
                int dcr=Integer.parseInt(pair, 16)-1;
                pair=Integer.toHexString(dcr);

                 int tmp=4-pair.length();
                 String dup="";
                 for(int j=0;j<tmp;j++) dup=dup+"0";
                 pair=dup+pair;

                reg.setD(pair.substring(0,2));
                reg.setE(pair.substring(2,4));
            }

            else if(hex.equals("2b")) //dcx h
            {
                String pair=reg.getHL();
                int dcr=Integer.parseInt(pair, 16)-1;
                pair=Integer.toHexString(dcr);

                 int tmp=4-pair.length();
                 String dup="";
                 for(int j=0;j<tmp;j++) dup=dup+"0";
                 pair=dup+pair;

                reg.setH(pair.substring(0,2));
                reg.setL(pair.substring(2,4));
            }

            else if(hex.equals("3b")) //dcx sp
            {
                String pair=reg.getSP();
                int dcr=Integer.parseInt(pair, 16)-1;
                pair=Integer.toHexString(dcr);
                if(pair.length()>4) pair=pair.substring(0, 4);
                reg.setSP(pair);
            }

            else if(hex.equals("f3")) //di
            {
                reg.setIEFF(0);
            }

            else if(hex.equals("fb")) //ei
            {
                reg.setIEFF(1);
            }

            else if(hex.equals("db")) //in xx
            {
                reg.setA(port.getPort(getOperand(hex)));
            }

            else if(hex.equals("3c")) //inr a
            {
                String operand=reg.additionEffectingAC(reg.getA(),"1");
                int temp=reg.getCarry();
                reg.setAwithFlags(operand);
                reg.setCarry(temp);
            }

            else if(hex.equals("04")) //inr b
            {
                String operand=reg.additionEffectingAC(reg.getB(),"1");
                int temp=reg.getCarry();
                reg.setBwithFlags(operand);
                reg.setCarry(temp);

            }

            else if(hex.equals("0c")) //inr c
            {
                String operand=reg.additionEffectingAC(reg.getC(),"1");
                int temp=reg.getCarry();
                reg.setCwithFlags(operand);
                reg.setCarry(temp);

            }

             else if(hex.equals("14")) //inr d
            {
                String operand=reg.additionEffectingAC(reg.getD(),"1");
                int temp=reg.getCarry();
                reg.setDwithFlags(operand);
                reg.setCarry(temp);

            }

            else if(hex.equals("1c")) //inr e
            {
                int temp=reg.getCarry();
                String operand=reg.additionEffectingAC(reg.getE(),"1");
                reg.setEwithFlags(operand);
                reg.setCarry(temp);

            }

             else if(hex.equals("24")) //inr h
            {
                int temp=reg.getCarry();
                String operand=reg.additionEffectingAC(reg.getH(),"1");
                reg.setHwithFlags(operand);
                reg.setCarry(temp);

            }

             else if(hex.equals("2c")) //inr l
            {
                int temp=reg.getCarry();
                String operand=reg.additionEffectingAC(reg.getL(),"1");
                reg.setLwithFlags(operand);
                reg.setCarry(temp);

            }

                else if(hex.equals("34")) //inr m
            {
                String adr=reg.getHL();
                String memory=mem.read(adr);
                memory=reg.additionEffectingAC(memory, "1");
                mem.write(memory,Integer.parseInt(adr,16));
                System.out.println("mem: "+memory+" adr: "+adr);
                //set Sign flag
                int num=Integer.parseInt(memory, 16);
                String tmp=Integer.toBinaryString(num);
                if(tmp.length()>=8)
                {
                    if(tmp.charAt(0)=='0') reg.setSign(0);
                    else reg.setSign(1);
                }
                else reg.setSign(0);

                //set Zero flag
                if(num==0) reg.setZero(1);
                else reg.setZero(0);

                //set Parity flag
                int ones=0;
                for(int i=0;i<tmp.length();i++)
                {
                    if(tmp.charAt(i)=='1') ones++;
                }
                if(ones%2==0) reg.setParity(1);
                else reg.setParity(0);

            }

            else if(hex.equals("03")) //inx b
            {
                String pair=reg.getBC();
                int inr=Integer.parseInt(pair, 16)+1;
                pair=Integer.toHexString(inr);
                if(pair.length()<5)
                {
                  int tmp=4-pair.length();
                  String dup="";
                  for(int j=0;j<tmp;j++) dup=dup+"0";
                  pair=dup+pair;
                }
                else
                {
                    pair=pair.substring(1, 5);

                }
                reg.setB(pair.substring(0,2));
                reg.setC(pair.substring(2,4));
            }

            else if(hex.equals("13")) //inx d
            {
                String pair=reg.getDE();
                int inr=Integer.parseInt(pair, 16)+1;
                pair=Integer.toHexString(inr);
                if(pair.length()<5)
                {
                  int tmp=4-pair.length();
                  String dup="";
                  for(int j=0;j<tmp;j++) dup=dup+"0";
                  pair=dup+pair;
                }
                else
                {
                    pair=pair.substring(1, 5);

                }
                reg.setD(pair.substring(0,2));
                reg.setE(pair.substring(2,4));
            }

            else if(hex.equals("23")) //inx h
            {
                String pair=reg.getHL();
                int inr=Integer.parseInt(pair, 16)+1;
                pair=Integer.toHexString(inr);
                if(pair.length()<5)
                {
                  int tmp=4-pair.length();
                  String dup="";
                  for(int j=0;j<tmp;j++) dup=dup+"0";
                  pair=dup+pair;
                }
                else
                {
                    pair=pair.substring(1, 5);

                }
                reg.setH(pair.substring(0,2));
                reg.setL(pair.substring(2,4));
            }

            else if(hex.equals("33")) //inx sp
            {
                String pair=reg.getSP();
                int inr=Integer.parseInt(pair, 16)+1;
                pair=Integer.toHexString(inr);
                if(pair.length()>4)
                 pair=pair.substring(1, 5);
                reg.setSP(pair);
            }

            else if(hex.equals("da")) //jc xxxx
            {
                if(reg.getCarry()==1)
                {   
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("fa")) //jm xxxx
            {
                if(reg.getSign()==1)
                {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("c3")) //jmp xxxx
            {
                jumpAction();
            }

            else if(hex.equals("d2")) //jnc xxxx
            {
                if(reg.getCarry()==0)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

             else if(hex.equals("c2")) //jnz xxxx
            {
                if(reg.getZero()==0)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("f2")) //jp xxxx
            {
                if(reg.getSign()==0)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("ea")) //jpe xxxx
            {
                if(reg.getParity()==1)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("e2")) //jpo xxxx
            {
                if(reg.getParity()==0)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("ca")) //jz xxxx
            {
                if(reg.getZero()==1)
                    {
                    tstate=10;
                    jumpAction();
                }
                else
                {
                    incrementAddr();
                    incrementAddr();
                }
            }

            else if(hex.equals("3a")) //LDA xxxx
            {
                String operand=getOperand(hex);
                reg.setA(mem.read(operand));
            }

            else if(hex.equals("0a")) //LDAX B
            {
                String addr=reg.getBC();
                reg.setA(mem.read(addr));
            }

            else if(hex.equals("1a")) //LDAX D
            {
                String addr=reg.getDE();
                reg.setA(mem.read(addr));
            }

            else if(hex.equals("2a")) //LHLD xxxx
            {
                String addr=getOperand(hex);
                reg.setL(mem.read(addr));
                addr=Integer.toHexString(Integer.parseInt(addr, 16)+1);
                reg.setH(mem.read(addr));
            }

            else if(hex.equals("01")) //lxi b,xxxx
            {
                String pair=getOperand(hex);

                reg.setB(pair.substring(0,2));
                reg.setC(pair.substring(2,4));
            }

            else if(hex.equals("11")) //lxi d,xxxx
            {
                String pair=getOperand(hex);

                reg.setD(pair.substring(0,2));
                reg.setE(pair.substring(2,4));
            }

            else if(hex.equals("21")) //lxi h,xxxx
            {
                String pair=getOperand(hex);

                reg.setH(pair.substring(0,2));
                reg.setL(pair.substring(2,4));
            }

            else if(hex.equals("01")) //lxi sp,xxxx
            {
                String pair=getOperand(hex);

                reg.setSP(pair);
            }

            else if(hex.equals("7f")) //mov a,a
            {

            }

            else if(hex.equals("78"))//mov a,b
            {
                reg.setA(reg.getB());
            }

            else if(hex.equals("79")) //mov a,c
            {
                reg.setA(reg.getC());
            }

            else if(hex.equals("7a")) //mov a,d
            {
                reg.setA(reg.getD());
            }

            else if(hex.equals("7b")) //mov a,e
            {
                reg.setA(reg.getE());
            }

            else if(hex.equals("7c")) //mov a,h
            {
                reg.setA(reg.getH());
            }

            else if(hex.equals("7d")) //mov a,l
            {
                reg.setA(reg.getL());
            }

            else if(hex.equals("7e")) //mov a,m
            {
                reg.setA(mem.read(reg.getHL()));
            }


            else if(hex.equals("40")) //mov b,b
            {

            }


            else if(hex.equals("47")) //mov b,a
            {
                reg.setB(reg.getA());
            }

            else if(hex.equals("41")) //mov b,c
            {
                reg.setB(reg.getC());
            }

            else if(hex.equals("42")) //mov b,d
            {
                reg.setB(reg.getD());
            }

            else if(hex.equals("43")) //mov b,e
            {
                reg.setB(reg.getE());
            }

            else if(hex.equals("44")) //mov b,h
            {
                reg.setB(reg.getH());
            }

            else if(hex.equals("45")) //mov b,l
            {
                reg.setB(reg.getL());
            }

            else if(hex.equals("46")) //mov b,m
            {
                reg.setB(mem.read(reg.getHL()));
            }


            else if(hex.equals("49")) //mov c,c
            {

            }


            else if(hex.equals("4f")) //mov c,a
            {
                reg.setC(reg.getA());
            }

            else if(hex.equals("48")) //mov c,b
            {
                reg.setC(reg.getB());
            }

            else if(hex.equals("4a")) //mov c,d
            {
                reg.setC(reg.getD());
            }

            else if(hex.equals("4b")) //mov c,e
            {
                reg.setC(reg.getE());
            }

            else if(hex.equals("4c")) //mov c,h
            {
                reg.setC(reg.getH());
            }

            else if(hex.equals("4d")) //mov c,l
            {
                reg.setC(reg.getL());
            }

            else if(hex.equals("4e")) //mov c,m
            {
                reg.setC(mem.read(reg.getHL()));
            }


            else if(hex.equals("52")) //mov d,d
            {

            }


            else if(hex.equals("57")) //mov d,a
            {
                reg.setD(reg.getA());
            }

            else if(hex.equals("50")) //mov d,b
            {
                reg.setD(reg.getB());
            }

            else if(hex.equals("51")) //mov d,c
            {
                reg.setD(reg.getC());
            }

            else if(hex.equals("53")) //mov d,e
            {
                reg.setD(reg.getE());
            }

            else if(hex.equals("54")) //mov d,h
            {
                reg.setD(reg.getH());
            }

            else if(hex.equals("55")) //mov d,l
            {
                reg.setD(reg.getL());
            }

            else if(hex.equals("56")) //mov d,m
            {
                reg.setD(mem.read(reg.getHL()));
            }



            else if(hex.equals("5b")) //mov e,e
            {

            }


            else if(hex.equals("5f")) //mov e,a
            {
                reg.setE(reg.getA());
            }

            else if(hex.equals("58")) //mov e,b
            {
                reg.setE(reg.getB());
            }

            else if(hex.equals("59")) //mov e,c
            {
                reg.setE(reg.getC());
            }

            else if(hex.equals("5a")) //mov e,d
            {
                reg.setE(reg.getD());
            }

            else if(hex.equals("5c")) //mov e,h
            {
                reg.setE(reg.getH());
            }

            else if(hex.equals("5d")) //mov e,l
            {
                reg.setE(reg.getL());
            }

            else if(hex.equals("5e")) //mov e,m
            {
                reg.setE(mem.read(reg.getHL()));
            }




            else if(hex.equals("64")) //mov h,h
            {

            }


            else if(hex.equals("67")) //mov h,a
            {
                reg.setH(reg.getA());
            }

            else if(hex.equals("60")) //mov h,b
            {
                reg.setH(reg.getB());
            }

            else if(hex.equals("61")) //mov h,c
            {
                reg.setH(reg.getC());
            }

            else if(hex.equals("62")) //mov h,d
            {
                reg.setH(reg.getD());
            }

            else if(hex.equals("63")) //mov h,e
            {
                reg.setH(reg.getE());
            }

            else if(hex.equals("65")) //mov h,l
            {
                reg.setH(reg.getL());
            }

            else if(hex.equals("66")) //mov h,m
            {
                reg.setH(mem.read(reg.getHL()));
            }




                else if(hex.equals("6d")) //mov l,l
            {

            }


            else if(hex.equals("6f")) //mov l,a
            {
                reg.setL(reg.getA());
            }

            else if(hex.equals("68")) //mov l,b
            {
                reg.setL(reg.getB());
            }

            else if(hex.equals("69")) //mov l,c
            {
                reg.setL(reg.getC());
            }

            else if(hex.equals("6a")) //mov l,d
            {
                reg.setL(reg.getD());
            }

            else if(hex.equals("6b")) //mov l,e
            {
                reg.setL(reg.getE());
            }

            else if(hex.equals("6c")) //mov l,h
            {
                reg.setL(reg.getH());
            }

            else if(hex.equals("6e")) //mov l,m
            {
                reg.setL(mem.read(reg.getHL()));
            }



            else if(hex.equals("77")) //mov m,a
            {
                mem.write(reg.getA(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("70")) //mov m,b
            {
                mem.write(reg.getB(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("71")) //mov m,c
            {
               mem.write(reg.getC(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("72")) //mov m,d
            {
                mem.write(reg.getD(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("73")) //mov m,e
            {
                mem.write(reg.getE(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("74")) //mov m,h
            {
                mem.write(reg.getH(),Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("75")) //mov m,l
            {
                mem.write(reg.getL(),Integer.parseInt(reg.getHL(),16));
            }



            else if(hex.equals("3e")) //mvi a,xx
            {
                String operand=getOperand(hex);
                reg.setA(operand);
            }

            else if(hex.equals("06")) //mvi b,xx
            {
                String operand=getOperand(hex);
                reg.setB(operand);
            }

            else if(hex.equals("0e")) //mvi c,xx
            {
                String operand=getOperand(hex);
                reg.setC(operand);
            }

            else if(hex.equals("16")) //mvi d,xx
            {
                String operand=getOperand(hex);
                reg.setD(operand);
            }

            else if(hex.equals("1e")) //mvi e,xx
            {
                String operand=getOperand(hex);
                reg.setE(operand);
            }

            else if(hex.equals("26")) //mvi h,xx
            {
                String operand=getOperand(hex);
                reg.setH(operand);
            }

            else if(hex.equals("2e")) //mvi l,xx
            {
                String operand=getOperand(hex);
                reg.setL(operand);
            }

            else if(hex.equals("36")) //mvi m,xx
            {
                String operand=getOperand(hex);
                mem.write(operand,Integer.parseInt(reg.getHL(),16));
            }

            else if(hex.equals("00")) //nop
            {

            }

            else if(hex.equals("b7")) //ora a
            {
                reg.setAwithFlags(reg.getA());
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b0")) //ora b
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getB(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b1")) //ora c
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getC(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b2")) //ora d
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getD(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b3")) //ora e
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getE(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b4")) //ora h
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getH(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b5")) //ora l
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getL(), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("b6")) //ora m
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(mem.read(reg.getHL()), 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("f6")) //ori xx
            {
                String operand=getOperand(hex);
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(operand, 16);
                int num3=num1|num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("d3")) //out xx
            {
                port.setPort(reg.getA(), getOperand(hex));
            }

            else if(hex.equals("e9")) //pchl
            {
                reg.setPC(reg.getHL());
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);

            }

            else if(hex.equals("c1")) //pop b
            {
                String val[]=popAction();
                reg.setB(val[0]);
                reg.setC(val[1]);
            }

            else if(hex.equals("d1")) //pop d
            {
                String val[]=popAction();
                reg.setD(val[0]);
                reg.setE(val[1]);
            }

            else if(hex.equals("e1")) //pop h
            {
                String val[]=popAction();
                reg.setH(val[0]);
                reg.setL(val[1]);
            }

            else if(hex.equals("f1")) //pop psw
            {
                String val[]=popAction();
                reg.setA(val[0]);
                reg.setF(val[1]);
            }

            else if(hex.equals("c5")) //push b
            {
                pushAction(reg.getB(), reg.getC(), "BC");
            }

            else if(hex.equals("d5")) //push d
            {
                pushAction(reg.getD(), reg.getE(), "DE");
            }

            else if(hex.equals("e5")) //push h
            {
                pushAction(reg.getH(), reg.getL(), "HL");
            }

            else if(hex.equals("f5")) //push psw
            {
                pushAction(reg.getA(), reg.getF(), "PSW");
            }

            else if(hex.equals("17")) //ral
            {
                String bin_accum=Integer.toBinaryString(Integer.parseInt(reg.getA(), 16));
                bin_accum=convertTo8(bin_accum);
                String acc_c=bin_accum+Integer.toString(reg.getCarry());
                reg.setA(Integer.toHexString(Integer.parseInt(acc_c.substring(1), 2)));
                reg.setCarry(Integer.parseInt(acc_c.substring(0, 1)));
            }

            else if(hex.equals("1f")) //rar
            {
                String bin_accum=Integer.toBinaryString(Integer.parseInt(reg.getA(), 16));
                bin_accum=convertTo8(bin_accum);
                String acc_c=Integer.toString(reg.getCarry())+bin_accum;
                reg.setA(Integer.toHexString(Integer.parseInt(acc_c.substring(0,8), 2)));
                reg.setCarry(Integer.parseInt(acc_c.substring(8)));
            }


            else if(hex.equals("d8")) //rc
            {
                if(reg.getCarry()==1)
                {
                    tstate=12;
                    returnAction();
                }
            }


            else if(hex.equals("c9")) //ret
            {
                returnAction();
            }

            else if(hex.equals("07")) //rlc
            {
                String bin_accum=Integer.toBinaryString(Integer.parseInt(reg.getA(), 16));
                bin_accum=convertTo8(bin_accum);
                String acc_d7=bin_accum.substring(0, 1);
                reg.setA(Integer.toHexString(Integer.parseInt(bin_accum.substring(1, 8)+acc_d7,2)));
                reg.setCarry(Integer.parseInt(acc_d7));
            }


            else if(hex.equals("f8")) //rm
            {
                if(reg.getSign()==1)
                    {
                    tstate=12;
                    returnAction();
                }
            }


            else if(hex.equals("d0")) //rnc
            {
                if(reg.getCarry()==0)
                    {
                    tstate=12;
                    returnAction();
                }
            }

             else if(hex.equals("c0")) //rnz
            {
                if(reg.getZero()==0)
                    {
                    tstate=12;
                    returnAction();
                }
            }

            else if(hex.equals("f0")) //rp
            {
                if(reg.getSign()==0)
                    {
                    tstate=12;
                    returnAction();
                }
            }

            else if(hex.equals("e8")) //rpe
            {
                if(reg.getParity()==1)
                   {
                    tstate=12;
                    returnAction();
                }
            }

            else if(hex.equals("e0")) //rpo
            {
                if(reg.getParity()==0)
                    {
                    tstate=12;
                    returnAction();
                }
            }

            else if(hex.equals("0f")) //rrc
            {
                String bin_accum=Integer.toBinaryString(Integer.parseInt(reg.getA(), 16));
                bin_accum=convertTo8(bin_accum);
                String acc_d0=bin_accum.substring(7);
                reg.setA(Integer.toHexString(Integer.parseInt(acc_d0+bin_accum.substring(0, 7),2)));
                reg.setCarry(Integer.parseInt(acc_d0));
            }

            else if(hex.equals("20")) //rim
            {
                String tem_acc;
                tem_acc="0"+reg.getI7()+reg.getI6()+reg.getI5()+reg.getIEFF()+reg.getM75()+reg.getM65()+reg.getM55();
                reg.setA(tem_acc);
            }

            else if(hex.equals("c7")) //rst 0
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0000");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("cf")) //rst 1
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0008");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("d7")) //rst 2
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0010");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("df")) //rst 3
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0018");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("e7")) //rst 4
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0020");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("ef")) //rst 5
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0028");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("f7")) //rst 6
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0030");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }

            else if(hex.equals("ff")) //rst 7
            {
                String address=Integer.toHexString(Integer.parseInt(reg.getPC(), 16)+1);
                address=convertTo4(address);
                pushAction(address.substring(0,2),address.substring(2,4),"PC");

                reg.setPC("0038");
                int kk=Integer.parseInt(reg.getPC(), 16)-1;
                addr=Integer.toHexString(kk);
            }


            else if(hex.equals("c8")) //rz
            {
                if(reg.getZero()==1)
                    {
                    tstate=12;
                    returnAction();
                }
            }

            else if(hex.equals("9f")) //sbb a
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getA() );
                String result;

                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));

                reg.setAwithFlags(result);
            }


            else if(hex.equals("98")) //sbb b
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getB() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("99")) //sbb c
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getC() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("9a")) //sbb d
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getD() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("9b")) //sbb e
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getE() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("9c")) //sbb h
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getH() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }


            else if(hex.equals("9d")) //sbb l
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),reg.getL() );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("9e")) //sbb m
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),mem.read(reg.getHL()) );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("de")) //sbi xx
            {
                String op2=reg.subtractionEffectingAC(reg.getA(),getOperand(hex) );

                String result;
                if(reg.getAuxCarry()==0)
                {
                    result=reg.subtractionEffectingAC(op2, Integer.toHexString(reg.getCarry()));
                }
                else
                {
                    int r=Integer.parseInt(op2, 16)-reg.getCarry();
                    result=Integer.toHexString(r);
                }

                reg.setAwithFlags(result);
            }

            else if(hex.equals("22")) //shld xxxx
            {
               String addr=getOperand(hex);
               int address=Integer.parseInt(addr, 16);
               mem.write(reg.getL(), address);
               address++;
               mem.write(reg.getH(), address);
            }

            else if(hex.equals("30")) //sim
            {
               String bin_acc=Integer.toBinaryString(Integer.parseInt(reg.getA(), 16));
               if((Integer.parseInt(bin_acc.substring(4, 5))==1))
               {
                   reg.setM55(Integer.parseInt(bin_acc.substring(7, 8)));
                   reg.setM65(Integer.parseInt(bin_acc.substring(6, 7)));
                   reg.setM75(Integer.parseInt(bin_acc.substring(5, 6)));
               }
               reg.setReset75(bin_acc.substring(3, 4));
            }

            else if(hex.equals("f9")) //sphl
            {
                reg.setSP(reg.getHL());
            }

            else if(hex.equals("32")) //sta xxxx
            {
                mem.write(reg.getA(), Integer.parseInt(getOperand(hex), 16));
            }

            else if(hex.equals("02")) //stax b
            {
                mem.write(reg.getA(), Integer.parseInt(reg.getBC(), 16));
            }

            else if(hex.equals("12")) //stax d
            {
                mem.write(reg.getA(), Integer.parseInt(reg.getDE(), 16));
            }

            else if(hex.equals("37")) //stc
            {
                reg.setCarry(1);
            }

            else if(hex.equals("97")) //sub a
            {
                reg.setAwithFlags("0");

            }

            else if(hex.equals("90")) //sub b
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getB());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("91")) //sub c
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getC());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("92")) //sub d
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getD());
                reg.setAwithFlags(result);
            }


            else if(hex.equals("93")) //sub e
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getE());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("94")) //sub h
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getH());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("95")) //sub l
            {
                String result=reg.subtractionEffectingAC(reg.getA(),reg.getL());
                reg.setAwithFlags(result);
            }

            else if(hex.equals("96")) //sub m
            {
                String result=reg.subtractionEffectingAC(reg.getA(), mem.read(reg.getHL()));
                reg.setAwithFlags(result);
            }

            else if(hex.equals("d6")) //sui xx
            {
                String result=reg.subtractionEffectingAC(reg.getA(), getOperand(hex));
                reg.setAwithFlags(result);
            }

            else if(hex.equals("eb")) //xchg
            {
                String h=reg.getH();
                String l=reg.getL();
                reg.setH(reg.getD());
                reg.setL(reg.getE());
                reg.setD(h);
                reg.setE(l);
            }

            else if(hex.equals("af")) //xra a
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num3=num1^num1;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("a8")) //xra b
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getB(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("a9")) //xra c
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getC(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("aa")) //xra d
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getD(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("ab")) //xra e
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getE(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("ac")) //xra h
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getH(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("ad")) //xra l
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(reg.getL(), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("ae")) //xra m
            {
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(mem.read(reg.getHL()), 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("ee")) //xri xx
            {
                String operand=getOperand(hex);
                int num1=Integer.parseInt(reg.getA(), 16);
                int num2=Integer.parseInt(operand, 16);
                int num3=num1^num2;
                reg.setAwithFlags(Integer.toHexString(num3));
                reg.setAuxCarry(0);
                reg.setCarry(0);
            }

            else if(hex.equals("e3")) //xthl
            {
                String sp=reg.getSP();
                String h=reg.getH();
                String l=reg.getL();
                reg.setL(mem.read(sp));
                mem.write(l, Integer.parseInt(sp, 16));
                sp=Integer.toHexString(Integer.parseInt(sp, 16)+1);
                reg.setH(mem.read(sp));
                mem.write(h, Integer.parseInt(sp ,16));
            }




            if(tstate<0) tstate=opt.getTstates(hex);

            ////////////////////////////
            /*
            int firstAddr = Integer.parseInt(passedAddress, 16);
            int secondAddr = Integer.parseInt(addr, 16);
            if(!branch && (secondAddr != (firstAddr + incrVal - 1)))
            {
                secondAddr = firstAddr + incrVal - 1;
                addr = convertTo4(Integer.toHexString(secondAddr));
            }
            System.out.println("firstAddr = "+firstAddr);
            System.out.println("secondAddr = "+secondAddr);
            System.out.println("finalAddr = "+addr);
             */
            ////////////////////////////

            incrementAddr();
            reg.setPC(addr);
            return addr;
        }
        else
        {
            if(tstate<0) tstate=opt.getTstates(hex);
            return null;
        } //Reached HLT statement
    }

    public int getTstates()
    {
        return tstate;
    }
    
    void errorInvalidHex(String adr)
    {
        if(errorMessage==null) errorMessage="";
        errorMessage=errorMessage+"\nInvalid Hexcode at Address "+adr;
    }
    void errorMemoryOverflow()
    {
        if(errorMessage==null) errorMessage="";
        errorMessage=errorMessage+"\nFATAL: Memory Overflow error. Cannot continue execution. Check whether 'HLT' or '76' instruction is reachable.";
    }

}
