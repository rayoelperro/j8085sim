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

/*
 * MainFrame.java
 *
 * Created on Feb 21, 2010, 7:13:25 PM
 */

package sim;

import java.awt.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.io.*;

//for cut copy actions
import java.util.HashMap;
import javax.swing.*;
import javax.swing.text.*;
/////////////////////////
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import javax.swing.undo.*;

import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 *
 * @author sinu
 */
public class MainFrame extends javax.swing.JFrame implements TableModelListener,Runnable{

    Register r;
    Memory m;
    Ports ports;
    SynAnalyser sa;
    Executer exec;
    String rowData[][],colNames[]={"Memory Location","Contents"};
    TableModel tmodel;

    Thread execThread;
    String memAddress;
    boolean debugMode;
    boolean compiled,suspended;

    String portList[];
    SpinnerModel smodel;

    boolean virgin;
    File currentFile;
    String initialdoc,finaldoc;

    HashMap<Object, Action> actions; //for cut copy
    JMenuItem cutMenu,copyMenu,pasteMenu,selectAllMenu;
    
    //for Symbol Viewer
    String symRows[][],symCols[]={"Symbol","Value"};
    TableModel symModel;

    int frequency; //in Hz
    boolean intr,rst55,rst65,rst75,trap; //for handling interrupts
    int rstNo;

    MutableAttributeSet highlightattr = new SimpleAttributeSet();
    MutableAttributeSet unhighlightattr = new SimpleAttributeSet();

    javax.swing.filechooser.FileFilter simFilter= new ExtendedFileFilter("8085 sim files", "sim");
    javax.swing.filechooser.FileFilter txtFilter= new ExtendedFileFilter("Text files", "txt");

    UndoManager history=new UndoManager();
    protected class MyUndoableEditListener
          implements UndoableEditListener {
            public void undoableEditHappened(UndoableEditEvent e) {
                if(!e.getEdit().getPresentationName().equals("style change"))
                {
                    history.addEdit(e.getEdit());
                    if(history.canUndo()) undoButton.setEnabled(true);
                    else undoButton.setEnabled(false);
                } //Add undo events only if its not a style change
            }
        }

