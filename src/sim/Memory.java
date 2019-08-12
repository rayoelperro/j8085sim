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

class Memory
{
    final int length=65536;
    String mem[]=new String[length];
    boolean changed=false;
    Memory()
    {
        for(int i=0;i<length;i++)
            mem[i]="08"; //There is no opcode with 08 hex
    }
    public void write(String hex,int pos)
    {
        changed=true;     
        int tmp=2-hex.length();
        String dup="";
        for(int j=0;j<tmp;j++) dup=dup+"0";
              hex=dup+hex;
        mem[pos]=hex;
    }
    public String read(String chpos)
    {
        int pos=Integer.valueOf(chpos, 16);
        return mem[pos].toUpperCase();
    }
    String[] getMemoryArray()
    {
        return mem;
    }
    void setUnchanged()
    {
        changed=false;
    }
    boolean isChanged()
    {
        return changed;
    }

}