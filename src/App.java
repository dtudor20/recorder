import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class App {
    private static TargetDataLine microphone;
    private static boolean isRecording = false;
    private static int fileIndex = 0; // To keep track of the file index

    public static void main(String[] args) throws IOException{
        final BufferedImage backgroundImage = ImageIO.read(App.class.getResourceAsStream("image.png"));

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(null); 

        
        JButton button = new JButton("Start Recording");
        button.setBounds(50, 150, 150, 50); 

        JFrame frame = new JFrame("Recording App");
        frame.setSize(250, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setContentPane(backgroundPanel);
        frame.setResizable(false);
        backgroundPanel.add(button);

        JButton savedButton = new JButton();
        savedButton.setBounds(50, 200, 150, 50);
        savedButton.setText("Play Audio:" + fileIndex);
        backgroundPanel.add(savedButton);

        JButton preButton = new JButton();
        preButton.setBounds(0, 200, 50, 50);
        preButton.setText("<");
        backgroundPanel.add(preButton);
        preButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileIndex--;
                savedButton.setText("Play Audio:" + fileIndex);
            }
        });

        JButton nexButton = new JButton();
        nexButton.setBounds(200, 200, 50, 50);
        nexButton.setText(">");
        backgroundPanel.add(nexButton);
        nexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileIndex++;
                savedButton.setText("Play Audio:" + fileIndex);
            }
        });

        frame.setVisible(true);

        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isRecording) {
                    // Stop recording
                    isRecording = false;
                    microphone.stop();
                    microphone.close();
                    button.setText("Start Recording");
                } else {
                    // Start recording
                    try {
                        microphone.open();
                        microphone.start();
                        button.setText("Stop Recording");
                        isRecording = true;

                        // Create a thread to capture the audio
                        Thread captureThread = new Thread(() -> {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            try {
                                while (isRecording) {
                                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                                    out.write(buffer, 0, bytesRead);
                                }
                                out.close();

                                // Save the captured audio data to a file
                                byte[] audioData = out.toByteArray();
                                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                                AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

                                // Write the audio data to a WAV file
                                File wavFile = new File("audio_" + fileIndex + ".wav");
                                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
                                fileIndex++; // Increment the file index for the next recording

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        captureThread.start();

                    } catch (LineUnavailableException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        savedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File audioFile = new File("audio_" + (fileIndex - 1) + ".wav");
                    if (!audioFile.exists()) {
                        System.out.println("Audio file not found!");
                        return;
                    }
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                    AudioFormat format = audioInputStream.getFormat();
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
                    sourceLine.open(format);
                    sourceLine.start();

                    int bytesRead = 0;
                    byte[] buffer = new byte[1024];
                    while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                        sourceLine.write(buffer, 0, bytesRead);
                    }

                    sourceLine.drain();
                    sourceLine.close();
                    audioInputStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}