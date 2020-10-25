package mapgenerator;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Frame extends JFrame implements ActionListener, MouseListener, MouseMotionListener {
	
	private static final long serialVersionUID = 3131325789395630830L;

	private JPanel panel;
	
	private JButton buttonLoadImage;
	private JButton buttonConvert;
	private JButton buttonSaveImage;
	private JButton buttonSaveSchem;
	private JButton buttonSaveMapFile;
	private JList listUsedBlocks;
	private JList listDaniedBlocks;
	private JLabel imageMap;
	private JLabel infoLabel1;
	private JLabel infoLabel2;
	private JLabel infoLabel3;
	private JLabel infoLabel4;
	
	public Frame() {
				
		this.setSize(new Dimension(965, 615));
		this.setResizable(false);
		this.setTitle("Minecraft Map Generator");
		
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
	    	}
	    });
		
		panel = new JPanel();
		panel.setLayout(null);
		
		buttonLoadImage = new JButton("Bild laden");
		buttonLoadImage.addActionListener(this);
		buttonLoadImage.setLocation(15, 15);
		buttonLoadImage.setSize(100, 25);
		panel.add(buttonLoadImage);
		buttonConvert = new JButton("Map Aktuallisieren");
		buttonConvert.addActionListener(this);
		buttonConvert.setLocation(130, 15);
		buttonConvert.setSize(140, 25);
		panel.add(buttonConvert);
		buttonSaveImage = new JButton("Blockliste und Bild speichern");
		buttonSaveImage.addActionListener(this);
		buttonSaveImage.setLocation(285, 15);
		buttonSaveImage.setSize(200, 25);
		panel.add(buttonSaveImage);
		buttonSaveSchem = new JButton("PixelArt-Vorlage speichern");
		buttonSaveSchem.addActionListener(this);
		buttonSaveSchem.setLocation(500, 15);
		buttonSaveSchem.setSize(200, 25);
		panel.add(buttonSaveSchem);
		buttonSaveMapFile = new JButton("Bild als MC Map speichern");
		buttonSaveMapFile.addActionListener(this);
		buttonSaveMapFile.setLocation(720, 15);
		buttonSaveMapFile.setSize(200, 25);
		panel.add(buttonSaveMapFile);
		
		listUsedBlocks = new JList(new DefaultListModel());
		listUsedBlocks.addMouseListener(this);
		listUsedBlocks.setLayoutOrientation(JList.VERTICAL);
		
		listDaniedBlocks = new JList(new DefaultListModel());
		listDaniedBlocks.addMouseListener(this);
		listDaniedBlocks.setLayoutOrientation(JList.VERTICAL);
		
		JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setViewportView(this.listUsedBlocks);
		scrollPane1.setSize(200, 500);
		scrollPane1.setLocation(15, 55);
		panel.add(scrollPane1);
		
		JScrollPane scrollPane2 = new JScrollPane();
		scrollPane2.setViewportView(this.listDaniedBlocks);
		scrollPane2.setSize(200, 500);
		scrollPane2.setLocation(230, 55);
		panel.add(scrollPane2);
		
		imageMap = new JLabel();
		imageMap.setSize(500, 500);
		imageMap.setLocation(445, 55);
		imageMap.setBorder(BorderFactory.createEtchedBorder());
		imageMap.setHorizontalAlignment(SwingConstants.CENTER);
		imageMap.addMouseMotionListener(this);
		panel.add(imageMap);
		
		infoLabel1 = new JLabel("Genutze Blöcke");
		infoLabel1.setSize(800, 15);
		infoLabel1.setLocation(15, 40);
		panel.add(infoLabel1);
		
		infoLabel2 = new JLabel("Verbotene Blöcke");
		infoLabel2.setSize(800, 15);
		infoLabel2.setLocation(230, 40);
		panel.add(infoLabel2);
		
		infoLabel3 = new JLabel("Map Vorschau");
		infoLabel3.setSize(800, 15);
		infoLabel3.setLocation(445, 40);
		panel.add(infoLabel3);
		
		infoLabel4 = new JLabel("---");
		infoLabel4.setSize(500, 15);
		infoLabel4.setLocation(445, 555);
		panel.add(infoLabel4);
		
		this.add(panel);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == this.buttonLoadImage) {
			
			Main.usedBlocks.clear();
			
			FileDialog fileDialog = new FileDialog(this, "Bild wählen ...", FileDialog.LOAD);
			fileDialog.setFile("*.png");
			fileDialog.setVisible(true);
			
			try {
				
				Main.imageFile = new File(fileDialog.getDirectory() + "/" + fileDialog.getFile());
				
				if (Main.imageFile.exists()) Main.loadImage(Main.imageFile);
				
			} catch (NullPointerException e1) {
			} catch (IOException e1) {
				System.err.println("ERROR Cant load Immage!");
				e1.printStackTrace();
				System.exit(-1);
			}
			
			this.setTitle("Minecraft Map Generator - " + Main.imageFile);
			
		} else if (e.getSource() == this.buttonConvert) {
			
			if (Main.imageFile != null ? Main.imageFile.exists() : false) {
				
				Main.usedBlocks.clear();
				Main.daniedBlocks.clear();
				for (int i = 0; i < this.listDaniedBlocks.getModel().getSize(); i++) {
					Main.daniedBlocks.add((String) this.listDaniedBlocks.getModel().getElementAt(i));
				}
				
				Main.generateMap();
				
				DefaultListModel model = (DefaultListModel) listUsedBlocks.getModel();
				model.clear();
				for (Entry<String, Integer> entry : Main.usedBlocks.entrySet()) {
					model.addElement(entry.getValue() + " x " + entry.getKey());
				}
				
				try {
					
					File temp = new File(ClassLoader.getSystemResource("").getPath() + "/mc_generator_map.png");
					System.out.println("Temp Path: " + temp);
					Main.saveMapImage(temp);
					ImageIcon icon = new ImageIcon(ImageIO.read(temp));
					icon.setImage(icon.getImage().getScaledInstance(500, 500, Image.SCALE_DEFAULT));
					icon.getImage().flush();
					imageMap.setIcon(icon);
					temp.delete();
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
			
		} else if (e.getSource() == this.buttonSaveImage) {
			
			if (Main.imageFile != null ? Main.imageFile.exists() : false) {
				
				FileDialog fileDialog = new FileDialog(this, "Speichern als ...", FileDialog.SAVE);
				fileDialog.setFile("*.png");
				fileDialog.setVisible(true);
				
				try {
					
					File pathImage = new File((fileDialog.getDirectory() + "/" + fileDialog.getFile()).split("\\.")[0] + ".png");
					File pathList = new File(pathImage.getAbsolutePath().split("\\.")[0] + ".txt");
					
					Main.saveMapImage(pathImage);
					
					BufferedWriter os = new BufferedWriter(new FileWriter(pathList));
					
					for (int i = 0; i < this.listUsedBlocks.getModel().getSize(); i++) {
						os.write((String) this.listUsedBlocks.getModel().getElementAt(i) + "\n");
					}
					
					os.close();
					
				} catch (NullPointerException e1) {
				} catch (IOException e1) {
					System.err.println("ERROR Cant save Image!");
					e1.printStackTrace();
					System.exit(-1);
				}
				
			}
			
		} else if (e.getSource() == this.buttonSaveSchem) {
			
			if (Main.imageFile != null ? Main.imageFile.exists() : false) {

				FileDialog fileDialog = new FileDialog(this, "Speicherb als ...", FileDialog.SAVE);
				fileDialog.setFile("*.pix");
				fileDialog.setVisible(true);
				
				try {
					
					File pathSchem = new File((fileDialog.getDirectory() + "/" + fileDialog.getFile()).split("\\.")[0] + ".pix");
					
					if (pathSchem.exists()) Main.saveAsSchematic(pathSchem);
					
				} catch (NullPointerException e1) {
				} catch (IOException e1) {
					System.err.println("ERROR Cant save Schematic!");
					e1.printStackTrace();
					System.exit(-1);
				}
				
			}
			
		} else if (e.getSource() == this.buttonSaveMapFile) {
			
			if (Main.imageFile != null ? Main.imageFile.exists() : false) {

				FileDialog fileDialog = new FileDialog(this, "Speicherb als ...", FileDialog.SAVE);
				fileDialog.setFile("map_*.dat");
				fileDialog.setVisible(true);
				
				try {
					
					File pathSchem = new File((fileDialog.getDirectory() + "/" + fileDialog.getFile()).split("\\.")[0] + ".dat");
					
					Main.saveAsMapFile(pathSchem, true);
					
				} catch (NullPointerException e1) {
				} catch (IOException e1) {
				}
				
			}
			
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
		if (e.getClickCount() == 2) {
			
			if (e.getSource() == this.listUsedBlocks) {
				
				String entry = (String) this.listUsedBlocks.getSelectedValue();
				int entryIndex = this.listUsedBlocks.getSelectedIndex();
				
				DefaultListModel model2 = (DefaultListModel) this.listDaniedBlocks.getModel();
				if (model2.size() < entryIndex) {
					model2.addElement(entry.split("x ")[1]);
				} else {
					model2.add(entryIndex, entry.split("x ")[1]);
				}
				
			} else if (e.getSource() == this.listDaniedBlocks) {
				
				DefaultListModel model = (DefaultListModel) this.listDaniedBlocks.getModel();
				
				int entryIndex = this.listDaniedBlocks.getSelectedIndex();
				model.remove(entryIndex);
				
			}
			
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		if (e.getSource() == this.imageMap && this.imageMap.getIcon() != null) {
			
			float x = e.getX();
			float y = e.getY();
			
			int mapX = (int) Math.ceil(x / 500 * 128) - 1;
			int mapY = (int) Math.ceil(y / 500 * 128) - 1;
			
			if (mapX >= 0 && mapX <= 128 && mapX >= 0 && mapX <= 128) {

				String block = Main.blockOrder[mapY][mapX];
				
				infoLabel4.setText(mapX + " " + mapY + ": " + block);
				
			}
			
		}
		
	}
	
}