    /** Creates new form MainFrame */
    public MainFrame() {
        showSplash(2000);

        initComponents();
        r=new Register();
        m=new Memory();
        ports=new Ports();

        debugMode=false;
        compiled=false;
        suspended=false;

        pgmEditor.setText("");
        virgin=true;
        currentFile=null;
        initialdoc=pgmEditor.getText();
        finaldoc=null;
        setWindowTitle();
        pgmEditor.requestFocusInWindow();

        pgmEditor.getDocument().addUndoableEditListener(new MyUndoableEditListener());

        frequency=10;
        intr=rst55=rst65=rst75=trap=false;
        rstNo=0;

        portList=new String[256];
        for(int i=0;i<256;i++) //enter data to portList
        {
            String temp=Integer.toHexString(i);
            int tmp=2-temp.length();
            String dup="";
            for(int j=0;j<tmp;j++) dup=dup+"0";
            temp=dup+temp;
            temp=temp.toUpperCase();
            portList[i]=temp;
        }
        smodel=new SpinnerListModel(portList);
        portSpinner.setModel(smodel);

        StyleConstants.setBackground(highlightattr, Color.YELLOW);
        StyleConstants.setBackground(unhighlightattr, Color.WHITE);

        actions=createActionTable(pgmEditor); //for cut copy
        cutMenu=editMenu.add(getActionByName(DefaultEditorKit.cutAction));
        cutMenu.setText("Cut");
        cutMenu.setIcon(new ImageIcon(getClass().getResource("/sim/images/small/edit-cut.png")));
        cutMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));

        copyMenu=editMenu.add(getActionByName(DefaultEditorKit.copyAction));
        copyMenu.setText("Copy");
        copyMenu.setIcon(new ImageIcon(getClass().getResource("/sim/images/small/edit-copy.png")));
        copyMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));

        pasteMenu=editMenu.add(getActionByName(DefaultEditorKit.pasteAction));
        pasteMenu.setText("Paste");
        pasteMenu.setIcon(new ImageIcon(getClass().getResource("/sim/images/small/edit-paste.png")));
        pasteMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        editMenu.addSeparator();
        selectAllMenu=editMenu.add(getActionByName(DefaultEditorKit.selectAllAction));
        selectAllMenu.setText("Select All");
        selectAllMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));

        cutButton.setAction(getActionByName(DefaultEditorKit.cutAction));
        cutButton.setIcon(new ImageIcon(getClass().getResource("/sim/images/edit-cut.png")));
        cutButton.setText("");
        cutButton.setToolTipText("Cut");
        copyButton.setAction(getActionByName(DefaultEditorKit.copyAction));
        copyButton.setIcon(new ImageIcon(getClass().getResource("/sim/images/edit-copy.png")));
        copyButton.setText("");
        copyButton.setToolTipText("Copy");
        pasteButton.setAction(getActionByName(DefaultEditorKit.pasteAction));
        pasteButton.setIcon(new ImageIcon(getClass().getResource("/sim/images/edit-paste.png")));
        pasteButton.setText("");
        pasteButton.setToolTipText("Paste");
        updateMemoryViewer();
        updatePortViewer();
        updateRegViewer();

        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    //The following two methods allow us to find an
    //action provided by the editor kit by its name.
    private HashMap<Object, Action> createActionTable(JTextComponent textComponent) {
        HashMap<Object, Action> actions = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            actions.put(a.getValue(Action.NAME), a);
        }
	return actions;
    }

    private Action getActionByName(String name) {
        return actions.get(name);
    }
    //////////////////////////////////////////////////////////////////////////////////////

    public void run()
    {
        StyledDocument doc=pgmEditor.getStyledDocument();
        HashMap lineDetail=sa.getLineInfo();
        int LINELIMITS[]={0,0};
        int tstate=0;

        while(memAddress!=null)
        {
            //for highlighting while executing
            doc.setCharacterAttributes(LINELIMITS[0], LINELIMITS[1], unhighlightattr, false);
            LINELIMITS=(int[])lineDetail.get(memAddress);
            if(LINELIMITS==null)
            {
                LINELIMITS=new int[]{0,0};
            }
            doc.setCharacterAttributes(LINELIMITS[0], LINELIMITS[1], highlightattr, false);
            ///////////////////////////////////////////////////////////////////////////////////
            String tempAddr=memAddress;
            memAddress=exec.execute(memAddress);
            tstate=exec.getTstates();
            updateRegViewer();

            if(tstate<0)
            {
                outputArea.setText(outputArea.getText()+"\n["+tempAddr+"]Invalid opcode present");
                break;
            }

            //if(isBetween(startAddress.getText(), tempAddr, endAddress.getText()))
            //{
                if(m.isChanged())    updateMemoryViewer();
            //}
            m.setUnchanged();
            
            updatePortViewer();
            updateStackViewer();
            System.out.println("threading.. "+memAddress);
            if(debugMode)
            {
                if(exec.errorMessage!=null)
                {
                    if(outputArea.getText()==null) outputArea.setText("Execution Errors:\n");
                    outputArea.setText(outputArea.getText()+exec.errorMessage);
                    exec.errorMessage=null;
                }
                suspended=true;
            }
            try
            {
                if(!suspended){
                float d=(tstate*1000)/frequency;
                int delay=(int)d; System.out.print("tstate: "+tstate+" delay: "+d);
                Thread.sleep(delay);
                }
                synchronized(this)
                {
                    while(suspended) wait();
                }
            }
            catch(InterruptedException qq)
            {
            }
            if(r.getReset75()!=null){
                if(r.getReset75().equals("1")) {
                    rst75=false;
                    r.setReset75(null);
                }
                else if(r.getReset75().equals("0")) {
                    r.setReset75(null);
                }
            }
            if(trap)
            {
                trap=false;
                exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                r.setPC("0024");
                memAddress=r.getPC();
            }
            else if(r.getIEFF()==1)
            {
                if(rst75&&r.getM75()==0)
                {
                    r.setIEFF(0);
                    rst75=false;
                    r.setI7(0);
                    exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                    r.setPC("003c");
                    memAddress=r.getPC();
                }
                else if(rst65&&r.getM65()==0)
                {
                    r.setIEFF(0);
                    rst65=false;
                    r.setI6(0);
                    exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                    r.setPC("0034");
                    memAddress=r.getPC();
                }
                else if(rst55&&r.getM55()==0)
                {
                    r.setIEFF(0);
                    rst55=false;
                    r.setI5(0);
                    exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                    r.setPC("002c");
                    memAddress=r.getPC();
                }
                else if(intr)
                {
                    r.setIEFF(0);
                    intr=false;
                    int n=rstNo;
                    switch(n)
                    {
                        case 0:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0000");
                            memAddress=r.getPC();
                            break;
                        case 1:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0008");
                            memAddress=r.getPC();
                            break;
                        case 2:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0010");
                            memAddress=r.getPC();
                            break;
                        case 3:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0018");
                            memAddress=r.getPC();
                            break;
                        case 4:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0020");
                            memAddress=r.getPC();
                            break;
                        case 5:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0028");
                            memAddress=r.getPC();
                            break;
                        case 6:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0030");
                            memAddress=r.getPC();
                            break;
                        case 7:
                            exec.pushAction(memAddress.substring(0,2),memAddress.substring(2,4),"PC");
                            r.setPC("0038");
                            memAddress=r.getPC();
                            break;
                    }
                }
            }
        }
        if(exec.errorMessage==null)
                outputArea.setText("Execution completed successfully");
            else
                outputArea.setText("Execution Errors:"+exec.errorMessage);
        debugMode=false;
        compiled=false;
        suspended=false;
        stopButton.setEnabled(false);
        stopMenu.setEnabled(false);
        executeButton.setEnabled(true);
    }

    synchronized void resumeExecution()
    {
        suspended=false;
        notify();
    }

    boolean isBetween(String addr1,String addr,String addr2)
    {
        int ad1=Integer.parseInt(addr1, 16);
        int ad=Integer.parseInt(addr, 16);
        int ad2=Integer.parseInt(addr2, 16);
        if(ad>=ad1&&ad<=ad2) return true;
        else return false;
    }

    public void setFrequency(int frq)
    {
        frequency=frq;
    }

    public void updateMemoryViewer()
    {
        int start=Integer.parseInt(startAddress.getText(), 16);
        int end=Integer.parseInt(endAddress.getText(), 16);
        if(start>end)
        {
            outputArea.setText(outputArea.getText()+"\nInvalid memory bounds. Check 'Start' and 'End' addresses.");
            return;
        }
        //TableModel tempModel=tmodel;
        rowData=new String[end-start+1][2];
        for(int i=0;i<=(end-start);i++)
        {
            String memloc=Integer.toHexString(start+i);

            //to convert memloc to 4digit hex
            int tmp=4-memloc.length();
            String dup="";
            for(int j=0;j<tmp;j++) dup=dup+"0";
                    memloc=dup+memloc;
            ////////////////////////////////////

            memloc=memloc.toUpperCase();
            rowData[i][0]=memloc;
            rowData[i][1]=m.read(memloc);
        }
        tmodel=new DefaultTableModel(rowData,colNames)
                {
                    boolean[] canEdit = new boolean [] {
                        false, true
                    };

                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return canEdit [columnIndex];
                    }
                };
        tmodel.addTableModelListener(this);
        //if(tempModel!=tmodel)
        memViewer.setModel(tmodel);
    }

    public void updateSymbolViewer()
    {
        HashMap symbols=sa.getSymbolInfo();
        Set set=symbols.entrySet();
        Iterator itr=set.iterator();
        int row=0;
        final int SYM=0,VAL=1;
        symRows=new String[symbols.size()][2];
        while(itr.hasNext())
        {
            Map.Entry element=(Map.Entry)itr.next();
            symRows[row][SYM]=element.getKey().toString().toUpperCase();
            symRows[row][VAL]=element.getValue().toString().toUpperCase();
            row++;
        }


        symModel=new DefaultTableModel(symRows,symCols)
                {
                    boolean[] canEdit = new boolean [] {
                        false, false
                    };

                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return canEdit [columnIndex];
                    }
                };
        //symModel.addTableModelListener(this);
        symbolViewer.setModel(symModel);

    }

    public void updateStackViewer()
    {
        if(exec.getStackModel()!=null)
            stackViewer.setModel(exec.getStackModel());
    }

    public void updateRegViewer()
    {
        Font usualFont=new Font("Dialog", Font.PLAIN, 18);
        Font changeFont=new Font("Dialog", Font.BOLD, 18);
        String text=r.getA().toUpperCase();
        if(aReg.getText().equals(text)) aReg.setFont(usualFont);
        else aReg.setFont(changeFont);
        text=r.getB().toUpperCase();
        if(bReg.getText().equals(text)) bReg.setFont(usualFont);
        else bReg.setFont(changeFont);
        text=r.getC().toUpperCase();
        if(cReg.getText().equals(text)) cReg.setFont(usualFont);
        else cReg.setFont(changeFont);
        text=r.getD().toUpperCase();
        if(dReg.getText().equals(text)) dReg.setFont(usualFont);
        else dReg.setFont(changeFont);
        text=r.getE().toUpperCase();
        if(eReg.getText().equals(text)) eReg.setFont(usualFont);
        else eReg.setFont(changeFont);
        text=r.getH().toUpperCase();
        if(hReg.getText().equals(text)) hReg.setFont(usualFont);
        else hReg.setFont(changeFont);
        text=r.getL().toUpperCase();
        if(lReg.getText().equals(text)) lReg.setFont(usualFont);
        else lReg.setFont(changeFont);
        text=r.getSP().toUpperCase();
        if(spReg.getText().equals(text)) spReg.setFont(usualFont);
        else spReg.setFont(changeFont);
        text=r.getPC().toUpperCase();
        if(pcReg.getText().equals(text)) pcReg.setFont(usualFont);
        else pcReg.setFont(changeFont);
        int n=r.getSign();
        if(Integer.parseInt(sFlag.getText())==n) sFlag.setFont(usualFont);
        else sFlag.setFont(changeFont);
        n=r.getZero();
        if(Integer.parseInt(zFlag.getText())==n) zFlag.setFont(usualFont);
        else zFlag.setFont(changeFont);
        n=r.getAuxCarry();
        if(Integer.parseInt(acFlag.getText())==n) acFlag.setFont(usualFont);
        else acFlag.setFont(changeFont);
        n=r.getParity();
        if(Integer.parseInt(pFlag.getText())==n) pFlag.setFont(usualFont);
        else pFlag.setFont(changeFont);
        n=r.getCarry();
        if(Integer.parseInt(cyFlag.getText())==n) cyFlag.setFont(usualFont);
        else cyFlag.setFont(changeFont);

        aReg.setText(r.getA().toUpperCase());
        bReg.setText(r.getB().toUpperCase());
        cReg.setText(r.getC().toUpperCase());
        dReg.setText(r.getD().toUpperCase());
        eReg.setText(r.getE().toUpperCase());
        hReg.setText(r.getH().toUpperCase());
        lReg.setText(r.getL().toUpperCase());
        spReg.setText(r.getSP().toUpperCase());
        pcReg.setText(r.getPC().toUpperCase());
        sFlag.setText(""+r.getSign());
        zFlag.setText(""+r.getZero());
        acFlag.setText(""+r.getAuxCarry());
        pFlag.setText(""+r.getParity());
        cyFlag.setText(""+r.getCarry());

        ieffFlag.setText(""+r.getIEFF());
    }

    public void updatePortViewer()
    {
        portText.setText(ports.getPort(portSpinner.getValue().toString()));
        //port0
        String binstr=ports.getPortBinary(port0.getText());
        if(binstr.charAt(7)=='0') port00.setEnabled(false);
        else port00.setEnabled(true);
        if(binstr.charAt(6)=='0') port01.setEnabled(false);
        else port01.setEnabled(true);
        if(binstr.charAt(5)=='0') port02.setEnabled(false);
        else port02.setEnabled(true);
        if(binstr.charAt(4)=='0') port03.setEnabled(false);
        else port03.setEnabled(true);
        if(binstr.charAt(3)=='0') port04.setEnabled(false);
        else port04.setEnabled(true);
        if(binstr.charAt(2)=='0') port05.setEnabled(false);
        else port05.setEnabled(true);
        if(binstr.charAt(1)=='0') port06.setEnabled(false);
        else port06.setEnabled(true);
        if(binstr.charAt(0)=='0') port07.setEnabled(false);
        else port07.setEnabled(true);

        //port1
        binstr=ports.getPortBinary(port1.getText());
        if(binstr.charAt(7)=='0') port10.setEnabled(false);
        else port10.setEnabled(true);
        if(binstr.charAt(6)=='0') port11.setEnabled(false);
        else port11.setEnabled(true);
        if(binstr.charAt(5)=='0') port12.setEnabled(false);
        else port12.setEnabled(true);
        if(binstr.charAt(4)=='0') port13.setEnabled(false);
        else port13.setEnabled(true);
        if(binstr.charAt(3)=='0') port14.setEnabled(false);
        else port14.setEnabled(true);
        if(binstr.charAt(2)=='0') port15.setEnabled(false);
        else port15.setEnabled(true);
        if(binstr.charAt(1)=='0') port16.setEnabled(false);
        else port16.setEnabled(true);
        if(binstr.charAt(0)=='0') port17.setEnabled(false);
        else port17.setEnabled(true);

        //port2
        binstr=ports.getPortBinary(port2.getText());
        if(binstr.charAt(7)=='0') port20.setEnabled(false);
        else port20.setEnabled(true);
        if(binstr.charAt(6)=='0') port21.setEnabled(false);
        else port21.setEnabled(true);
        if(binstr.charAt(5)=='0') port22.setEnabled(false);
        else port22.setEnabled(true);
        if(binstr.charAt(4)=='0') port23.setEnabled(false);
        else port23.setEnabled(true);
        if(binstr.charAt(3)=='0') port24.setEnabled(false);
        else port24.setEnabled(true);
        if(binstr.charAt(2)=='0') port25.setEnabled(false);
        else port25.setEnabled(true);
        if(binstr.charAt(1)=='0') port26.setEnabled(false);
        else port26.setEnabled(true);
        if(binstr.charAt(0)=='0') port27.setEnabled(false);
        else port27.setEnabled(true);

        //port3
        binstr=ports.getPortBinary(port3.getText());
        if(binstr.charAt(7)=='0') port30.setEnabled(false);
        else port30.setEnabled(true);
        if(binstr.charAt(6)=='0') port31.setEnabled(false);
        else port31.setEnabled(true);
        if(binstr.charAt(5)=='0') port32.setEnabled(false);
        else port32.setEnabled(true);
        if(binstr.charAt(4)=='0') port33.setEnabled(false);
        else port33.setEnabled(true);
        if(binstr.charAt(3)=='0') port34.setEnabled(false);
        else port34.setEnabled(true);
        if(binstr.charAt(2)=='0') port35.setEnabled(false);
        else port35.setEnabled(true);
        if(binstr.charAt(1)=='0') port36.setEnabled(false);
        else port36.setEnabled(true);
        if(binstr.charAt(0)=='0') port37.setEnabled(false);
        else port37.setEnabled(true);

        //port4
        binstr=ports.getPortBinary(port4.getText());
        if(binstr.charAt(7)=='0') port40.setEnabled(false);
        else port40.setEnabled(true);
        if(binstr.charAt(6)=='0') port41.setEnabled(false);
        else port41.setEnabled(true);
        if(binstr.charAt(5)=='0') port42.setEnabled(false);
        else port42.setEnabled(true);
        if(binstr.charAt(4)=='0') port43.setEnabled(false);
        else port43.setEnabled(true);
        if(binstr.charAt(3)=='0') port44.setEnabled(false);
        else port44.setEnabled(true);
        if(binstr.charAt(2)=='0') port45.setEnabled(false);
        else port45.setEnabled(true);
        if(binstr.charAt(1)=='0') port46.setEnabled(false);
        else port46.setEnabled(true);
        if(binstr.charAt(0)=='0') port47.setEnabled(false);
        else port47.setEnabled(true);

        //port5
        binstr=ports.getPortBinary(port5.getText());
        if(binstr.charAt(7)=='0') port50.setEnabled(false);
        else port50.setEnabled(true);
        if(binstr.charAt(6)=='0') port51.setEnabled(false);
        else port51.setEnabled(true);
        if(binstr.charAt(5)=='0') port52.setEnabled(false);
        else port52.setEnabled(true);
        if(binstr.charAt(4)=='0') port53.setEnabled(false);
        else port53.setEnabled(true);
        if(binstr.charAt(3)=='0') port54.setEnabled(false);
        else port54.setEnabled(true);
        if(binstr.charAt(2)=='0') port55.setEnabled(false);
        else port55.setEnabled(true);
        if(binstr.charAt(1)=='0') port56.setEnabled(false);
        else port56.setEnabled(true);
        if(binstr.charAt(0)=='0') port57.setEnabled(false);
        else port57.setEnabled(true);

    }

    public void tableChanged(TableModelEvent e)
    {
        int row=memViewer.getEditingRow();
        int tmp;
        String val=(String)memViewer.getValueAt(row, 1);
        String addr=(String)memViewer.getValueAt(row, 0);
        try
        {
            tmp=Integer.parseInt(val, 16);
            if(val.length()==2) m.write(val, Integer.parseInt(addr, 16));
            else 
            {
                memViewer.setValueAt("00", row, 1);
                outputArea.setText(outputArea.getText()+"\nEnter only 'two digit' hexadecimal numbers into the memory.");
            }
        }
        catch(java.lang.NumberFormatException q)
        {
            memViewer.setValueAt("00", row, 1);
            outputArea.setText(outputArea.getText()+"\nInvalid hexadecimal number.");
        }
    }

    public void showOpen()
    {
        if(!newFileAction()) return;
        JFileChooser chooser=new JFileChooser(".");
        chooser.setFileFilter(simFilter);
        chooser.addChoosableFileFilter(txtFilter);
        File selectedFile;
        int status=chooser.showOpenDialog(null);
        if(status==JFileChooser.APPROVE_OPTION)
        {
            selectedFile=chooser.getSelectedFile();
        }
        else selectedFile=null;
        if(selectedFile!=null)
        {
            FileReader reader=null;
            try
            {
                reader=new FileReader(selectedFile);
                pgmEditor.read(reader,selectedFile);
                compileProgramAction();

                currentFile=selectedFile;
                setEditorAsVirgin();
            }
            catch(IOException qq)
            {
                System.err.println("Error while loading text");
            }
            finally{
                if(reader!=null)
                {
                    try{
                        reader.close();
                    }catch(IOException qw)
                    {
                        System.err.println("Error closing the file reader");
                    }
                }
            }
        }
        setWindowTitle();
    }

    public void showSave()
    {
        String extension="";
        JFileChooser chooser=new JFileChooser(".");
        chooser.setFileFilter(simFilter);
        chooser.addChoosableFileFilter(txtFilter);
        File selectedFile;
        int status=chooser.showSaveDialog(null);
        if(status==JFileChooser.APPROVE_OPTION)
        {
            selectedFile=chooser.getSelectedFile();
        }
        else selectedFile=null;
        if(selectedFile!=null)
        {
            FileWriter writer=null;
            try{
                if(chooser.getFileFilter()==simFilter) extension=".sim";
                else if(chooser.getFileFilter()==txtFilter) extension=".txt";
                writer=new FileWriter(selectedFile.getAbsolutePath()+extension);//selectedFile);
                pgmEditor.write(writer);

                currentFile=selectedFile;
                setEditorAsVirgin();
            }catch(IOException qq) {
                System.err.println("Error while saving");
            }
            finally{
                if(writer!=null){
                    try{
                        writer.close();
                    }catch(IOException qw){
                        System.err.println("Error while closing the file writer");
                    }
                }
            }
        }
        setWindowTitle();
    }

    public boolean newFileAction() //boolean checks whether NEW action is CANCELled.
    {                               //Required for QUIT action. If CANCEL is pressed returns False
        setVirginityOfEditor();
        int status;
        if(virgin)
        {
            pgmEditor.setText("");
            currentFile=null;
            setEditorAsVirgin();
        }
        else
        {
            if(currentFile==null)
            {
                //ask to save the file or not
                status=JOptionPane.showConfirmDialog(this, "The editor is modified. Do you want to save the changes?", "j8085sim", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if(status==JOptionPane.YES_OPTION)
                {
                    showSave();
                    pgmEditor.setText("");
                    currentFile=null;
                    setEditorAsVirgin();
                }
                else if(status==JOptionPane.NO_OPTION)
                {
                    pgmEditor.setText("");
                    currentFile=null;
                    setEditorAsVirgin();
                }
                else if(status==JOptionPane.CANCEL_OPTION) return false;
            }
            else
            {
                status=JOptionPane.showConfirmDialog(this, "The file \""+currentFile.getName() +"\"is modified. Do you want to save the changes?", "New", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if(status==JOptionPane.YES_OPTION)
                {
                    FileWriter writer=null;
                    try{
                        writer=new FileWriter(currentFile);
                        pgmEditor.write(writer);

                        pgmEditor.setText("");
                        currentFile=null;
                        setEditorAsVirgin();
                    }catch(IOException qq) {
                        System.err.println("Error while saving");
                    }
                    finally{
                        if(writer!=null){
                            try{
                                writer.close();
                            }catch(IOException qw){
                                System.err.println("Error while closing the file writer");
                            }
                        }
                    }
                }
                else if(status==JOptionPane.NO_OPTION)
                {
                    pgmEditor.setText("");
                    currentFile=null;
                    setEditorAsVirgin();
                }
                else if(status==JOptionPane.CANCEL_OPTION) return false;
            }
        }
        setWindowTitle();
        return true;
    }

    public void saveFileAction()
    {
        if(currentFile==null) showSave();
        else
        {
            FileWriter writer=null;
            try{
                writer=new FileWriter(currentFile);
                pgmEditor.write(writer);
                setEditorAsVirgin();
            }catch(IOException qq) {
                System.err.println("Error while saving");
            }
            finally{
                if(writer!=null){
                    try{
                        writer.close();
                    }catch(IOException qw){
                        System.err.println("Error while closing the file writer");
                    }
                }
            }
        }
    }

    public void saveAsFileAction()
    {
        showSave();
    }

    public void quitEditorAction()
    {
        if(newFileAction()) System.exit(0);
    }

    public void checkUndoRedo()
    {
        if(history.canUndo())
        {
            undoButton.setEnabled(true);
            undoMenu.setEnabled(true);
        }
        else
        {
            undoButton.setEnabled(false);
            undoMenu.setEnabled(false);
        }
        if(history.canRedo())
        {
            redoButton.setEnabled(true);
            redoMenu.setEnabled(true);
        }
        else
        {
            redoButton.setEnabled(false);
            redoMenu.setEnabled(false);
        }
    }

    public void undoAction()
    {
        try{
            if(history.canUndo())
                history.undo();
            checkUndoRedo();
        }
        catch(CannotUndoException udo)
        {
            System.out.println("Cannot undo");
        }
    }

    public void redoAction()
    {
        try
        {
            if(history.canRedo())
                history.redo();
            checkUndoRedo();
        }
        catch(CannotRedoException rdo)
        {
            System.out.println("Cannot redo");
        }
    }

    public void setEditorAsVirgin()
    {
        virgin=true;
        initialdoc=pgmEditor.getText();
    }

    public void setVirginityOfEditor()
    {
        finaldoc=pgmEditor.getText();
        if(!finaldoc.equals(initialdoc)) virgin=false;
    }

    public void setWindowTitle()
    {
        String title="";
        if(currentFile==null) title="Default";
        else title=currentFile.getName();
        title=title+" - j8085sim";
        setTitle(title);
    }

    boolean validHex(String h)
    {
        try
        {
            int n=Integer.parseInt(h, 16);
            return true;
        }
        catch(NumberFormatException q)
        {
            return false;
        }
    }

    public void compileProgramAction()
    {
        sa=new SynAnalyser(m); sa.setTextpane(pgmEditor);
        sa.input(pgmEditor.getText());
        sa.analyser();
        updateMemoryViewer();
        updateSymbolViewer();

        XMLRead help=new XMLRead();
        try{
            String shorty=help.getHelp(sa.getOpcode())[1];
        //if(shorty!=null)
            shortHelpLabel.setText(sa.getOpcode().toUpperCase()+": "+shorty);
        }
        catch(NullPointerException nl){}

        execAddress.setText(sa.getStartLocation());
        if(sa.errorMsg!=null) outputArea.setText("Compilation Errors:"+sa.errorMsg);
        else outputArea.setText("Compilation complete with no errors.");
    }

    public void executeProgramAction()
    {
                execThread=new Thread(this,"execution thread");
        sa=new SynAnalyser(m); sa.setTextpane(pgmEditor);
        sa.input(pgmEditor.getText());
        sa.analyser();
        updateMemoryViewer();
        updateSymbolViewer();
        if(!execAddress.getText().equalsIgnoreCase(sa.getStartLocation()))
        {
            if(!validHex(execAddress.getText()))
            {
                errorInvalidHex();
                outputArea.setText("Invalid Execution start address.");
                return;
            }
            execAddress.setText(execAddress.getText().toUpperCase());
            //memAddress=execAddress.getText();
        }
        if(sa.errorMsg!=null) outputArea.setText("Compilation Errors:"+sa.errorMsg);
        else
        {
            outputArea.setText("Executing..");
            exec=new Executer(r, m,ports);
            //exec.setStartAddress(execAddress.getText());
            memAddress=execAddress.getText();
            execThread.start();
            executeButton.setEnabled(false);
            stopButton.setEnabled(true);
            stopMenu.setEnabled(true);
            //exec.execute();
            //while(memAddress!=null)
                //updateRegViewer();
            /*if(exec.errorMessage==null)
                outputArea.setText("Execution completed successfully");
            else
                outputArea.setText("Execution Errors:"+exec.errorMessage);*/
        }
    }

    public void debugProgramAction()
    {
                debugMode=true;
        if(!compiled)
        {
            sa=new SynAnalyser(m);
            sa.input(pgmEditor.getText()); sa.setTextpane(pgmEditor);
            sa.analyser();
            updateMemoryViewer();
            updateSymbolViewer();
            if(!execAddress.getText().equalsIgnoreCase(sa.getStartLocation()))
            {
                if(!validHex(execAddress.getText()))
                {
                    errorInvalidHex();
                    outputArea.setText("Invalid Execution start address.");
                    return;
                }
                execAddress.setText(execAddress.getText().toUpperCase());
            }

            if(sa.errorMsg!=null) outputArea.setText("Compilation Errors:"+sa.errorMsg);
            else
            {
                compiled=true;
                execThread=new Thread(this,"execution thread");
                exec=new Executer(r, m,ports);
                memAddress=execAddress.getText();
                execThread.start();
                executeButton.setEnabled(false);
                stopButton.setEnabled(true);
                stopMenu.setEnabled(true);
                outputArea.setText("");
            }
        }
        else resumeExecution();
    }

    public void stopExecutionAction()
    {
                memAddress=null;
        debugMode=false;
        compiled=false;
        suspended=false;
        stopButton.setEnabled(false);
        stopMenu.setEnabled(false);
        executeButton.setEnabled(true);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel3Clone = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane2Clone = new javax.swing.JScrollPane();
        outputArea = new javax.swing.JTextPane();
        outputAreaClone = new javax.swing.JTextPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel17 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        pgmEditor = new javax.swing.JTextPane();
        jPanel18 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        port05 = new javax.swing.JLabel();
        port06 = new javax.swing.JLabel();
        port04 = new javax.swing.JLabel();
        port07 = new javax.swing.JLabel();
        port01 = new javax.swing.JLabel();
        port02 = new javax.swing.JLabel();
        port00 = new javax.swing.JLabel();
        port03 = new javax.swing.JLabel();
        port0 = new javax.swing.JTextField();
        jPanel10 = new javax.swing.JPanel();
        portSpinner = new javax.swing.JSpinner();
        portText = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jPanel11 = new javax.swing.JPanel();
        port15 = new javax.swing.JLabel();
        port16 = new javax.swing.JLabel();
        port14 = new javax.swing.JLabel();
        port17 = new javax.swing.JLabel();
        port11 = new javax.swing.JLabel();
        port12 = new javax.swing.JLabel();
        port10 = new javax.swing.JLabel();
        port13 = new javax.swing.JLabel();
        port1 = new javax.swing.JTextField();
        jPanel12 = new javax.swing.JPanel();
        port35 = new javax.swing.JLabel();
        port36 = new javax.swing.JLabel();
        port34 = new javax.swing.JLabel();
        port37 = new javax.swing.JLabel();
        port31 = new javax.swing.JLabel();
        port32 = new javax.swing.JLabel();
        port30 = new javax.swing.JLabel();
        port33 = new javax.swing.JLabel();
        port3 = new javax.swing.JTextField();
        jPanel13 = new javax.swing.JPanel();
        port45 = new javax.swing.JLabel();
        port46 = new javax.swing.JLabel();
        port44 = new javax.swing.JLabel();
        port47 = new javax.swing.JLabel();
        port41 = new javax.swing.JLabel();
        port42 = new javax.swing.JLabel();
        port40 = new javax.swing.JLabel();
        port43 = new javax.swing.JLabel();
        port4 = new javax.swing.JTextField();
        jPanel14 = new javax.swing.JPanel();
        port55 = new javax.swing.JLabel();
        port56 = new javax.swing.JLabel();
        port54 = new javax.swing.JLabel();
        port57 = new javax.swing.JLabel();
        port51 = new javax.swing.JLabel();
        port52 = new javax.swing.JLabel();
        port50 = new javax.swing.JLabel();
        port53 = new javax.swing.JLabel();
        port5 = new javax.swing.JTextField();
        jPanel15 = new javax.swing.JPanel();
        port25 = new javax.swing.JLabel();
        port26 = new javax.swing.JLabel();
        port24 = new javax.swing.JLabel();
        port27 = new javax.swing.JLabel();
        port21 = new javax.swing.JLabel();
        port22 = new javax.swing.JLabel();
        port20 = new javax.swing.JLabel();
        port23 = new javax.swing.JLabel();
        port2 = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        stackViewer = new javax.swing.JTable();
        jPanel19 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        symbolViewer = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        ieffFlag = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        rstField = new javax.swing.JTextField();
        intrButton = new javax.swing.JButton();
        rst55Button = new javax.swing.JButton();
        rst65Button = new javax.swing.JButton();
        rst75Button = new javax.swing.JButton();
        trapButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lineNoLabel = new javax.swing.JLabel();
        shortHelpLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        aReg = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        bReg = new javax.swing.JLabel();
        cReg = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        dReg = new javax.swing.JLabel();
        eReg = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        hReg = new javax.swing.JLabel();
        lReg = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        spReg = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        pcReg = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        decfield = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        hexfield = new javax.swing.JTextField();
        dhButton = new javax.swing.JButton();
        hdButton = new javax.swing.JButton();
        jPanel21 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        sFlag = new javax.swing.JLabel();
        zFlag = new javax.swing.JLabel();
        acFlag = new javax.swing.JLabel();
        pFlag = new javax.swing.JLabel();
        cyFlag = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        startAddress = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        endAddress = new javax.swing.JTextField();
        refreshButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        memViewer = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        newButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        saveButton = new javax.swing.JButton();
        jToolBar2 = new javax.swing.JToolBar();
        tButton6 = new javax.swing.JButton();
        jLabel36 = new javax.swing.JLabel();
        execAddress = new javax.swing.JTextField();
        executeButton = new javax.swing.JButton();
        debugButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jToolBar3 = new javax.swing.JToolBar();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        cutButton = new javax.swing.JButton();
        copyButton = new javax.swing.JButton();
        pasteButton = new javax.swing.JButton();
        jToolBar4 = new javax.swing.JToolBar();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        newMenu = new javax.swing.JMenuItem();
        openMenu = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        saveMenu = new javax.swing.JMenuItem();
        saveAsMenu = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        quitMenu = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoMenu = new javax.swing.JMenuItem();
        redoMenu = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMenu2 = new javax.swing.JMenu();
        registerMenu = new javax.swing.JMenuItem();
        memoryMenu = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        cleanMenu = new javax.swing.JMenu();
        cleanReg = new javax.swing.JMenuItem();
        cleanPorts = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        compileMenu = new javax.swing.JMenuItem();
        executeMenu = new javax.swing.JMenuItem();
        debugMenu = new javax.swing.JMenuItem();
        stopMenu = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        instrMenu = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        aboutMenu = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("j8085sim");
        setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/sim/images/small/icon.png")));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Messages"));
        jPanel3Clone.setBorder(javax.swing.BorderFactory.createTitledBorder("Messages"));

        outputArea.setEditable(false);
        outputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                outputAreaClone.setText(outputArea.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                outputAreaClone.setText(outputArea.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                outputAreaClone.setText(outputArea.getText());
            }
        });
        outputAreaClone.setEditable(false);
        jScrollPane2.setViewportView(outputArea);
        jScrollPane2Clone.setViewportView(outputAreaClone);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3LayoutClone = new javax.swing.GroupLayout(jPanel3Clone);
        jPanel3Clone.setLayout(jPanel3LayoutClone);
        jPanel3LayoutClone.setHorizontalGroup(
                jPanel3LayoutClone.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane2Clone, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
        );
        jPanel3LayoutClone.setVerticalGroup(
                jPanel3LayoutClone.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3LayoutClone.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2Clone, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE))
        );

        jSplitPane2.setResizeWeight(1.0);
        jSplitPane2.setOneTouchExpandable(true);

        jPanel17.setPreferredSize(new java.awt.Dimension(300, 415));

        pgmEditor.setFont(new java.awt.Font("Dialog", 0, 18));
        pgmEditor.setPreferredSize(new java.awt.Dimension(300, 20));
        pgmEditor.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                pgmEditorCaretUpdate(evt);
            }
        });
        pgmEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                pgmEditorKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(pgmEditor);

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);

        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(jPanel17);

        jPanel7.setPreferredSize(new java.awt.Dimension(350, 383));

        jPanel9.setLayout(null);

        port05.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port05.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port05);
        port05.setBounds(110, 0, 48, 48);

        port06.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port06.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port06);
        port06.setBounds(80, 0, 48, 48);

        port04.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port04.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port04);
        port04.setBounds(140, 0, 48, 48);

        port07.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port07.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port07);
        port07.setBounds(50, 0, 48, 48);

        port01.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port01.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port01);
        port01.setBounds(230, 0, 48, 48);

        port02.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port02.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port02);
        port02.setBounds(200, 0, 48, 48);

        port00.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port00.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port00);
        port00.setBounds(260, 0, 48, 48);

        port03.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port03.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel9.add(port03);
        port03.setBounds(170, 0, 48, 48);

        port0.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port0.setText("00");
        port0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port0ActionPerformed(evt);
            }
        });
        jPanel9.add(port0);
        port0.setBounds(10, 10, 40, 18);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Modify Port's content"));

        portSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                portSpinnerStateChanged(evt);
            }
        });

        portText.setColumns(3);
        portText.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        portText.setText("00");
        portText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portTextActionPerformed(evt);
            }
        });

        jLabel2.setText("Port Address");

        jLabel4.setText("Value");

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(portSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(jLabel4)
                .addGap(4, 4, 4)
                .addComponent(portText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(portSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel4)
                .addComponent(portText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPanel11.setLayout(null);

        port15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port15.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port15);
        port15.setBounds(110, 0, 48, 48);

        port16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port16.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port16);
        port16.setBounds(80, 0, 48, 48);

        port14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port14.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port14);
        port14.setBounds(140, 0, 48, 48);

        port17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port17.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port17);
        port17.setBounds(50, 0, 48, 48);

        port11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port11.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port11);
        port11.setBounds(230, 0, 48, 48);

        port12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port12.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port12);
        port12.setBounds(200, 0, 48, 48);

        port10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port10.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port10);
        port10.setBounds(260, 0, 48, 48);

        port13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port13.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel11.add(port13);
        port13.setBounds(170, 0, 48, 48);

        port1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port1.setText("01");
        port1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port1ActionPerformed(evt);
            }
        });
        jPanel11.add(port1);
        port1.setBounds(10, 10, 40, 18);

        jPanel12.setLayout(null);

        port35.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port35.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port35);
        port35.setBounds(110, 0, 48, 48);

        port36.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port36.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port36);
        port36.setBounds(80, 0, 48, 48);

        port34.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port34.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port34);
        port34.setBounds(140, 0, 48, 48);

        port37.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port37.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port37);
        port37.setBounds(50, 0, 48, 48);

        port31.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port31.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port31);
        port31.setBounds(230, 0, 48, 48);

        port32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port32.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port32);
        port32.setBounds(200, 0, 48, 48);

        port30.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port30.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port30);
        port30.setBounds(260, 0, 48, 48);

        port33.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port33.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel12.add(port33);
        port33.setBounds(170, 0, 48, 48);

        port3.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port3.setText("03");
        port3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port3ActionPerformed(evt);
            }
        });
        jPanel12.add(port3);
        port3.setBounds(10, 10, 40, 18);

        jPanel13.setLayout(null);

        port45.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port45.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port45);
        port45.setBounds(110, 0, 48, 48);

        port46.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port46.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port46);
        port46.setBounds(80, 0, 48, 48);

        port44.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port44.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port44);
        port44.setBounds(140, 0, 48, 48);

        port47.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port47.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port47);
        port47.setBounds(50, 0, 48, 48);

        port41.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port41.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port41);
        port41.setBounds(230, 0, 48, 48);

        port42.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port42.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port42);
        port42.setBounds(200, 0, 48, 48);

        port40.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port40.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port40);
        port40.setBounds(260, 0, 48, 48);

        port43.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port43.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel13.add(port43);
        port43.setBounds(170, 0, 48, 48);

        port4.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port4.setText("04");
        port4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port4ActionPerformed(evt);
            }
        });
        jPanel13.add(port4);
        port4.setBounds(10, 10, 40, 18);

        jPanel14.setLayout(null);

        port55.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port55.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port55);
        port55.setBounds(110, 0, 48, 48);

        port56.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port56.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port56);
        port56.setBounds(80, 0, 48, 48);

        port54.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port54.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port54);
        port54.setBounds(140, 0, 48, 48);

        port57.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port57.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port57);
        port57.setBounds(50, 0, 48, 48);

        port51.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port51.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port51);
        port51.setBounds(230, 0, 48, 48);

        port52.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port52.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port52);
        port52.setBounds(200, 0, 48, 48);

        port50.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port50.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port50);
        port50.setBounds(260, 0, 48, 48);

        port53.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port53.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel14.add(port53);
        port53.setBounds(170, 0, 48, 48);

        port5.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port5.setText("05");
        port5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port5ActionPerformed(evt);
            }
        });
        jPanel14.add(port5);
        port5.setBounds(10, 10, 40, 18);

        jPanel15.setLayout(null);

        port25.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port25.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port25);
        port25.setBounds(110, 0, 48, 48);

        port26.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port26.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port26);
        port26.setBounds(80, 0, 48, 48);

        port24.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port24.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port24);
        port24.setBounds(140, 0, 48, 48);

        port27.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port27.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port27);
        port27.setBounds(50, 0, 48, 48);

        port21.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port21.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port21);
        port21.setBounds(230, 0, 48, 48);

        port22.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port22.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port22);
        port22.setBounds(200, 0, 48, 48);

        port20.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port20.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port20);
        port20.setBounds(260, 0, 48, 48);

        port23.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/active_bulb.png"))); // NOI18N
        port23.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/inactive_bulb.png"))); // NOI18N
        jPanel15.add(port23);
        port23.setBounds(170, 0, 48, 48);

        port2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        port2.setText("02");
        port2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                port2ActionPerformed(evt);
            }
        });
        jPanel15.add(port2);
        port2.setBounds(10, 10, 40, 18);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1020, 1020, 1020))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                            .addComponent(jPanel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE))
                        .addGap(1020, 1020, 1020))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, 1301, Short.MAX_VALUE)
                            .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1301, Short.MAX_VALUE))
                        .addGap(27, 27, 27))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, 1301, Short.MAX_VALUE)
                            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, 1301, Short.MAX_VALUE))
                        .addGap(27, 27, 27))))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Ports", jPanel7);

        jPanel8.setPreferredSize(new java.awt.Dimension(350, 433));

        stackViewer.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SP", "Register Pair", "Value"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        stackViewer.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(stackViewer);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Stack", jPanel8);

        symbolViewer.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Symbol", "Value"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        symbolViewer.getTableHeader().setReorderingAllowed(false);
        jScrollPane5.setViewportView(symbolViewer);

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Symbols", jPanel19);
        jTabbedPane1.addTab("Output", jPanel3Clone);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Interrupts"));

        jLabel3.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel3.setText("IEFF");
        jLabel3.setToolTipText("Interrupt Enable Flip Flop");

        ieffFlag.setFont(new java.awt.Font("Dialog", 1, 14));
        ieffFlag.setText("0");
        ieffFlag.setToolTipText("Interrupt Enable Flip Flop");

        jLabel9.setText("RST");
        jLabel9.setToolTipText("RST code provided by INTR");

        rstField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        rstField.setText("0");
        rstField.setToolTipText("RST code provided by INTR");

        intrButton.setText("INTR");
        intrButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intrButtonActionPerformed(evt);
            }
        });

        rst55Button.setText("RST 5.5");
        rst55Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rst55ButtonActionPerformed(evt);
            }
        });

        rst65Button.setText("RST 6.5");
        rst65Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rst65ButtonActionPerformed(evt);
            }
        });

        rst75Button.setText("RST 7.5");
        rst75Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rst75ButtonActionPerformed(evt);
            }
        });

        trapButton.setText("TRAP");
        trapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trapButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(rst55Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rst65Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rst75Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(trapButton))
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ieffFlag, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rstField, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(intrButton)
                .addGap(81, 81, 81))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(ieffFlag, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(rstField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intrButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rst55Button)
                    .addComponent(rst65Button)
                    .addComponent(rst75Button)
                    .addComponent(trapButton)))
        );

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );

        jSplitPane2.setRightComponent(jPanel18);

        lineNoLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        lineNoLabel.setText(":");

        shortHelpLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        //shortHelpLabel.setText(".");

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("Line: ");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lineNoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(shortHelpLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 766, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel8)
                .addComponent(lineNoLabel)
                .addComponent(shortHelpLabel))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 881, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap()
            )
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 800, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                )
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Registers"));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setText("A");

        aReg.setFont(new java.awt.Font("Dialog", 0, 18));
        aReg.setText("00");

        jLabel17.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel17.setText("B");

        bReg.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        bReg.setText("00");

        cReg.setFont(new java.awt.Font("Dialog", 0, 18));
        cReg.setText("00");

        jLabel20.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel20.setText("C");

        jLabel21.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel21.setText("D");

        dReg.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        dReg.setText("00");

        eReg.setFont(new java.awt.Font("Dialog", 0, 18));
        eReg.setText("00");

        jLabel24.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel24.setText("E");

        jLabel25.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel25.setText("H");

        hReg.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        hReg.setText("00");

        lReg.setFont(new java.awt.Font("Dialog", 0, 18));
        lReg.setText("00");

        jLabel28.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel28.setText("L");

        jLabel29.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel29.setText("SP");

        spReg.setFont(new java.awt.Font("Dialog", 0, 18));
        spReg.setText("FFFF");

        jLabel31.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel31.setText("PC");

        pcReg.setFont(new java.awt.Font("Dialog", 0, 18));
        pcReg.setText("0000");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(49, 49, 49)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(aReg, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25)
                            .addComponent(jLabel21)
                            .addComponent(jLabel17))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(hReg)
                            .addComponent(dReg, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                            .addComponent(bReg, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cReg, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                            .addComponent(eReg, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                            .addComponent(lReg))
                        .addGap(6, 6, 6)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel28)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel24)
                                .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.TRAILING)))))
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel29)
                        .addGap(18, 18, 18)
                        .addComponent(spReg, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addGap(18, 18, 18)
                        .addComponent(pcReg, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)))
                .addGap(22, 22, 22))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(aReg))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(bReg)
                        .addComponent(jLabel17))
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(39, 39, 39)
                                .addComponent(jLabel24))
                            .addComponent(jLabel20)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(78, 78, 78)
                                .addComponent(jLabel28)))
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addComponent(cReg)
                            .addGap(18, 18, 18)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(eReg)
                                .addComponent(dReg)
                                .addComponent(jLabel21))
                            .addGap(18, 18, 18)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lReg)
                                .addComponent(hReg)
                                .addComponent(jLabel25)))))
                .addGap(22, 22, 22)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(spReg))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(pcReg))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel16.setBorder(javax.swing.BorderFactory.createTitledBorder("Decimal Hex Converter"));
        jPanel16.setLayout(null);

        decfield.setColumns(5);
        decfield.setFont(new java.awt.Font("Dialog", 0, 18));
        decfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        decfield.setText("0");
        decfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decfieldActionPerformed(evt);
            }
        });
        jPanel16.add(decfield);
        decfield.setBounds(10, 40, 80, 25);

        jLabel5.setText("Decimal");
        jPanel16.add(jLabel5);
        jLabel5.setBounds(20, 20, 53, 16);

        jLabel7.setText("Hex");
        jPanel16.add(jLabel7);
        jLabel7.setBounds(170, 20, 30, 16);

        hexfield.setColumns(5);
        hexfield.setFont(new java.awt.Font("Dialog", 0, 18));
        hexfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        hexfield.setText("0");
        hexfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexfieldActionPerformed(evt);
            }
        });
        jPanel16.add(hexfield);
        hexfield.setBounds(150, 40, 80, 25);

        dhButton.setFont(new java.awt.Font("Dialog", 1, 14));
        dhButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/d_to_h.png"))); // NOI18N
        dhButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        dhButton.setContentAreaFilled(false);
        dhButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhButtonActionPerformed(evt);
            }
        });
        jPanel16.add(dhButton);
        dhButton.setBounds(110, 30, 24, 20);

        hdButton.setFont(new java.awt.Font("Dialog", 1, 14));
        hdButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/h_to_d.png"))); // NOI18N
        hdButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        hdButton.setContentAreaFilled(false);
        hdButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hdButtonActionPerformed(evt);
            }
        });
        jPanel16.add(hdButton);
        hdButton.setBounds(110, 50, 24, 20);

        jPanel21.setBorder(javax.swing.BorderFactory.createTitledBorder("Flags"));

        jLabel12.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("S");

        jLabel13.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("Z");

        jLabel14.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("AC");

        jLabel15.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("P");

        jLabel16.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("CY");

        sFlag.setFont(new java.awt.Font("Dialog", 0, 18));
        sFlag.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sFlag.setText("0");

        zFlag.setFont(new java.awt.Font("Dialog", 0, 18));
        zFlag.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        zFlag.setText("0");

        acFlag.setFont(new java.awt.Font("Dialog", 0, 18));
        acFlag.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        acFlag.setText("0");

        pFlag.setFont(new java.awt.Font("Dialog", 0, 18));
        pFlag.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pFlag.setText("0");

        cyFlag.setFont(new java.awt.Font("Dialog", 0, 18));
        cyFlag.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cyFlag.setText("0");

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(acFlag, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(pFlag, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(cyFlag, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(zFlag, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sFlag, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sFlag)
                    .addComponent(jLabel12))
                .addGap(18, 18, 18)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zFlag)
                    .addComponent(jLabel13))
                .addGap(18, 18, 18)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(acFlag)
                    .addComponent(jLabel14))
                .addGap(18, 18, 18)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pFlag)
                    .addComponent(jLabel15))
                .addGap(18, 18, 18)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cyFlag)
                    .addComponent(jLabel16))
                .addContainerGap(43, Short.MAX_VALUE))
        );

        jPanel20.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory Contents"));

        jLabel33.setText("Start");

        startAddress.setFont(new java.awt.Font("Dialog", 1, 14));
        startAddress.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        startAddress.setText("0000");

        jLabel34.setText("End");

        endAddress.setFont(new java.awt.Font("Dialog", 1, 14));
        endAddress.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        endAddress.setText("0010");
        endAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endAddressActionPerformed(evt);
            }
        });

        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        memViewer.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Memory Location", "Contents"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        memViewer.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(memViewer);
        memViewer.getColumnModel().getColumn(0).setResizable(false);
        memViewer.getColumnModel().getColumn(1).setResizable(false);

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel34)
                    .addComponent(jLabel33))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(endAddress)
                    .addComponent(startAddress, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(refreshButton)
                .addContainerGap(36, Short.MAX_VALUE))
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(startAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel33))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel34)
                            .addComponent(endAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(refreshButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel20, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        newButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/document-new.png"))); // NOI18N
        newButton.setToolTipText("New");
        newButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        newButton.setFocusable(false);
        newButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newButton.setOpaque(false);
        newButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(newButton);

        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/document-open.png"))); // NOI18N
        openButton.setToolTipText("Open");
        openButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setOpaque(false);
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(openButton);
        jToolBar1.add(jSeparator5);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/document-save.png"))); // NOI18N
        saveButton.setToolTipText("Save");
        saveButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setOpaque(false);
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButton);

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        tButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/run-build.png"))); // NOI18N
        tButton6.setToolTipText("Compile");
        tButton6.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tButton6.setFocusable(false);
        tButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tButton6.setOpaque(false);
        tButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tButton6ActionPerformed(evt);
            }
        });
        jToolBar2.add(tButton6);

        jLabel36.setFont(new java.awt.Font("Dialog", 0, 10));
        jLabel36.setText("Execution Address:");
        jToolBar2.add(jLabel36);

        execAddress.setFont(new java.awt.Font("Dialog", 0, 14));
        execAddress.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        execAddress.setText("0000");
        execAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                execAddressActionPerformed(evt);
            }
        });
        jToolBar2.add(execAddress);

        executeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/arrow-right.png"))); // NOI18N
        executeButton.setToolTipText("Execute");
        executeButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        executeButton.setFocusable(false);
        executeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        executeButton.setOpaque(false);
        executeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        executeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(executeButton);

        debugButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/go-next-context.png"))); // NOI18N
        debugButton.setToolTipText("Single line execution");
        debugButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        debugButton.setFocusable(false);
        debugButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        debugButton.setOpaque(false);
        debugButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        debugButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(debugButton);

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/process-stop.png"))); // NOI18N
        stopButton.setToolTipText("Stop execution");
        stopButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        stopButton.setEnabled(false);
        stopButton.setFocusable(false);
        stopButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stopButton.setOpaque(false);
        stopButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(stopButton);

        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);

        undoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/edit-undo.png"))); // NOI18N
        undoButton.setToolTipText("Undo");
        undoButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        undoButton.setEnabled(false);
        undoButton.setFocusable(false);
        undoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        undoButton.setOpaque(false);
        undoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        undoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(undoButton);

        redoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/edit-redo.png"))); // NOI18N
        redoButton.setToolTipText("Redo");
        redoButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        redoButton.setEnabled(false);
        redoButton.setFocusable(false);
        redoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        redoButton.setOpaque(false);
        redoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        redoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(redoButton);
        jToolBar3.add(jSeparator6);
        jToolBar3.add(jSeparator7);

        cutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/edit-cut.png"))); // NOI18N
        cutButton.setToolTipText("");
        cutButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        cutButton.setFocusable(false);
        cutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cutButton.setOpaque(false);
        cutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(cutButton);

        copyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/edit-copy.png"))); // NOI18N
        copyButton.setToolTipText("");
        copyButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        copyButton.setFocusable(false);
        copyButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyButton.setOpaque(false);
        copyButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(copyButton);

        pasteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/edit-paste.png"))); // NOI18N
        pasteButton.setToolTipText("");
        pasteButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pasteButton.setFocusable(false);
        pasteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pasteButton.setOpaque(false);
        pasteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(pasteButton);

        jToolBar4.setFloatable(false);
        jToolBar4.setRollover(true);

        jMenu1.setMnemonic('F');
        jMenu1.setText("File");

        newMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/document-new.png"))); // NOI18N
        newMenu.setMnemonic('N');
        newMenu.setText("New");
        newMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuActionPerformed(evt);
            }
        });
        jMenu1.add(newMenu);

        openMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/document-open.png"))); // NOI18N
        openMenu.setMnemonic('O');
        openMenu.setText("Open");
        openMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuActionPerformed(evt);
            }
        });
        jMenu1.add(openMenu);
        jMenu1.add(jSeparator1);

        saveMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/document-save.png"))); // NOI18N
        saveMenu.setMnemonic('S');
        saveMenu.setText("Save");
        saveMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuActionPerformed(evt);
            }
        });
        jMenu1.add(saveMenu);

        saveAsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/document-save-as.png"))); // NOI18N
        saveAsMenu.setMnemonic('A');
        saveAsMenu.setText("Save As");
        saveAsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuActionPerformed(evt);
            }
        });
        jMenu1.add(saveAsMenu);
        jMenu1.add(jSeparator2);

        quitMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/application-exit.png"))); // NOI18N
        quitMenu.setText("Quit");
        quitMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuActionPerformed(evt);
            }
        });
        jMenu1.add(quitMenu);

        jMenuBar1.add(jMenu1);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        undoMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/edit-undo.png"))); // NOI18N
        undoMenu.setText("Undo");
        undoMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoMenuActionPerformed(evt);
            }
        });
        editMenu.add(undoMenu);

        redoMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        redoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/edit-redo.png"))); // NOI18N
        redoMenu.setText("Redo");
        redoMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoMenuActionPerformed(evt);
            }
        });
        editMenu.add(redoMenu);
        editMenu.add(jSeparator3);

        jMenuBar1.add(editMenu);

        jMenu2.setText("Manage");

        registerMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/code-block.png"))); // NOI18N
        registerMenu.setText("Registers");
        registerMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                registerMenuActionPerformed(evt);
            }
        });
        jMenu2.add(registerMenu);

        memoryMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/memory.png"))); // NOI18N
        memoryMenu.setText("Memory");
        memoryMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memoryMenuActionPerformed(evt);
            }
        });
        jMenu2.add(memoryMenu);

        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/clock.png"))); // NOI18N
        jMenuItem2.setText("Clock Frequency");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Run");

        compileMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        compileMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/run-build.png"))); // NOI18N
        compileMenu.setText("Compile");
        compileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileMenuActionPerformed(evt);
            }
        });
        jMenu3.add(compileMenu);

        executeMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        executeMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/arrow-right.png"))); // NOI18N
        executeMenu.setText("Execute");
        executeMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeMenuActionPerformed(evt);
            }
        });
        jMenu3.add(executeMenu);

        debugMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        debugMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/go-next-context.png"))); // NOI18N
        debugMenu.setText("Single Line Execution");
        debugMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugMenuActionPerformed(evt);
            }
        });
        jMenu3.add(debugMenu);

        stopMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, java.awt.event.InputEvent.CTRL_MASK));
        stopMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/process-stop.png"))); // NOI18N
        stopMenu.setText("Stop Execution");
        stopMenu.setEnabled(false);
        stopMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopMenuActionPerformed(evt);
            }
        });
        jMenu3.add(stopMenu);

        jMenuBar1.add(jMenu3);

        cleanMenu.setMnemonic('C');
        cleanMenu.setText("Clean");

        cleanReg.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        cleanReg.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/code-block.png"))); // NOI18N
        cleanReg.setMnemonic('R');
        cleanReg.setText("Registers");
        cleanReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                r.setA("00");
                r.setB("00");
                r.setC("00");
                r.setD("00");
                r.setE("00");
                r.setH("00");
                r.setL("00");
                r.setCarry(0);
                r.setSign(0);
                r.setZero(0);
                r.setAuxCarry(0);
                r.setParity(0);
                r.setPC("0000");
                r.setSP("FFFF");
                updateRegViewer();
            }
        });
        cleanMenu.add(cleanReg);

        cleanPorts.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        cleanPorts.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/memory.png"))); // NOI18N
        cleanPorts.setMnemonic('P');
        cleanPorts.setText("Ports");
        cleanPorts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                for (int x = 0; x < 6; x++) {
                    ports.setPort("00", "0" + String.valueOf(x));
                    updatePortViewer();
                }
            }
        });
        cleanMenu.add(cleanPorts);

        jMenuBar1.add(cleanMenu);

        jMenu4.setText("Help");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/help-browser.png"))); // NOI18N
        jMenuItem1.setText("j8085sim Help");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem1);

        instrMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/help-contents.png"))); // NOI18N
        instrMenu.setText("8085 Instructions");
        instrMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrMenuActionPerformed(evt);
            }
        });
        jMenu4.add(instrMenu);
        jMenu4.add(jSeparator8);

        aboutMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sim/images/small/help-about.png"))); // NOI18N
        aboutMenu.setText("About");
        aboutMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuActionPerformed(evt);
            }
        });
        jMenu4.add(aboutMenu);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar4, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jToolBar4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToolBar3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        if(validHex(startAddress.getText())&&validHex(endAddress.getText()))
        {
            if(startAddress.getText().length()>4||endAddress.getText().length()>4)
            {
                errorLargeNumber(4);
                return;
            }
            int start=Integer.parseInt(startAddress.getText(), 16);
            int end=Integer.parseInt(endAddress.getText(), 16);
            if(start>end)
            {
                errorMemoryBounds();
            }
            else  updateMemoryViewer();
        }
        else
        {
            errorInvalidHex();
        }
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void portTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portTextActionPerformed
        if(validHex(portText.getText()))
        {
            if(portText.getText().length()>2)
            {
                errorLargeNumber(2);
                return;
            }
            String pos=(String)portSpinner.getValue();
            ports.setPort(portText.getText(), pos);
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            portText.setText("00");
        }

    }//GEN-LAST:event_portTextActionPerformed

    private void portSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portSpinnerStateChanged
        portText.setText(ports.getPort(portSpinner.getValue().toString()));
    }//GEN-LAST:event_portSpinnerStateChanged

    private void port0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port0ActionPerformed
        if(validHex(port0.getText()))
        {
            if(port0.getText().length()>2)
            {
                errorLargeNumber(2);
                port0.setText("00");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port0.setText("00");
        }
    }//GEN-LAST:event_port0ActionPerformed

    private void port1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port1ActionPerformed
        if(validHex(port1.getText()))
        {
            if(port1.getText().length()>2)
            {
                errorLargeNumber(2);
                port1.setText("01");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port1.setText("01");
        }
    }//GEN-LAST:event_port1ActionPerformed

    private void port2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port2ActionPerformed
        if(validHex(port2.getText()))
        {
            if(port2.getText().length()>2)
            {
                errorLargeNumber(2);
                port2.setText("02");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port2.setText("02");
        }
    }//GEN-LAST:event_port2ActionPerformed

    private void port3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port3ActionPerformed
        if(validHex(port3.getText()))
        {
            if(port3.getText().length()>2)
            {
                errorLargeNumber(2);
                port3.setText("03");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port3.setText("03");
        }
    }//GEN-LAST:event_port3ActionPerformed

    private void port4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port4ActionPerformed
        if(validHex(port4.getText()))
        {
            if(port4.getText().length()>2)
            {
                errorLargeNumber(2);
                port4.setText("04");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port4.setText("04");
        }
    }//GEN-LAST:event_port4ActionPerformed

    private void port5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_port5ActionPerformed
        if(validHex(port5.getText()))
        {
            if(port5.getText().length()>2)
            {
                errorLargeNumber(2);
                port5.setText("05");
            }
            updatePortViewer();
        }
        else
        {
            errorInvalidHex();
            port5.setText("05");
        }
    }//GEN-LAST:event_port5ActionPerformed

    private void endAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endAddressActionPerformed
        if(validHex(startAddress.getText())&&validHex(endAddress.getText()))
        {
            if(startAddress.getText().length()>4||endAddress.getText().length()>4)
            {
                errorLargeNumber(4);
                return;
            }
            int start=Integer.parseInt(startAddress.getText(), 16);
            int end=Integer.parseInt(endAddress.getText(), 16);
            if(start>end)
            {
                errorMemoryBounds();
            }
            else  updateMemoryViewer();
        }
        else
        {
            errorInvalidHex();
        }
    }//GEN-LAST:event_endAddressActionPerformed

    private void dhButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhButtonActionPerformed
        int d;
        try
        {
            d=Integer.parseInt(decfield.getText());
            hexfield.setText(Integer.toHexString(d).toUpperCase());
        }
        catch(NumberFormatException q)
        {
            errorInvalidDecimal();
        }
    }//GEN-LAST:event_dhButtonActionPerformed

    private void decfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decfieldActionPerformed
        int d;
        try
        {
            d=Integer.parseInt(decfield.getText());
            hexfield.setText(Integer.toHexString(d).toUpperCase());
        }
        catch(NumberFormatException q)
        {
            errorInvalidDecimal();
        }
    }//GEN-LAST:event_decfieldActionPerformed

    private void hdButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hdButtonActionPerformed
        int d;
        try
        {
            d=Integer.parseInt(hexfield.getText(),16);
            decfield.setText(""+d);
            hexfield.setText(hexfield.getText().toUpperCase());
        }
        catch(NumberFormatException q)
        {
            errorInvalidHex();
        }
    }//GEN-LAST:event_hdButtonActionPerformed

    private void hexfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexfieldActionPerformed
        int d;
        try
        {
            d=Integer.parseInt(hexfield.getText(),16);
            decfield.setText(""+d);
            hexfield.setText(hexfield.getText().toUpperCase());
        }
        catch(NumberFormatException q)
        {
            errorInvalidHex();
        }
    }//GEN-LAST:event_hexfieldActionPerformed

    private void executeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeButtonActionPerformed
        executeProgramAction();
    }//GEN-LAST:event_executeButtonActionPerformed

    private void tButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tButton6ActionPerformed
                sa=new SynAnalyser(m); sa.setTextpane(pgmEditor);
        sa.input(pgmEditor.getText());
        sa.analyser();
        updateMemoryViewer();
        execAddress.setText(sa.getStartLocation());
        if(sa.errorMsg!=null) outputArea.setText("Compilation Errors:"+sa.errorMsg);
        else outputArea.setText("Compilation complete with no errors.");
    }//GEN-LAST:event_tButton6ActionPerformed

    private void execAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_execAddressActionPerformed
        if(!validHex(execAddress.getText())) errorInvalidHex();
    }//GEN-LAST:event_execAddressActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        stopExecutionAction();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void debugButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugButtonActionPerformed
        debugProgramAction();
}//GEN-LAST:event_debugButtonActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        showOpen();
}//GEN-LAST:event_openButtonActionPerformed

    private void openMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuActionPerformed
        showOpen();
}//GEN-LAST:event_openMenuActionPerformed

    private void quitMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuActionPerformed
        quitEditorAction();
}//GEN-LAST:event_quitMenuActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        saveFileAction();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        newFileAction();
    }//GEN-LAST:event_newButtonActionPerformed

    private void newMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuActionPerformed
        newFileAction();
    }//GEN-LAST:event_newMenuActionPerformed

    private void saveMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuActionPerformed
        saveFileAction();
    }//GEN-LAST:event_saveMenuActionPerformed

    private void saveAsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuActionPerformed
        saveAsFileAction();
    }//GEN-LAST:event_saveAsMenuActionPerformed

    private void pgmEditorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pgmEditorKeyReleased
        compileProgramAction();
    }//GEN-LAST:event_pgmEditorKeyReleased

    private void stopMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopMenuActionPerformed
        stopExecutionAction();
    }//GEN-LAST:event_stopMenuActionPerformed

    private void debugMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugMenuActionPerformed
        debugProgramAction();
    }//GEN-LAST:event_debugMenuActionPerformed

    private void executeMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeMenuActionPerformed
        executeProgramAction();
    }//GEN-LAST:event_executeMenuActionPerformed

    private void compileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compileMenuActionPerformed
        compileProgramAction();
    }//GEN-LAST:event_compileMenuActionPerformed

    private void registerMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_registerMenuActionPerformed
        RegFrame rframe=new RegFrame(r,this);
        rframe.setLocation((int)(this.getWidth()-rframe.getWidth())/2, (int)(this.getHeight()-rframe.getHeight())/2);
        setEnabled(false);
    }//GEN-LAST:event_registerMenuActionPerformed

    private void memoryMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_memoryMenuActionPerformed
        MemFrame mframe=new MemFrame(m,this);
        mframe.setLocation((int)(this.getWidth()-mframe.getWidth())/2, (int)(this.getHeight()-mframe.getHeight())/2);
        setEnabled(false);
    }//GEN-LAST:event_memoryMenuActionPerformed

    private void pgmEditorCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_pgmEditorCaretUpdate
        int dot=evt.getDot();
        int l=1;
        for(int i=0;i<dot;i++) if(pgmEditor.getText().charAt(i)=='\n') l++;
        lineNoLabel.setText(""+l);
    }//GEN-LAST:event_pgmEditorCaretUpdate

    private void instrMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_instrMenuActionPerformed
        new HelpFrame().setLocationRelativeTo(null);
}//GEN-LAST:event_instrMenuActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        String str=JOptionPane.showInputDialog(this,"Enter clock frequency (in Hertz)", ""+frequency);
        if(str!=null)
        {
            try{
                int fr=Integer.parseInt(str);
                if(fr>0)
                    setFrequency(fr);
                else JOptionPane.showMessageDialog(this, "Enter a frequency greater than 0", "Invalid frequency", JOptionPane.ERROR_MESSAGE);
            }
            catch(NumberFormatException n)
            {
                JOptionPane.showMessageDialog(this, "Enter a valid decimal integer", "Invalid number", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void undoMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoMenuActionPerformed
        undoAction();
}//GEN-LAST:event_undoMenuActionPerformed

    private void redoMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoMenuActionPerformed
        redoAction();
}//GEN-LAST:event_redoMenuActionPerformed

    private void undoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoButtonActionPerformed
        undoAction();
}//GEN-LAST:event_undoButtonActionPerformed

    private void redoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoButtonActionPerformed
        redoAction();
    }//GEN-LAST:event_redoButtonActionPerformed

    private void aboutMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuActionPerformed
        new aboutFrame().setLocationRelativeTo(null);
    }//GEN-LAST:event_aboutMenuActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        quitEditorAction();
    }//GEN-LAST:event_formWindowClosing

    private void intrButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intrButtonActionPerformed
        int n;
        try{
            n=Integer.parseInt(rstField.getText());
            if(n<0||n>7) {
                rstField.setText("0");
                n=0;
            }
        }
        catch(NumberFormatException e)
        {
            rstField.setText("0");
            n=0;
        }
        rstNo=n;
        intr=true;
    }//GEN-LAST:event_intrButtonActionPerformed

    private void rst55ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rst55ButtonActionPerformed
        rst55=true;
        r.setI5(1);
    }//GEN-LAST:event_rst55ButtonActionPerformed

    private void rst65ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rst65ButtonActionPerformed
        rst65=true;
        r.setI6(1);
    }//GEN-LAST:event_rst65ButtonActionPerformed

    private void rst75ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rst75ButtonActionPerformed
        rst75=true;
        r.setI7(1);
    }//GEN-LAST:event_rst75ButtonActionPerformed

    private void trapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trapButtonActionPerformed
        trap=true;
    }//GEN-LAST:event_trapButtonActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        new HelpSimFrame().setLocationRelativeTo(null);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    void errorInvalidDecimal()
    {
        JOptionPane.showMessageDialog(this, "Invalid decimal number. Please try again.", "Invalid decimal number", JOptionPane.ERROR_MESSAGE);
    }
    void errorInvalidHex()
    {
        JOptionPane.showMessageDialog(this, "Invalid Hexadecimal number. Please try again.", "Invalid Hexadecimal",JOptionPane.ERROR_MESSAGE);
    }

    void errorMemoryBounds()
    {
        JOptionPane.showMessageDialog(this,"Invalid memory bounds. Check 'Start' and 'End' addresses.","Invalid memory bounds",JOptionPane.ERROR_MESSAGE);
    }

    void errorLargeNumber(int l)
    {
        JOptionPane.showMessageDialog(this,"Invalid number. Please enter a number of length "+l,"Invalid number",JOptionPane.ERROR_MESSAGE);
    }

    void showSplash(int delay) {
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash == null) {
            System.out.println("SplashScreen.getSplashScreen() returned null");
            return;
        }
        Graphics2D g = splash.createGraphics();
        if (g == null) {
            System.out.println("g is null");
            return;
        }

            splash.update();
            try {
                Thread.sleep(delay);
            }
            catch(InterruptedException e) {
            }
        splash.close();
    }
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                try{
                    UIManager.setLookAndFeel(new MetalLookAndFeel());
                }
                catch(Exception e)
                {
                    System.out.println("Can't do it");
                }

                new MainFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aReg;
    private javax.swing.JMenuItem aboutMenu;
    private javax.swing.JLabel acFlag;
    private javax.swing.JLabel bReg;
    private javax.swing.JLabel cReg;
    private javax.swing.JMenuItem compileMenu;
    private javax.swing.JButton copyButton;
    private javax.swing.JButton cutButton;
    private javax.swing.JLabel cyFlag;
    private javax.swing.JLabel dReg;
    private javax.swing.JButton debugButton;
    private javax.swing.JMenuItem debugMenu;
    private javax.swing.JTextField decfield;
    private javax.swing.JButton dhButton;
    private javax.swing.JLabel eReg;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTextField endAddress;
    private javax.swing.JTextField execAddress;
    private javax.swing.JButton executeButton;
    private javax.swing.JMenuItem executeMenu;
    private javax.swing.JLabel hReg;
    private javax.swing.JButton hdButton;
    private javax.swing.JTextField hexfield;
    private javax.swing.JLabel ieffFlag;
    private javax.swing.JMenuItem instrMenu;
    private javax.swing.JButton intrButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel3Clone;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane2Clone;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JLabel lReg;
    private javax.swing.JLabel lineNoLabel;
    private javax.swing.JTable memViewer;
    private javax.swing.JMenuItem memoryMenu;
    private javax.swing.JButton newButton;
    private javax.swing.JMenuItem newMenu;
    private javax.swing.JButton openButton;
    private javax.swing.JMenuItem openMenu;
    private javax.swing.JTextPane outputArea;
    private javax.swing.JTextPane outputAreaClone;
    private javax.swing.JLabel pFlag;
    private javax.swing.JButton pasteButton;
    private javax.swing.JLabel pcReg;
    private javax.swing.JTextPane pgmEditor;
    private javax.swing.JTextField port0;
    private javax.swing.JLabel port00;
    private javax.swing.JLabel port01;
    private javax.swing.JLabel port02;
    private javax.swing.JLabel port03;
    private javax.swing.JLabel port04;
    private javax.swing.JLabel port05;
    private javax.swing.JLabel port06;
    private javax.swing.JLabel port07;
    private javax.swing.JTextField port1;
    private javax.swing.JLabel port10;
    private javax.swing.JLabel port11;
    private javax.swing.JLabel port12;
    private javax.swing.JLabel port13;
    private javax.swing.JLabel port14;
    private javax.swing.JLabel port15;
    private javax.swing.JLabel port16;
    private javax.swing.JLabel port17;
    private javax.swing.JTextField port2;
    private javax.swing.JLabel port20;
    private javax.swing.JLabel port21;
    private javax.swing.JLabel port22;
    private javax.swing.JLabel port23;
    private javax.swing.JLabel port24;
    private javax.swing.JLabel port25;
    private javax.swing.JLabel port26;
    private javax.swing.JLabel port27;
    private javax.swing.JTextField port3;
    private javax.swing.JLabel port30;
    private javax.swing.JLabel port31;
    private javax.swing.JLabel port32;
    private javax.swing.JLabel port33;
    private javax.swing.JLabel port34;
    private javax.swing.JLabel port35;
    private javax.swing.JLabel port36;
    private javax.swing.JLabel port37;
    private javax.swing.JTextField port4;
    private javax.swing.JLabel port40;
    private javax.swing.JLabel port41;
    private javax.swing.JLabel port42;
    private javax.swing.JLabel port43;
    private javax.swing.JLabel port44;
    private javax.swing.JLabel port45;
    private javax.swing.JLabel port46;
    private javax.swing.JLabel port47;
    private javax.swing.JTextField port5;
    private javax.swing.JLabel port50;
    private javax.swing.JLabel port51;
    private javax.swing.JLabel port52;
    private javax.swing.JLabel port53;
    private javax.swing.JLabel port54;
    private javax.swing.JLabel port55;
    private javax.swing.JLabel port56;
    private javax.swing.JLabel port57;
    private javax.swing.JSpinner portSpinner;
    private javax.swing.JTextField portText;
    private javax.swing.JMenuItem quitMenu;
    private javax.swing.JButton redoButton;
    private javax.swing.JMenuItem redoMenu;
    private javax.swing.JButton refreshButton;
    private javax.swing.JMenuItem registerMenu;
    private javax.swing.JButton rst55Button;
    private javax.swing.JButton rst65Button;
    private javax.swing.JButton rst75Button;
    private javax.swing.JTextField rstField;
    private javax.swing.JLabel sFlag;
    private javax.swing.JMenuItem saveAsMenu;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem saveMenu;
    private javax.swing.JLabel shortHelpLabel;
    private javax.swing.JLabel spReg;
    private javax.swing.JTable stackViewer;
    private javax.swing.JTextField startAddress;
    private javax.swing.JButton stopButton;
    private javax.swing.JMenuItem stopMenu;
    private javax.swing.JTable symbolViewer;
    private javax.swing.JButton tButton6;
    private javax.swing.JButton trapButton;
    private javax.swing.JButton undoButton;
    private javax.swing.JMenuItem undoMenu;
    private javax.swing.JLabel zFlag;
    private javax.swing.JMenu cleanMenu;
    private javax.swing.JMenuItem cleanReg;
    private javax.swing.JMenuItem cleanPorts;
    // End of variables declaration//GEN-END:variables

}
