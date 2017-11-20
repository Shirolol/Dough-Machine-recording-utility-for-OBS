package doughMachine.main;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

/**
 * Dough Machine Recording Tool v1.0
 * @author Serpentyan
 *
 */
public class Main extends Frame implements KeyListener, WindowListener, Runnable, ActionListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8650212624840027611L;
	private Label durationMinorL, durationMajorL, statusL, pathL;
	private int startRec, stopRec, durMin, durMinTimer, durMaj, durMajTimer, pathTimer, status;
	private TextField startRecF, stopRecF, path, durationMinor, durationMajor;
	private Button ctrl;
	private File dir;
	private byte hkSet;
	private Robot trigger;
	private static final int STATUS_WAITING=0, STATUS_RUNNING=1, STATUS_SUSPENDED=2,STATUS_ERROR=3, STATUS_CHECKING=4, STATUS_TERMINATED=5;
	private static final String[] STATUS_STR={"Waiting for parameters", "Running", "Suspended", "Error", "Checking...", "AAARGH!"};
	
	public static void main(String[] args) {
		
		new Main();
	}
	
	public Main(){
		
		setTitle("Dough Machine Recording Tool by Serpy");
		setSize(400,330);
		setResizable(false);
		setLayout(null);
		addWindowListener(this);
		startRecF=new TextField("Start recording hotkey");
		startRecF.addKeyListener(this);
		startRecF.setEditable(false);
		stopRecF=new TextField("Stop recording hotkey");
		stopRecF.addKeyListener(this);
		stopRecF.setEditable(false);
		durationMinorL=new Label("Individual recording length(minutes):");
		durationMajorL=new Label("Total recording length(minutes)");
		status=STATUS_WAITING;
		statusL=new Label(STATUS_STR[status]);
		setVisible(true);
		pathL=new Label("Recording file path:");
		path=new TextField();
		path.addKeyListener(this);
		durationMinor=new TextField();
		durationMinor.addKeyListener(this);
		durationMajor=new TextField();
		durationMajor.addKeyListener(this);
		ctrl=new Button("Start");
		ctrl.addActionListener(this);
		ctrl.setEnabled(false);
		Component[] components={startRecF, stopRecF, durationMinorL, durationMinor, durationMajorL, durationMajor, pathL, path, statusL,ctrl};
		int xmin=20, ymin=50, width=360, height=20, gap=5;
		for(int i=0;i<components.length;i++){
			components[i].setBounds(xmin, ymin+i*(height+gap), width, height);
			add(components[i]);
		}
		new Thread(this).start();
		try {
			trigger=new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void keyPressed(KeyEvent kEvt) {
		final int fieldTimer=20;
		System.out.println(kEvt.getKeyCode());
		if(kEvt.getSource()==startRecF){
			hkSet|=0b1;
			if(kEvt.getKeyCode()==0){
				startRecF.setBackground(Color.RED);
				changeStatus(STATUS_ERROR);
				hkSet|=0b100;
			}else{
				startRecF.setBackground(Color.WHITE);
				hkSet&=0b1011;
				changeStatus(STATUS_CHECKING);
			}
			startRec=kEvt.getKeyCode();
			startRecF.setText("Start recording: "+KeyEvent.getKeyText(startRec));
		}else if(kEvt.getSource()==stopRecF){
			hkSet|=0b10;
			if(kEvt.getKeyCode()==0){
				hkSet|=0b1000;
				stopRecF.setBackground(Color.RED);
				changeStatus(STATUS_ERROR);
			}else{
				stopRecF.setBackground(Color.WHITE);
				hkSet&=0b111;
				changeStatus(STATUS_CHECKING);
			}
			stopRec=kEvt.getKeyCode();
			stopRecF.setText("Stop recording: "+KeyEvent.getKeyText(stopRec));
		}else if(kEvt.getSource()==durationMinor){
			durMinTimer=fieldTimer;
			changeStatus(STATUS_CHECKING);
		}else if(kEvt.getSource()==durationMajor){
			durMajTimer=fieldTimer;
			changeStatus(STATUS_CHECKING);
		}else if(kEvt.getSource()==path){
			pathTimer=fieldTimer;
			changeStatus(STATUS_CHECKING);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}
	@Override
	public void keyTyped(KeyEvent e) {}
	@Override
	public void windowActivated(WindowEvent arg0) {}
	@Override
	public void windowClosed(WindowEvent arg0) {}
	@Override
	public void windowClosing(WindowEvent arg0) {
		changeStatus(STATUS_TERMINATED);
		dispose();
	}
	@Override
	public void windowDeactivated(WindowEvent arg0) {}
	@Override
	public void windowDeiconified(WindowEvent arg0) {}
	@Override
	public void windowIconified(WindowEvent arg0) {}
	@Override
	public void windowOpened(WindowEvent arg0) {}

	@Override
	public void run() {
		boolean errMin=false, errMaj=false, errPath=false,
				minSet=false, majSet=false, pathSet=false, recording=false;
		long next = 0;
		int restartTimer=0, holdStartTimer=0, holdStopTimer=0;
		while(true){
			if(holdStopTimer!=0)if(--holdStopTimer==0)trigger.keyRelease(stopRec);
			if(holdStartTimer!=0)if(--holdStartTimer==0)trigger.keyRelease(startRec);
			if(recording&status!=STATUS_RUNNING){
				System.out.println("Stopping");
				trigger.keyPress(stopRec);
				holdStopTimer=10;
				recording=false;
			}
			if(status==STATUS_WAITING|status==STATUS_SUSPENDED);//skip;
			else if(status==STATUS_CHECKING||status==STATUS_ERROR){
				if(durMinTimer!=0||durMajTimer!=0||pathTimer!=0){
					if(durMinTimer!=0){
						if(--durMinTimer==0){
							try{
								errMin=false;
								if(durationMinor.getText().isEmpty()){
									minSet=false;
									durationMinor.setBackground(Color.YELLOW);
								}else{
									durMin=Integer.parseInt(durationMinor.getText());
									if(durMin<1)throw null;//trigger catch
									durationMinor.setBackground(Color.WHITE);
									minSet=true;
								}
							}catch(NumberFormatException e){
								errMin=true;
								durationMinor.setBackground(Color.RED);
							}
						}
					}
					if(durMajTimer!=0){
						if(--durMajTimer==0){
							try{
								errMaj=false;
								if(durationMajor.getText().isEmpty()){
									majSet=false;
									durationMajor.setBackground(Color.YELLOW);
								}else{
									durMaj=Integer.parseInt(durationMajor.getText());
									errMaj=false;
									durationMajor.setBackground(Color.WHITE);//low numbers don't matter.
									majSet=true;
								}
							}catch(NumberFormatException e){
								errMaj=true;
								durationMajor.setBackground(Color.RED);
							}
						}
					}
					if(pathTimer!=0){
						if(--pathTimer==0){
							try{
								if(path.getText().isEmpty()){
									pathSet=false;
									path.setBackground(Color.YELLOW);
								}else{
									dir=new File(path.getText());
									if(!dir.isDirectory())throw null;
									errPath=false;
									path.setBackground(Color.WHITE);
									pathSet=true;
								}
							}catch(Exception e){
								path.setBackground(Color.RED);
								errPath=true;
							}
						}
					}
				}else if((!(errMin|errMaj|errPath))&&(hkSet&(hkSet>>2))==0){
					if(((startRec==0||stopRec==0))||!(minSet&majSet&pathSet)){
						changeStatus(STATUS_WAITING);
					}else changeStatus(STATUS_SUSPENDED);
				}
				else changeStatus(STATUS_ERROR);
			}else if(status==STATUS_RUNNING){//stupid java.io approach. Might try nio later
				File[] files=dir.listFiles((f)->!f.isDirectory());//BEHOLD my LAMBDA skill! OOOOOOOH
				int max=durMaj/durMin;
				if(files.length>max+(recording?1:0)){
					long oldest=files[0].lastModified();
					File oldestFile=files[0];
					for(int i=1;i<files.length;i++){
						if(files[i].lastModified()<oldest){
							oldest=files[i].lastModified();
							oldestFile=files[i];
						}
					}
					System.out.println("deleting"+oldestFile.getName());
					oldestFile.delete();//be careful
				}
				if(recording){
					if(System.currentTimeMillis()>=next){
						trigger.keyPress(stopRec);
						holdStopTimer=10;
						restartTimer=20;
						recording=false;
					}
				}else{
					if(restartTimer--<=0){
						next=System.currentTimeMillis()+60000*durMin;
						System.out.println("Starting");
						trigger.keyPress(startRec);
						holdStartTimer=10;
						recording=true;
					}
				}
			}
			else if(status==STATUS_TERMINATED)break;
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {//This probably happens when you close it.
				e.printStackTrace();
			}
		}
	}
	
	void changeStatus(int newStatus){
		if(newStatus==STATUS_ERROR||newStatus==STATUS_CHECKING||newStatus==STATUS_WAITING){
			ctrl.setEnabled(false);
			ctrl.setLabel("Start");
		}
		else if(newStatus==STATUS_SUSPENDED){
			ctrl.setLabel("Start");
			ctrl.setEnabled(true);
		}else if(newStatus==STATUS_RUNNING){
			ctrl.setLabel("Stop");
			durMaj=(int) Math.ceil((durMaj-0.01)/durMin)*durMin;
			if(durMaj<durMin)durMaj=durMin;
			durationMajor.setText(String.valueOf(durMaj));
		}
		status=newStatus;
		statusL.setText(STATUS_STR[status]);
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(status==STATUS_SUSPENDED){
			changeStatus(STATUS_RUNNING);
		}else if(status==STATUS_RUNNING){
			changeStatus(STATUS_SUSPENDED);
		}
	}
}