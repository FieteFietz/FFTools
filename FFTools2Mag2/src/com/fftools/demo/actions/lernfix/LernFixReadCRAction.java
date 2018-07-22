package com.fftools.demo.actions.lernfix;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import magellan.client.swing.InternationalizedDialog;
import magellan.library.Order;
import magellan.library.Unit;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.demo.actions.MenuAction;
import com.fftools.pools.ausbildung.LernplanHandler;
import com.fftools.swing.LernfixLernplanSubMenu;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

/**
 * Setzt das Rekrutieren um
 * @author Fiete
 *
 */
public class LernFixReadCRAction extends MenuAction{
	private static final long serialVersionUID = 1L;
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	private static final OutTextClass outText = OutTextClass.getInstance();
	private SelectionObserver selectionObserver = null;
	private LernfixLernplanSubMenu parentMenu = null;
	
	public LernFixReadCRAction(SelectionObserver selectionObserver, LernfixLernplanSubMenu parentMenu) {
		super(selectionObserver.getClient());
		setName("Lernfix CR lesen");
		this.selectionObserver = selectionObserver;
		this.parentMenu = parentMenu;
	}
	

	private void makeIt(Unit u2){
		// CR los analysiert werden
		// System.out.println("start makeIt CR Read");
		// danach parent neu initialisieren...
		aFrame frame = new aFrame(this.selectionObserver.getClient(),false);
		frame.setVisible(true);
		Thread t = new Thread(frame);
		t.start();
		
		
		
		
	}
	
	
	// inner class
	private class aFrame extends InternationalizedDialog implements Runnable{
		private static final long serialVersionUID = 1L;
		private JTextArea txtOutput = null;
		private JScrollPane sp = null;
		
		public aFrame(Frame owner, boolean modal){
			super(owner, modal);
			init();
		}
		
		private void init() {
			// FFTools2 path to jar
			setContentPane(getMainPanel());
			setTitle("Reading CR for Lernfix");
			setSize(500,700);
			outText.addOutLine("init done");
		}

		private Container getMainPanel() {
			JPanel mainPanel = new JPanel(new BorderLayout());
			txtOutput = new JTextArea("start");
			outText.setTxtOut(txtOutput);
			sp = new JScrollPane(txtOutput,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setPreferredSize(new Dimension(500,400));
			txtOutput.setEditable(false);
			mainPanel.add(sp,BorderLayout.CENTER);
			return mainPanel;
		}
		
		
		public void run(){
			outText.addOutLine("remember - no temp units are inspected");
			// wir müssen durch alle units....oha
			ScriptMain scriptMain = null;
			LernplanHandler LPH = null;
			for (Unit u:selectionObserver.getClient().getData().getUnits()){
				outText.addPoint();
				boolean treffer=false;
				for (Order o:u.getOrders2()){
					String order = o.getText();
					if (order.toLowerCase().startsWith("// script setlernplan")){
						treffer=true;
						break;
					}
				}
				if (treffer){
					// jetzt brauchen wir spätestens scriptMain
					if (scriptMain==null){
						scriptMain = new ScriptMain(selectionObserver.getClient(),outText.getTxtOut());
						reportSettings.setScriptMain(scriptMain);
					}
					ScriptUnit su = new ScriptUnit(u,scriptMain);
					outText.addOutLine("inspecting " + su.unitDesc());
					if (LPH==null){
						LPH = scriptMain.getOverlord().getLernplanHandler();
						LPH.reset();
						outText.addOutLine("LernplanHandler requested");
					}
					LPH.parseOrders(su);
					// outText.addNewLine();
				}
			}
			outText.addOutLine("finished CR Read");
			if (LPH!=null){
				outText.addOutLine("LPH has now: " + LPH.getSortedLernPläne().size() + " Lernpläne.");
				// obiges Menu neu machen
				parentMenu.init();
				outText.addOutLine("Menu recreated");
				LPH.reset();
			}
		}
	}
	
	
	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ArrayList<Object> units = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (units!=null && units.size()>0){
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.makeIt(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Lernfix möglich.","Fehler",true);
		}
	}
	
}
