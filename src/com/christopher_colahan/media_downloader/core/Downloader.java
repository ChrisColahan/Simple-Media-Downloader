package com.christopher_colahan.media_downloader.core;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;


//TODO: add protection from re-writing over same file

public class Downloader {
	
	public static DefaultListModel<DownloadGUI> listModel = new DefaultListModel<DownloadGUI>();
	
	public static void main(String args[]) {
		initGUI();
	}
	
	private static void initGUI() {
		JFrame frame = new JFrame("Media Downloader");
		frame.setLayout(new BorderLayout());
		
		JPanel input = new JPanel();
		JPanel topInput = new JPanel();
		JPanel botInput = new JPanel();
		
		input.setLayout(new BorderLayout());
		botInput.setLayout(new BorderLayout());
		topInput.setLayout(new GridLayout(3, 2));
		
		input.add(BorderLayout.NORTH, topInput);
		input.add(BorderLayout.SOUTH, botInput);
		
		JPanel output = new JPanel();
		
		JList<DownloadGUI> list = new JList<DownloadGUI>(listModel);
		JScrollPane scrollPane = new JScrollPane(list);
		
		output.add(scrollPane);
		
		
		//{input panel}
		//"source URL"			[url here]
		//"save file"			[file here]
		//<start download>
		//{output panel}
		//[list of download's progress]
		
		final JTextField urlInput = new JTextField(25);
		final JTextField saveInput = new JTextField(25);
		JButton startDownload = new JButton("Start Download");
		
		topInput.add(new JLabel("From URL"));
		topInput.add(urlInput);
		
		topInput.add(new JLabel("Save to"));
		topInput.add(saveInput);
		
		botInput.add(BorderLayout.CENTER, startDownload);
		
		startDownload.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!urlInput.getText().trim().equals("")) {
					String url = urlInput.getText().trim();
					String file = saveInput.getText().trim();
					
					try {
						if(file.trim().equals(""))
							listModel.addElement(new DownloadGUI(new Download(url)));
						else
							listModel.addElement(new DownloadGUI(new Download(url, file)));
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		frame.add(BorderLayout.NORTH, input);
		frame.add(BorderLayout.SOUTH, output);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}

class Download implements Runnable{
	
	private long totalBytes, currBytes;
	
	URL downloadURL;
	URLConnection connection;
	
	File file;
	
	String status = "";
	
	//same as Download(String url, String filePath), except filePath is the file of url
	public Download(String url) throws IOException {
		this(url, url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : url);
	}
	
	//if exception is MalformedURLException, the URL is wrong, else it is a connection error or file error
	public Download(String url, String filePath) throws IOException {
		downloadURL = new URL(url);
		connection = downloadURL.openConnection();
		
		file = new File(filePath);
		if(!file.exists()) {
			if(file.getParentFile() != null)
				file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		totalBytes = connection.getContentLengthLong();
	}
	
	@Override
	public void run() {
		try {
			status = "Downloading";
			DataInputStream in = new DataInputStream(connection.getInputStream());
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
			
			int b = 0;
			while((b = in.read()) != -1) {
				out.write(b);
				currBytes ++;
			}
			
			in.close();
			out.close();
			
			status = "Complete";
			
		} catch (IOException e) {
			e.printStackTrace();
			status = "Failed";
		}
	}
	
	public long getSize() {
		return totalBytes;
	}
	
	public long getCurrentBytes() {
		return currBytes;
	}
	
	public String getURL() {
		return downloadURL.toString();
	}
	
	public String getFile() {
		return file.toString();
	}
}

class DownloadGUI extends JPanel{
	private static final long serialVersionUID = 1L;

	private String status = "Starting";
	
	private JProgressBar progressBar;
	
	private Download d;
	
	public DownloadGUI(Download d) {
		super();
		
		this.d = d;
		
		setLayout(new BorderLayout());
		
		progressBar = new JProgressBar(0, (int) d.getSize());
		progressBar.setPreferredSize(new Dimension(200,200));
		
		add(BorderLayout.WEST, new JLabel(status));
		add(BorderLayout.EAST, progressBar);
		
		startDownload(d);
		
		setPreferredSize(new Dimension(100,100));
	}
	
	private void startDownload(final Download d) {
		Thread download = new Thread(d);
		download.start();
		
		Thread guiUpdater = new Thread(new Runnable() {

			@Override
			public void run() {
				while(d.getCurrentBytes() < d.getSize() && !d.status.equals("Failed")) {
					progressBar.setValue((int) d.getCurrentBytes());
					status = d.status;
				}
				
			}
			
		});
		guiUpdater.start();
	}
	
	public String toString() {
		return d.getURL() + "                                " + d.getFile() + "                         " + ((double)d.getCurrentBytes())/1000.0 + "/" + ((double)d.getSize())/1000.0 + " MB" + "                    " + (100.0)*((double)d.getCurrentBytes())/((double)d.getSize()) + "%";
	}
}