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

class Ports
{
    String ports[];
    Ports()
    {
        ports=new String[256];
        for(int i=0;i<256;i++) ports[i]="00";
    }
    void setPort(String hex,String pos)
    {
        int p=Integer.parseInt(pos, 16);
        ports[p]=hex.toUpperCase();
    }
    String getPort(String pos)
    {
        String hex=ports[Integer.parseInt(pos, 16)];
        int tmp=2-hex.length(); //convert to 2digit hex
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              hex=dup+hex;
        return hex;
    }
    String getPortBinary(String pos)
    {
        String hex=ports[Integer.parseInt(pos, 16)];
        int temp=Integer.parseInt(hex, 16);
        String binstr=Integer.toBinaryString(temp);
        int tmp=8-binstr.length(); //convert to 8digit binary
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              binstr=dup+binstr;
        return binstr;
    }
}