package game;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Menu {
	private JFrame frame;
	private boolean started = false;
	private boolean stepMode = true;
	private int size;
	private JLabel label;

	public JFrame getFrame() {
		return frame;
	}

	public int getSize() {
		return size;
	}
	
	public boolean isStepMode() {
		return stepMode;
	}

	public boolean isStarted() {
		return started;
	}

	public Menu() {

		size = 20;

		frame = new JFrame();
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		label = new JLabel("<html>Grid Size: " + String.valueOf(size) + 
				"<br>Step Mode: " + String.valueOf(stepMode).toUpperCase() + "</html>");
		label.setSize(100, 500);
		label.setHorizontalAlignment(JLabel.CENTER);

		JButton startButton = new JButton("Start");
		startButton.setActionCommand("Start");
		startButton.addActionListener(new EventoBotao());

		JButton raiseGridSizeButton = new JButton("+");
		raiseGridSizeButton.setActionCommand("raiseGridSize");
		raiseGridSizeButton.addActionListener(new EventoBotao());
		raiseGridSizeButton.setSize(500, 50);

		JButton lowerGridSizeButton = new JButton("-");
		lowerGridSizeButton.setActionCommand("lowerGridSize");
		lowerGridSizeButton.addActionListener(new EventoBotao());
		lowerGridSizeButton.setSize(200, 50);
		
		JButton toggleStepButton = new JButton("Toggle StepMode");
		toggleStepButton.setActionCommand("toggleStepButton");
		toggleStepButton.addActionListener(new EventoBotao());
		toggleStepButton.setSize(200, 50);

		frame.add(panel);
		panel.setSize(500, 500);
		panel.add(startButton, BorderLayout.PAGE_START);
		panel.add(label, BorderLayout.CENTER);
		panel.add(raiseGridSizeButton, BorderLayout.LINE_END);
		panel.add(lowerGridSizeButton, BorderLayout.LINE_START);
		panel.add(toggleStepButton, BorderLayout.PAGE_END);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Enterprise Tactical RPG");
		frame.pack();
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private class EventoBotao implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String comando = e.getActionCommand();
			if (comando.equals("Start")) {
				started = true;
			} else if (comando.equals("raiseGridSize") && size < 30) {
				size++;
				label.setText("<html>Grid Size: " + String.valueOf(size) + 
						"<br>Step Mode: " + String.valueOf(stepMode).toUpperCase() + "</html>");
			} else if (comando.equals("lowerGridSize") && size > 16) {
				size--;
				label.setText("<html>Grid Size: " + String.valueOf(size) + 
						"<br>Step Mode: " + String.valueOf(stepMode).toUpperCase() + "</html>");
			} else if (comando.equals("toggleStepButton") && size > 16) {
				stepMode = !stepMode;
				label.setText("<html>Grid Size: " + String.valueOf(size) + 
						"<br>Step Mode: " + String.valueOf(stepMode).toUpperCase() + "</html>");
			}
		}
	}

}